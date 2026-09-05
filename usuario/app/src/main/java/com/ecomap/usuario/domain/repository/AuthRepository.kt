package com.ecomap.usuario.domain.repository

import com.ecomap.usuario.data.model.User

interface AuthRepository {
    suspend fun signUp(email: String, password: String, fullName: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun isUserSignedIn(): Boolean
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun verifyCode(email: String, code: String): Result<User>
    suspend fun checkEmailExists(email: String): Result<Boolean>
    suspend fun resendVerificationCode(email: String): Result<Unit>
    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit>
}
