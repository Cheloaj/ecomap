package com.ecomap.socio.domain.repository

import com.ecomap.socio.data.model.User

interface AuthRepository {
    suspend fun signUp(email: String, password: String, fullName: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun sendVerificationCode(email: String): Result<String>
    suspend fun verifyEmail(email: String, code: String): Result<Boolean>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun checkEmailExists(email: String): Result<Boolean>
    suspend fun updateUserProfile(userId: String, fullName: String): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(email: String, verificationCode: String, newPassword: String): Result<Unit>
    suspend fun deleteUserAccount(userId: String): Result<Unit>
    suspend fun updateOnboardingStep(userId: String, step: String): Result<Unit>
    suspend fun updateUserVerificationStatus(userId: String, onboardingStep: String, accountStatus: String): Result<Unit>
    suspend fun updateProStatus(userId: String, isPro: Boolean): Result<Unit>
}
