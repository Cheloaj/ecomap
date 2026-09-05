package com.ecomap.usuario.data.repository

import com.ecomap.usuario.data.model.User
import com.ecomap.usuario.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(email: String, password: String, fullName: String): Result<User> {
        return try {
            println("🔍 Iniciando proceso de registro para: $email")

            // ========================================
            // PASO 0: VERIFICAR SI EL USUARIO YA EXISTE
            // ========================================
            // Vía RPC: el SELECT directo sobre users ya no está permitido por el RLS
            try {
                val existing = userPublicInfo(email)
                val tipo = existing?.get("user_type")?.jsonPrimitive?.contentOrNull
                val verificado = existing?.get("email_verified")?.jsonPrimitive?.booleanOrNull ?: false

                if (tipo == "socio") {
                    return Result.failure(
                        Exception("Este correo ya está registrado como vendedor. Usa un correo diferente o inicia sesión en la app de vendedores.")
                    )
                }

                if (tipo == "cliente" && verificado) {
                    return Result.failure(
                        Exception("Este correo ya está registrado. Por favor inicia sesión.")
                    )
                }

                if (tipo == "cliente" && !verificado) {
                    // Cuenta a medias: se reemite el código en el servidor.
                    // El código ya no se genera aquí ni se imprime en logcat.
                    val issued = rpcJson("issue_verification_code", mapOf("email_param" to email))
                    val code = issued["code"]?.jsonPrimitive?.contentOrNull

                    if (code != null) {
                        try {
                            sendVerificationEmail(email, code, fullName)
                        } catch (emailError: Exception) {
                            // El código quedó guardado aunque falle el correo
                        }
                    }

                    val id = existing["id"]?.jsonPrimitive?.contentOrNull ?: ""
                    return Result.success(
                        User(id = id, email = email, fullName = fullName, userType = "cliente")
                    )
                }
            } catch (e: Exception) {
                // Si la consulta falla, se continúa: auth.users validará el alta
            }

            // ========================================
            // PASO 1: CREAR USUARIO EN AUTH (o recuperar si ya existe)
            // ========================================
            var userId: String? = null
            var isRecoveredUser = false

            try {
                val authResponse = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                userId = authResponse?.id
                println("✅ Usuario creado en Auth con ID: $userId")
            } catch (authError: Exception) {
                println("⚠️ Error al crear en Auth: ${authError.message}")

                // Si el error es "already registered", el usuario existe en Auth pero no en la tabla users
                if (authError.message?.contains("already registered", ignoreCase = true) == true ||
                    authError.message?.contains("User already registered", ignoreCase = true) == true) {

                    println("🔄 Usuario existe en Auth. Intentando hacer signIn para recuperar ID...")
                    try {
                        // Hacer signIn para obtener el userId
                        supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                        userId = supabase.auth.currentUserOrNull()?.id
                        isRecoveredUser = true
                        println("✅ Usuario recuperado de Auth con ID: $userId")
                    } catch (signInError: Exception) {
                        println("❌ Error al recuperar usuario: ${signInError.message}")
                        throw authError
                    }
                } else {
                    throw authError
                }
            }

            // Obtener el usuario ID si aún no lo tenemos
            if (userId == null) {
                kotlinx.coroutines.delay(1000)
                userId = supabase.auth.currentUserOrNull()?.id
            }

            if (userId == null) {
                println("❌ No se pudo obtener el ID del usuario")
                return Result.failure(Exception("No se pudo completar el registro."))
            }

            if (isRecoveredUser) {
                println("🔄 Completando registro de usuario huérfano en Auth")
            }

            // ========================================
            // PASO 2: GENERAR CÓDIGO DE VERIFICACIÓN
            // ========================================
            val verificationCode = generateVerificationCode()
            val expiryTime = Clock.System.now().plus(15.minutes)

            // El código NO se imprime: println() sobrevive a ProGuard y
            // quedaría legible en logcat en compilaciones de release.

            // ========================================
            // PASO 2.5: ENVIAR EMAIL CON CÓDIGO
            // ========================================
            try {
                println("📧 Enviando código de verificación por email...")
                sendVerificationEmail(email, verificationCode, fullName)
                println("✅ Email enviado exitosamente")
            } catch (emailError: Exception) {
                println("⚠️ Error al enviar email (continuando registro): ${emailError.message}")
                // NO fallar el registro si el email falla
            }

            // ========================================
            // PASO 3: CREAR REGISTRO EN TABLA USERS
            // ========================================
            val newUser = User(
                id = userId,
                email = email,
                fullName = fullName,
                emailVerified = false,
                verificationCode = verificationCode,
                verificationCodeExpiry = expiryTime.toString(),
                createdAt = Clock.System.now().toString(),
                userType = "cliente" // ✅ Identificar como usuario/cliente
            )

            try {
                println("📝 Insertando usuario en tabla 'users'...")
                supabase.from("users").insert(newUser)
                println("✅ Usuario insertado correctamente")
            } catch (insertError: Exception) {
                println("⚠️ Insert falló, intentando update: ${insertError.message}")
                try {
                    supabase.from("users").update(
                        {
                            set("full_name", fullName)
                            set("verification_code", verificationCode)
                            set("verification_code_expiry", expiryTime.toString())
                            set("email_verified", false)
                        }
                    ) {
                        filter { eq("id", userId) }
                    }
                    println("✅ Usuario actualizado correctamente")
                } catch (updateError: Exception) {
                    println("❌ Update también falló: ${updateError.message}")
                    throw insertError
                }
            }

            println("🎉 Registro completado exitosamente")
            Result.success(newUser)
        } catch (e: Exception) {
            println("❌ SignUp error: ${e.message}")
            e.printStackTrace()
            val errorMessage = when {
                e.message?.contains("already registered") == true -> "Este correo ya está registrado"
                e.message?.contains("Invalid email") == true -> "Correo electrónico inválido"
                else -> e.message ?: "Error al registrarse"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Código de 6 dígitos con entropía criptográfica.
     *
     * Antes usaba kotlin.random.Random, un PRNG NO criptográfico y por tanto
     * predecible. Para un código que autoriza resetear una contraseña, eso es
     * un fallo de seguridad.
     */
    private fun generateVerificationCode(): String {
        val n = java.security.SecureRandom().nextInt(1_000_000)
        return n.toString().padStart(6, '0')
    }

    /** Ejecuta un RPC de Postgres y devuelve su respuesta ya parseada. */
    private suspend fun rpcJson(
        function: String,
        params: Map<String, String?>
    ): JsonObject {
        val result = supabase.postgrest.rpc(function, params)
        return Json.parseToJsonElement(result.data).jsonObject
    }

    /**
     * Consulta pública mínima de una cuenta por correo.
     *
     * Sustituye a los SELECT directos sobre `users`: el RLS ahora solo deja
     * leer la propia fila. El RPC devuelve únicamente campos no sensibles.
     */
    private suspend fun userPublicInfo(email: String): JsonObject? {
        val response = rpcJson("user_public_info", mapOf("email_param" to email))
        val exists = response["exists"]?.jsonPrimitive?.booleanOrNull ?: false
        return if (exists) response else null
    }

    private suspend fun sendVerificationEmail(email: String, code: String, userName: String) {
        try {
            println("📧 Llamando a Edge Function send-verification-email...")

            val body = buildJsonObject {
                put("email", email)
                put("code", code)
                put("userName", userName)
            }

            supabase.functions.invoke(
                function = "send-verification-email",
                body = body
            )

            println("✅ Edge Function respondió correctamente - Email enviado")
        } catch (e: Exception) {
            println("❌ Error al enviar email: ${e.message}")
            e.printStackTrace()
            throw Exception("No se pudo enviar el email de verificación: ${e.message}")
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Iniciar sesión en Supabase Auth
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Se filtra por id (no por correo) porque el RLS solo permite leer
            // la propia fila, y un mismo correo puede tener cuenta de socio y
            // de cliente. Con el id se toma siempre la que acaba de iniciar sesión.
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("No se pudo obtener la sesión"))

            val user = supabase.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()
                ?: return Result.failure(Exception("No se encontró el perfil de este usuario"))

            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Invalid login credentials") == true -> "Credenciales inválidas"
                e.message?.contains("Email not confirmed") == true -> "Verifica tu correo electrónico"
                else -> e.message ?: "Error al iniciar sesión"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
            if (currentUser == null) {
                return Result.success(null)
            }

            val user = supabase.from("users")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<User>()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUserSignedIn(): Boolean {
        return try {
            supabase.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            // Usar deep link de la app para manejar el reset de contraseña
            supabase.auth.resetPasswordForEmail(
                email = email,
                redirectUrl = "ecomap://reset-password"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Invalid email") == true -> "Correo electrónico inválido"
                e.message?.contains("not found") == true -> "No existe una cuenta con ese correo"
                else -> e.message ?: "Error al enviar el correo de recuperación"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            supabase.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("New password should be different") == true -> "La nueva contraseña debe ser diferente a la actual"
                e.message?.contains("Password should be at least") == true -> "La contraseña debe tener al menos 6 caracteres"
                else -> e.message ?: "Error al actualizar la contraseña"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Valida el código de verificación CONTRA EL SERVIDOR.
     *
     * Antes esta función descargaba la fila del usuario por correo y comparaba
     * `user.verificationCode != code` en el dispositivo. Para eso la tabla
     * `users` tenía que exponer `verification_code` a cualquiera que
     * consultara, y la anon key viaja dentro del APK: bastaba una petición para
     * leer el código de otra persona y apoderarse de su cuenta.
     *
     * Ahora se manda correo + código al RPC y solo regresa si fue válido. El
     * código nunca sale de la base. El servidor también marca el correo como
     * verificado y activa la cuenta, porque el cliente ya no puede tocar
     * `account_status` (lo impide un trigger).
     */
    override suspend fun verifyCode(email: String, code: String): Result<User> {
        return try {
            val response = rpcJson(
                "validate_verification_code",
                mapOf("email_param" to email, "code_param" to code)
            )

            val isValid = response["valid"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!isValid) {
                val reason = response["reason"]?.jsonPrimitive?.contentOrNull
                val message = when (reason) {
                    "expired" -> "Código de verificación expirado"
                    "locked" -> "Demasiados intentos fallidos. Espera 15 minutos e inténtalo de nuevo"
                    else -> {
                        val left = response["attempts_left"]?.jsonPrimitive?.intOrNull
                        if (left != null && left > 0) {
                            "Código de verificación incorrecto. Te quedan $left intentos"
                        } else {
                            "Código de verificación incorrecto"
                        }
                    }
                }
                return Result.failure(Exception(message))
            }

            // El usuario ya tiene sesión en este punto del registro, así que
            // puede leer su propia fila (es lo único que permite el RLS).
            val userId = response["user_id"]?.jsonPrimitive?.contentOrNull
            val updatedUser = userId?.let { id ->
                supabase.from("users")
                    .select { filter { eq("id", id) } }
                    .decodeSingleOrNull<User>()
            }

            if (updatedUser != null) {
                Result.success(updatedUser)
            } else {
                // Sin sesión todavía: se devuelve lo mínimo que la UI necesita
                Result.success(
                    User(
                        id = userId ?: "",
                        email = email,
                        emailVerified = true,
                        accountStatus = "active"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo verificar el código. Revisa tu conexión."))
        }
    }

    override suspend fun checkEmailExists(email: String): Result<Boolean> {
        return try {
            Result.success(userPublicInfo(email) != null)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo verificar el correo. Revisa tu conexión."))
        }
    }

    /**
     * Pide al servidor que emita un código nuevo y lo envía por correo.
     *
     * El código lo genera Postgres con gen_random_bytes() (criptográfico) y se
     * guarda junto con su expiración en una sola operación. Ya no se imprime en
     * logcat: antes se escribía en claro con println(), que ProGuard NO elimina
     * en las compilaciones de release.
     */
    override suspend fun resendVerificationCode(email: String): Result<Unit> {
        return try {
            val response = rpcJson("issue_verification_code", mapOf("email_param" to email))

            val ok = response["success"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!ok) return Result.failure(Exception("Usuario no encontrado"))

            val code = response["code"]?.jsonPrimitive?.contentOrNull
                ?: return Result.failure(Exception("No se pudo generar el código"))
            val userName = response["full_name"]?.jsonPrimitive?.contentOrNull ?: "Usuario"

            try {
                sendVerificationEmail(email, code, userName)
            } catch (emailError: Exception) {
                return Result.failure(Exception("No se pudo enviar el correo con el código"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo reenviar el código. Revisa tu conexión."))
        }
    }

    /**
     * Restablece la contraseña usando el código de verificación.
     *
     * La versión anterior estaba rota de raíz: comparaba el código en el
     * cliente (mismo fallo de seguridad que verifyCode) y luego intentaba
     * `signInWith(Email)` usando la contraseña NUEVA para obtener sesión, cosa
     * que nunca podía funcionar — el propio código lo admitía en un comentario.
     *
     * Ahora todo ocurre en un RPC SECURITY DEFINER: valida el código, aplica
     * el límite de intentos y actualiza auth.users en una sola operación.
     */
    override suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
        return try {
            val response = rpcJson(
                "reset_password_with_code",
                mapOf(
                    "email_param" to email,
                    "code_param" to code,
                    "new_password_param" to newPassword
                )
            )

            val success = response["success"]?.jsonPrimitive?.booleanOrNull ?: false
            if (success) {
                Result.success(Unit)
            } else {
                val error = response["error"]?.jsonPrimitive?.contentOrNull
                Result.failure(Exception(error ?: "No se pudo restablecer la contraseña"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo restablecer la contraseña. Revisa tu conexión."))
        }
    }
}
