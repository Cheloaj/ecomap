package com.ecomap.socio.domain.usecase

import com.ecomap.socio.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String): Result<Boolean> {
        if (code.length != 6) {
            return Result.failure(Exception("El código debe tener 6 dígitos"))
        }

        return authRepository.verifyEmail(email, code)
    }
}
