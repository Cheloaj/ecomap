# Auditoría de seguridad — EcoMap

Revisión completa de las dos aplicaciones y de su backend en Supabase.
**9 hallazgos**, 8 corregidos y verificados.

Este documento describe qué estaba mal, por qué era explotable y cómo se corrigió.
Los parches SQL correspondientes están en [`../db/`](../db/).

---

## Resumen

| # | Hallazgo | Gravedad | Estado |
|---|---|---|---|
| 1 | Toma de control de cuenta por código de verificación legible | 🔴 Crítico | Corregido |
| 2 | Row Level Security abierto a `anon` | 🔴 Crítico | Corregido |
| 3 | Código de verificación predecible y sin límite de intentos | 🟠 Alto | Corregido |
| 4 | Contraseña guardada con degradación silenciosa a texto plano | 🟠 Alto | Corregido |
| 5 | Documentos de identidad (INE) con URL pública permanente | 🟠 Alto | Corregido |
| 6 | Datos sensibles impresos en logcat, sobreviviendo a ProGuard | 🟡 Medio | Parcial |
| 7 | `usesCleartextTraffic` habilitado sin necesidad | 🟡 Medio | Corregido |
| 8 | Número de tarjeta y CVV en la ruta de navegación | 🟡 Medio | Corregido |
| 9 | Sin configuración de firma para release | 🟡 Medio | Pendiente |

---

## 1 · Toma de control de cuenta 🔴

**Dónde:** `AuthRepositoryImpl.kt` en ambas apps.

La validación del código de verificación ocurría **en el dispositivo**:

```kotlin
val users = supabase.from("users")
    .select { filter { eq("email", email) } }
    .decodeList<User>()

val isCodeValid = user.verificationCode == code   // ← comparación local
```

Para que esa línea funcione, la columna `verification_code` tiene que viajar hasta el
teléfono. Y la anon key de Supabase va **dentro del APK**: se extrae descomprimiendo el
archivo y leyendo el DEX.

**El ataque completo, sin adivinar nada:**

```
GET /rest/v1/users?email=eq.victima@correo.com&select=verification_code
→ se lee el código de recuperación de esa persona
→ POST /rest/v1/rpc/reset_password_with_code
→ cuenta tomada
```

**Corrección.** La comparación se movió a un RPC `SECURITY DEFINER`. El cliente envía
correo y código, y lo único que recibe de vuelta es si fue válido o no. El código
nunca sale de la base de datos.

```sql
CREATE FUNCTION validate_verification_code(email_param TEXT, code_param TEXT)
RETURNS JSON SECURITY DEFINER AS $$
  -- compara en el servidor, cuenta intentos, y devuelve solo {valid: bool}
$$;
```

**Verificación contra el servidor real:**

```
$ curl "$URL/rest/v1/users?select=email,verification_code" -H "apikey: $ANON"
{"code":"42501","message":"permission denied for table users"}
```

---

## 2 · Row Level Security abierto 🔴

Las políticas heredadas (`fix_rls_para_panel.sql`, `fix_update_policies.sql`) daban
lectura **y escritura** al rol `anon` sobre `users`, `businesses` y `products`. Era lo
que hacía explotable el hallazgo 1, y además dejaba a la vista la canasta, los
favoritos y el historial de cualquier persona.

**Corrección.**

- `users`: cada quien lee y edita **solo su propia fila**.
- Catálogo (negocios, productos, calificaciones): lectura para usuarios autenticados,
  escritura solo del dueño.
- Datos personales (canasta, favoritos, historial, preferencias): solo su dueño.
- Se revocó todo acceso del rol `anon`.
- **Triggers de blindaje de columnas**, porque una política RLS no puede restringir
  campos por sí sola. Sin ellos, cualquiera podía hacer `PATCH` sobre su propia fila y
  ponerse `is_pro = true` para saltarse el pago, o `user_type = 'admin'`. Lo mismo
  aplica a un vendedor aprobando su propio negocio sin pasar verificación.

---

## 3 · Código de verificación predecible 🟠

```kotlin
private fun generateVerificationCode(): String {
    return Random.nextInt(100000, 999999).toString()   // kotlin.random
}
```

`kotlin.random.Random` es un PRNG **no criptográfico**: su salida es predecible a partir
de observaciones previas. Para un código que autoriza restablecer una contraseña, eso es
un fallo de seguridad.

Agravante: `reset_password_with_code` era ejecutable por `anon` **sin contador de
intentos**. 900,000 combinaciones sin nada que frenara la fuerza bruta.

**Corrección.** `SecureRandom` en el cliente y `gen_random_bytes()` de pgcrypto en el
servidor, más bloqueo tras **5 intentos fallidos durante 15 minutos**, aplicado tanto en
la validación como en el restablecimiento.

---

## 4 · Contraseña en texto plano 🟠

**Dónde:** `SecurePreferences.kt` (EcoMap Usuario).

```kotlin
private val sharedPreferences = try {
    EncryptedSharedPreferences.create(...)
} catch (e: Exception) {
    context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)  // ← SIN cifrar
}
```

Si el cifrado fallaba, la clase caía a almacenamiento normal y guardaba ahí la
contraseña del usuario **en claro, sin avisar a nadie**.

Y ese fallo no es hipotético: la app tenía `android:allowBackup="true"` con los archivos
`backup_rules.xml` vacíos (la plantilla del arquetipo, todo comentado). Restaurar la app
en otro dispositivo desde el respaldo de Google invalida la llave del Keystore y hace
fallar exactamente esa llamada — es decir, la ruta al texto plano se activaba justo en
el escenario más común.

**Corrección.** Se eliminó la degradación: si no hay cifrado disponible, no se guarda
nada y el acceso biométrico queda deshabilitado. Perder una comodidad es preferible a
filtrar la credencial. Además `allowBackup="false"` en ambas apps, y se limpia cualquier
resto que una versión anterior hubiera dejado sin cifrar.

**Recomendación no implementada:** guardar el *refresh token* de Supabase en lugar de la
contraseña. Es la solución correcta, pero exige rehacer el flujo biométrico completo.

---

## 5 · Documentos de identidad públicos 🟠

`BusinessRepositoryImpl.kt` subía las INEs y comprobantes de domicilio y guardaba el
resultado de `bucket.publicUrl(...)`: un enlace **sin caducidad y sin autenticación**.
Cualquiera con la URL veía la identificación oficial de un vendedor.

**Corrección.** Se guarda la **ruta** del objeto, no una URL. Quien necesite verlo
genera un enlace firmado que expira a los 5 minutos (`createSignedUrl`). El bucket debe
marcarse como privado.

---

## 6 · Datos sensibles en logcat 🟡

ProGuard estaba configurado para neutralizar `android.util.Log`:

```proguard
-assumenosideeffects class android.util.Log { ... }
```

Pero el código usa **`println()` en 692 llamadas repartidas en 38 archivos**, y R8 no lo
toca. Una de ellas imprimía el código de verificación en claro:

```kotlin
println("🔍 Código en BD: '${user.verificationCode}'")
```

**Corrección parcial.** Se eliminaron los `println` que exponían códigos de verificación,
correos e identificadores en los flujos de autenticación. Quedan llamadas en otras capas:
lo correcto es sustituirlas por un logger que se apague en release.

---

## 7 · Tráfico en claro habilitado 🟡

Ambos manifiestos declaraban `android:usesCleartextTraffic="true"` sin necesitarlo: todo
el tráfico real va por HTTPS. Ese atributo permite conexiones HTTP sin cifrar y abre la
puerta a intercepción en redes hostiles. **Eliminado de las dos apps.**

---

## 8 · Tarjeta y CVV en la ruta de navegación 🟡

```kotlin
Screen("processing_payment/{cardNumber}/{expiryMonth}/{expiryYear}/{cvv}/{cardHolderName}")
```

El número de tarjeta y el CVV viajaban **como argumentos de navegación**, así que
quedaban escritos en la ruta dentro del back stack del `NavController`, en el
`SavedStateHandle` y en cualquier volcado de estado o reporte de fallo.

La pantalla de destino solo necesitaba mostrar `"Visa •••• 4242"`.

**Corrección.** La ruta pasó a `processing_payment/{cardBrand}/{lastFour}`. Los datos
sensibles no salen de la pantalla de pago.

> El módulo de pagos es una **simulación**: no hay pasarela integrada y ningún dato de
> tarjeta se transmite ni se almacena. Aun así, la corrección aplica: un dato sensible no
> debe viajar por la ruta de navegación, lo procese quien lo procese.

---

## 9 · Sin firma de release 🟡 *(pendiente)*

No existe `signingConfigs` ni keystore, y el `buildType release` tiene `isMinifyEnabled`
e `isShrinkResources` activados sin haberse compilado nunca. Con kotlinx-serialization,
Ktor y los modelos de Supabase, R8 suele romper la deserialización si faltan reglas.

Presupuestar una sesión de `assembleRelease` más prueba en dispositivo físico antes de
comprometer una fecha de publicación.

---

## Fuera del alcance

Por honestidad, lo que **no** cubre esta auditoría:

- No hay *rate limiting* por IP; el límite de intentos es por cuenta.
- Se puede seguir enumerando correos registrados: es inherente al mensaje "este correo ya
  está registrado" que muestra la app. Quitarlo exigiría rediseñar la UX del registro.
- El envío del correo con el código sigue saliendo desde la app, así que el código pasa
  por el dispositivo de la persona dueña de la cuenta. Cerrarlo del todo requiere mover
  el envío a una Edge Function.
- No se hizo análisis dinámico ni pruebas de penetración: es una revisión de código y de
  configuración del backend, con verificación puntual de los hallazgos explotables.
