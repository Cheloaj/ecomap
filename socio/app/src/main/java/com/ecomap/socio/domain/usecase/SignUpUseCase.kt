package com.ecomap.socio.domain.usecase

import com.ecomap.socio.data.model.User
import com.ecomap.socio.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, fullName: String): Result<User> {
        // Validation
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Email inválido"))
        }

        if (password.length < 6) {
            return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
        }

        if (fullName.length < 3) {
            return Result.failure(Exception("El nombre debe tener al menos 3 caracteres"))
        }

        return authRepository.signUp(email, password, fullName)
    }
}
