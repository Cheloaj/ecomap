package com.ecomap.socio.domain.usecase

import com.ecomap.socio.data.model.User
import com.ecomap.socio.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.failure(Exception("El email no puede estar vacío"))
        }

        if (password.isBlank()) {
            return Result.failure(Exception("La contraseña no puede estar vacía"))
        }

        return authRepository.signIn(email, password)
    }
}
