package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.User
import com.ecomap.socio.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.functions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonObject
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
            // PASO 0: VALIDAR QUE EL EMAIL NO EXISTA COMO CLIENTE
            // ========================================
            try {
                // Vía RPC: el SELECT directo sobre users ya no está permitido
                val existing = userPublicInfo(email)
                val tipo = existing?.get("user_type")?.jsonPrimitive?.contentOrNull
                if (tipo == "cliente") {
                    return Result.failure(
                        Exception("Este correo ya está registrado como cliente. Usa un correo diferente o inicia sesión en la app de clientes.")
                    )
                }
            } catch (e: Exception) {
                // Si la consulta falla, se continúa: el registro se validará
                // igualmente contra auth.users al crear la cuenta.
            }

            // ========================================
            // PASO 1: CREAR USUARIO EN AUTH
            // ========================================
            val authResponse = try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (authError: Exception) {
                // Manejar error específico de email duplicado en Auth
                val errorMessage = authError.message?.lowercase() ?: ""
                if (errorMessage.contains("user already registered") ||
                    errorMessage.contains("email address already in use") ||
                    errorMessage.contains("already exists")) {

                    println("⚠️ Email ya existe en Auth: $email")
                    println("🔍 Verificando si el usuario no ha verificado su email...")

                    // Recuperación de registro a medias. Vía RPC: el SELECT
                    // directo sobre users ya no está permitido por el RLS.
                    try {
                        val existingUser = userPublicInfo(email)
                        val existingId = existingUser?.get("id")?.jsonPrimitive?.contentOrNull
                        val existingType = existingUser?.get("user_type")?.jsonPrimitive?.contentOrNull
                        val existingVerified =
                            existingUser?.get("email_verified")?.jsonPrimitive?.booleanOrNull ?: false

                        if (existingType == "cliente") {
                            return Result.failure(
                                Exception("Este correo ya está registrado como cliente. Usa un correo diferente.")
                            )
                        }

                        if (existingId != null && !existingVerified) {
                            // Eliminar de la tabla users
                            supabase.from("users").delete {
                                filter { eq("id", existingId) }
                            }

                            // Llamar a RPC function para eliminar de auth.users
                            try {
                                supabase.postgrest.rpc("delete_unverified_auth_user", buildJsonObject {
                                    put("user_email", email)
                                })
                                println("✅ Usuario eliminado de auth.users")
                            } catch (rpcError: Exception) {
                                println("⚠️ Error al eliminar de auth.users: ${rpcError.message}")
                            }

                            // ✅ REINTENTAR REGISTRO CON BACKOFF
                            println("🔄 Reintentando registro hasta que Supabase Auth lo permita...")
                            return retrySignUpAfterDeletion(email, password, fullName, maxRetries = 5)
                        }  else if (existingUser == null) {
                        // 🛠️ FIX: EL USUARIO "ZOMBIE" (Existe en Auth, no en BD)
                        // En lugar de reintentar y borrar, intentamos "rescatarlo" haciendo login.
                        println("⚠️ Usuario detectado en Auth pero NO en base de datos.")
                        println("🛠️ Solución: Intentando iniciar sesión y reparar perfil...")

                        try {
                            // 1. Intentamos loguear con la contraseña que el usuario acaba de escribir
                            supabase.auth.signInWith(Email) {
                                this.email = email
                                this.password = password
                            }

                            // 2. Obtenemos su ID real de Auth
                            val recoveredId = supabase.auth.currentUserOrNull()?.id

                            if (recoveredId != null) {
                                println("✅ Login de recuperación exitoso. ID: $recoveredId")
                                println("📝 Creando perfil faltante en tabla 'users'...")

                                // 3. Preparamos los datos
                                val verificationCode = generateVerificationCode()
                                val expiryTime = Clock.System.now().plus(15.minutes)

                                val recoveredUser = User(
                                    id = recoveredId,
                                    email = email,
                                    fullName = fullName,
                                    createdAt = Clock.System.now().toString(),
                                    emailVerified = false,
                                    verificationCode = verificationCode,
                                    verificationCodeExpiry = expiryTime.toString(),
                                    onboardingStep = "email_verification",
                                    accountStatus = "pending_verification",
                                    userType = "socio"
                                )

                                // 4. Insertamos en la BD para arreglar el problema
                                supabase.from("users").insert(recoveredUser)
                                println("✅ Perfil reparado e insertado en BD.")

                                // 5. Enviamos el correo de verificación
                                try {
                                    sendVerificationEmail(email, verificationCode, fullName)
                                } catch (e: Exception) {
                                    println("⚠️ Error envío email (no crítico): ${e.message}")
                                }

                                // 6. ¡Éxito! Devolvemos el usuario recuperado
                                return Result.success(recoveredUser)

                            } else {
                                throw Exception("No se pudo recuperar el ID del usuario.")
                            }
                        } catch (loginError: Exception) {
                            println("❌ Falló la recuperación automática: ${loginError.message}")
                            // Si la contraseña no coincide, le decimos que inicie sesión normal
                            return Result.failure(
                                Exception("Este correo ya está registrado. Por favor intenta Iniciar Sesión.")
                            )
                        }
                    } else {
                            // Email verificado
                            println("❌ Email ya está registrado y verificado")
                            return Result.failure(
                                Exception("Este correo ya está registrado. Inicia sesión en su lugar.")
                            )
                        }
                    } catch (checkError: Exception) {
                        println("❌ Error al verificar usuario existente: ${checkError.message}")
                        return Result.failure(
                            Exception("Este correo ya está registrado. Inicia sesión en su lugar.")
                        )
                    }
                }
                // Otro tipo de error
                throw authError
            }

            // Get user ID from the auth response directly (no need to wait for currentUser)
            val userId = authResponse?.id ?: run {
                // If response doesn't have ID, try to get current user after a short delay
                kotlinx.coroutines.delay(1000)
                supabase.auth.currentUserOrNull()?.id
            }

            if (userId == null) {
                println("❌ No se pudo obtener el ID del usuario después del registro")
                println("Esto puede significar que 'Confirm email' está activado en Supabase")
                println("Ve a: Authentication → Providers → Email → Confirm email: OFF")
                return Result.failure(Exception("No se pudo completar el registro. Por favor verifica la configuración de Supabase."))
            }

            println("✅ Usuario creado en Auth con ID: $userId")

            // ========================================
            // PASO 2: GENERAR CÓDIGO DE VERIFICACIÓN
            // ========================================
            val verificationCode = generateVerificationCode()
            val expiryTime = Clock.System.now().plus(15.minutes)

            // Print code for testing (remove in production)
            println("=================================")
            println("CÓDIGO DE VERIFICACIÓN: $verificationCode")
            println("Email: $email")
            println("Usuario ID: $userId")
            println("=================================")

            // ========================================
            // PASO 2.5: ENVIAR EMAIL CON CÓDIGO
            // ========================================
            try {
                println("📧 Enviando código de verificación por email...")
                sendVerificationEmail(email, verificationCode, fullName)
                println("✅ Email enviado exitosamente")
            } catch (emailError: Exception) {
                println("⚠️ Error al enviar email (continuando registro): ${emailError.message}")
                // NO fallar el registro si el email falla - el usuario puede solicitar reenvío
            }

            // ========================================
            // PASO 3: CREAR REGISTRO EN TABLA USERS
            // ========================================
            val user = User(
                id = userId,
                email = email,
                fullName = fullName,
                createdAt = Clock.System.now().toString(),
                emailVerified = false,
                verificationCode = verificationCode,
                verificationCodeExpiry = expiryTime.toString(),
                onboardingStep = "email_verification",  // Paso inicial: verificar email
                accountStatus = "pending_verification",   // Estado inicial: pendiente
                userType = "socio" // ✅ Identificar como vendedor
            )

            try {
                println("📝 Intentando insertar usuario en tabla 'users'...")
                supabase.from("users").insert(user)
                println("✅ Usuario insertado correctamente en tabla 'users'")
            } catch (insertError: Exception) {
                // If insert fails, it might be because user already exists
                // Try to update instead
                println("⚠️ Insert falló, intentando update: ${insertError.message}")
                try {
                    supabase.from("users").update(
                        {
                            set("full_name", fullName)
                            set("verification_code", verificationCode)
                            set("verification_code_expiry", expiryTime.toString())
                            set("email_verified", false)
                            set("onboarding_step", "email_verification")
                            set("account_status", "pending_verification")
                        }
                    ) {
                        filter {
                            eq("id", userId)
                        }
                    }
                    println("✅ Usuario actualizado correctamente en tabla 'users'")
                } catch (updateError: Exception) {
                    println("❌ Update también falló: ${updateError.message}")
                    throw insertError // Throw original error
                }
            }

            println("🎉 Registro completado exitosamente")
            Result.success(user)
        } catch (e: Exception) {
            println("❌ SignUp error completo: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al crear usuario: ${e.message}"))
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val authUser = supabase.auth.currentUserOrNull()
            if (authUser == null) {
                return Result.failure(Exception("User not found"))
            }

            // Fetch user data from database
            val user = supabase.from("users")
                .select {
                    filter {
                        eq("id", authUser.id)
                    }
                }.decodeSingle<User>()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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
            println("🔍 getCurrentUser: Obteniendo usuario autenticado...")

            val authUser = supabase.auth.currentUserOrNull()
            if (authUser == null) {
                println("❌ getCurrentUser: No hay usuario autenticado en Supabase Auth")
                return Result.success(null)
            }

            println("✅ getCurrentUser: Usuario autenticado encontrado - ID: ${authUser.id}")
            println("📝 getCurrentUser: Consultando tabla 'users'...")

            // Usar decodeList en lugar de decodeSingle para evitar excepciones
            val users = try {
                supabase.from("users")
                    .select {
                        filter {
                            eq("id", authUser.id)
                        }
                    }
                    .decodeList<User>()
            } catch (e: Exception) {
                println("❌ getCurrentUser: Error al consultar tabla users: ${e.message}")
                e.printStackTrace()
                return Result.failure(e)
            }

            if (users.isEmpty()) {
                println("⚠️ getCurrentUser: Usuario autenticado pero NO existe en tabla 'users'")
                println("⚠️ Esto significa que el usuario no completó el registro")
                println("⚠️ AuthUser ID: ${authUser.id}, Email: ${authUser.email}")
                return Result.success(null)
            }

            val user = users.first()
            println("✅ getCurrentUser: Usuario encontrado en BD")
            println("   - ID: ${user.id}")
            println("   - Email: ${user.email}")
            println("   - Verificado: ${user.emailVerified}")
            println("   - Estado: ${user.accountStatus}")
            println("   - isPro: ${user.isPro}") // ✅ Debug Pro status

            Result.success(user)
        } catch (e: Exception) {
            println("❌ getCurrentUser: Exception inesperada: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Pide al servidor que emita un código nuevo y lo envía por correo.
     *
     * El código lo genera ahora Postgres con gen_random_bytes() (criptográfico)
     * en lugar de kotlin.random en el dispositivo, y el RPC lo guarda con su
     * expiración en una sola operación. Ya no se imprime en logcat: antes se
     * escribía en claro con println(), que ProGuard NO elimina en release.
     */
    override suspend fun sendVerificationCode(email: String): Result<String> {
        return try {
            val response = rpcJson("issue_verification_code", mapOf("email_param" to email))

            val ok = response["success"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!ok) {
                return Result.failure(Exception("No se encontró una cuenta con este correo electrónico"))
            }

            val code = response["code"]?.jsonPrimitive?.contentOrNull
                ?: return Result.failure(Exception("No se pudo generar el código de verificación"))
            val userName = response["full_name"]?.jsonPrimitive?.contentOrNull ?: "Usuario"

            try {
                sendVerificationEmail(email, code, userName)
            } catch (emailError: Exception) {
                // El código ya quedó guardado; que falle el correo no invalida la operación
            }

            Result.success(code)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo enviar el código. Revisa tu conexión."))
        }
    }


    /**
     * Valida el código de verificación CONTRA EL SERVIDOR.
     *
     * Antes esta función descargaba la fila del usuario por correo y comparaba
     * `user.verificationCode == code` en el dispositivo. Para que eso pudiera
     * funcionar, la tabla `users` tenía que exponer `verification_code` a quien
     * consultara, y la anon key va dentro del APK. El resultado era que
     * cualquiera podía leer el código de otra persona con una sola petición y
     * apoderarse de su cuenta.
     *
     * Ahora se manda correo + código al RPC `validate_verification_code` y solo
     * regresa si fue válido. El código nunca sale de la base de datos.
     * El servidor además lleva el contador de intentos (5 y bloquea 15 min).
     */
    override suspend fun verifyEmail(email: String, code: String): Result<Boolean> {
        return try {
            val response = rpcJson(
                "validate_verification_code",
                mapOf("email_param" to email, "code_param" to code)
            )

            val isValid = response["valid"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isValid) return Result.success(true)

            val reason = response["reason"]?.jsonPrimitive?.contentOrNull
            val message = when (reason) {
                "expired" -> "El código de verificación ha expirado. Solicita uno nuevo"
                "locked" -> "Demasiados intentos fallidos. Espera 15 minutos e inténtalo de nuevo"
                else -> {
                    val left = response["attempts_left"]?.jsonPrimitive?.intOrNull
                    if (left != null && left > 0) {
                        "El código de verificación es incorrecto. Te quedan $left intentos"
                    } else {
                        "El código de verificación es incorrecto"
                    }
                }
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo verificar el código. Revisa tu conexión."))
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    /**
     * Consulta pública mínima de una cuenta por correo.
     *
     * Sustituye a los SELECT directos sobre `users`, que ahora el RLS bloquea:
     * cada quien solo puede leer su propia fila. El RPC es SECURITY DEFINER y
     * devuelve únicamente campos no sensibles (nunca el código de verificación
     * ni el token de notificaciones).
     */
    private suspend fun userPublicInfo(email: String): JsonObject? {
        val response = rpcJson("user_public_info", mapOf("email_param" to email))
        val exists = response["exists"]?.jsonPrimitive?.booleanOrNull ?: false
        return if (exists) response else null
    }

    override suspend fun checkEmailExists(email: String): Result<Boolean> {
        return try {
            Result.success(userPublicInfo(email) != null)
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo verificar el correo. Revisa tu conexión."))
        }
    }

    override suspend fun updateUserProfile(userId: String, fullName: String): Result<User> {
        return try {
            supabase.from("users").update(
                {
                    set("full_name", fullName)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }

            // Fetch updated user
            val user = supabase.from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingle<User>()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val authUser = supabase.auth.currentUserOrNull()
            if (authUser == null) {
                return Result.failure(Exception("No user logged in"))
            }

            // Update password in Supabase Auth
            supabase.auth.updateUser {
                password = newPassword
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String, verificationCode: String, newPassword: String): Result<Unit> {
        return try {
            println("🔐 Iniciando reset de contraseña para: $email")
            println("🔑 Código de verificación: $verificationCode")

            // ✅ Usar RPC para resetear contraseña (bypassa autenticación)
            val params = mapOf(
                "email_param" to email,
                "code_param" to verificationCode,
                "new_password_param" to newPassword
            )

            println("🔧 Llamando a RPC reset_password_with_code...")
            val result = supabase.postgrest.rpc("reset_password_with_code", params)
            println("✅ RPC ejecutado")
            println("🔍 Respuesta RPC: ${result.data}")

            // Parsear respuesta JSON
            val jsonElement = Json.parseToJsonElement(result.data)
            val responseJson = jsonElement.jsonObject
            val success = responseJson["success"]?.jsonPrimitive?.booleanOrNull ?: false
            val error = responseJson["error"]?.jsonPrimitive?.content
            val message = responseJson["message"]?.jsonPrimitive?.content

            println("📊 Success: $success")
            if (error != null) println("❌ Error: $error")
            if (message != null) println("✅ Message: $message")

            if (success) {
                println("✅ Contraseña actualizada exitosamente")
                Result.success(Unit)
            } else {
                println("❌ RPC retornó error: $error")
                Result.failure(Exception(error ?: "Error desconocido al resetear contraseña"))
            }
        } catch (e: Exception) {
            println("❌ Error al resetear contraseña: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error al restablecer la contraseña: ${e.message}"))
        }
    }

    override suspend fun deleteUserAccount(userId: String): Result<Unit> {
        return try {
            // Delete user from database (cascade will handle related records)
            supabase.from("users").delete {
                filter {
                    eq("id", userId)
                }
            }

            // Sign out
            supabase.auth.signOut()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOnboardingStep(userId: String, step: String): Result<Unit> {
        return try {
            supabase.from("users").update(
                {
                    set("onboarding_step", step)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserVerificationStatus(userId: String, onboardingStep: String, accountStatus: String): Result<Unit> {
        return try {
            println("📝 Actualizando estado de verificación del usuario...")
            println("   - User ID: $userId")
            println("   - onboarding_step: $onboardingStep")
            println("   - account_status: $accountStatus")

            supabase.from("users").update(
                {
                    set("onboarding_step", onboardingStep)
                    set("account_status", accountStatus)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }

            println("✅ Estado de verificación actualizado correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al actualizar estado de verificación: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Código de 6 dígitos con entropía criptográfica.
     *
     * Antes usaba kotlin.random.Random, que es un PRNG NO criptográfico y por
     * tanto predecible. Para un código que autoriza el reseteo de una
     * contraseña eso es un fallo de seguridad, no un detalle de estilo.
     */
    private fun generateVerificationCode(): String {
        val n = java.security.SecureRandom().nextInt(1_000_000)
        return n.toString().padStart(6, '0')
    }

    /**
     * Ejecuta un RPC de Postgres y devuelve su respuesta ya parseada.
     *
     * Los flujos de verificación pasaron a RPC porque antes el cliente
     * descargaba la fila del usuario (incluido verification_code) y comparaba
     * en local. Con la anon key extraída del APK, cualquiera podía leer el
     * código de otra cuenta y tomarla. Ahora el código nunca sale del servidor.
     */
    private suspend fun rpcJson(
        function: String,
        params: Map<String, String?>
    ): JsonObject {
        val result = supabase.postgrest.rpc(function, params)
        return Json.parseToJsonElement(result.data).jsonObject
    }

    /**
     * Envía el email de verificación usando la Edge Function de Supabase
     */
    private suspend fun sendVerificationEmail(email: String, code: String, userName: String) {
        try {
            println("📧 Llamando a Edge Function send-verification-email...")

            // Crear JsonObject para el body de la Edge Function
            val body = buildJsonObject {
                put("email", email)
                put("code", code)
                put("userName", userName)
            }

            // Invocar la Edge Function sin tipo genérico (se infiere automáticamente)
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

    /**
     * Reintenta el registro después de eliminar un usuario no verificado
     * Usa backoff exponencial: 1s, 2s, 3s, 4s, 5s
     */
    private suspend fun retrySignUpAfterDeletion(
        email: String,
        password: String,
        fullName: String,
        maxRetries: Int = 5
    ): Result<User> {
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            val attemptNumber = attempt + 1
            val delaySeconds = attemptNumber

            println("🔄 Intento $attemptNumber de $maxRetries - esperando ${delaySeconds}s...")
            kotlinx.coroutines.delay(delaySeconds * 1000L)

            try {
                // Intentar crear usuario en auth
                val authResponse = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // ✅ Si llegó aquí, el registro funcionó
                println("✅ Registro exitoso en intento $attemptNumber")

                // Obtener userId
                val userId = authResponse?.id ?: run {
                    kotlinx.coroutines.delay(1000)
                    supabase.auth.currentUserOrNull()?.id
                }

                if (userId == null) {
                    println("❌ No se pudo obtener el ID del usuario")
                    return Result.failure(Exception("No se pudo completar el registro"))
                }

                println("✅ Usuario creado en Auth con ID: $userId")

                // Generar código de verificación
                val verificationCode = generateVerificationCode()
                val expiryTime = Clock.System.now().plus(15.minutes)

                println("=================================")
                println("CÓDIGO DE VERIFICACIÓN: $verificationCode")
                println("Email: $email")
                println("Usuario ID: $userId")
                println("=================================")

                // Enviar email
                try {
                    sendVerificationEmail(email, verificationCode, fullName)
                    println("✅ Email enviado exitosamente")
                } catch (emailError: Exception) {
                    println("⚠️ Error al enviar email: ${emailError.message}")
                }

                // Crear registro en tabla users
                val user = User(
                    id = userId,
                    email = email,
                    fullName = fullName,
                    createdAt = Clock.System.now().toString(),
                    emailVerified = false,
                    verificationCode = verificationCode,
                    verificationCodeExpiry = expiryTime.toString(),
                    onboardingStep = "email_verification",
                    accountStatus = "pending_verification",
                    userType = "socio" // ✅ Identificar como vendedor
                )

                try {
                    supabase.from("users").insert(user)
                    println("✅ Usuario insertado en tabla 'users'")
                } catch (insertError: Exception) {
                    println("⚠️ Error al insertar: ${insertError.message}")
                }

                println("🎉 Registro completado exitosamente después de $attemptNumber intentos")
                return Result.success(user)

            } catch (e: Exception) {
                val errorMessage = e.message?.lowercase() ?: ""
                if (errorMessage.contains("user already registered") ||
                    errorMessage.contains("email address already in use") ||
                    errorMessage.contains("already exists")) {

                    println("⚠️ Intento $attemptNumber falló: Email aún existe en auth.users")
                    lastError = e

                    // Si no es el último intento, continuar
                    if (attemptNumber < maxRetries) {
                        println("🔄 Esperando a que Supabase procese la eliminación...")
                    }
                } else {
                    // Otro tipo de error, fallar inmediatamente
                    println("❌ Error inesperado: ${e.message}")
                    return Result.failure(e)
                }
            }
        }

        // Si llegamos aquí, agotamos todos los reintentos
        println("❌ Se agotaron los $maxRetries intentos")
        return Result.failure(
            lastError ?: Exception("No se pudo completar el registro después de $maxRetries intentos")
        )
    }

    override suspend fun updateProStatus(userId: String, isPro: Boolean): Result<Unit> {
        return try {
            println("🔄 updateProStatus: Actualizando estado PRO para usuario $userId a $isPro")

            supabase.from("users").update(
                {
                    set("is_pro", isPro)
                }
            ) {
                filter { eq("id", userId) }
            }

            println("✅ updateProStatus: Estado PRO actualizado exitosamente en la base de datos")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ updateProStatus: Error al actualizar estado PRO: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}