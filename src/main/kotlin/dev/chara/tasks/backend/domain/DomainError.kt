package dev.chara.tasks.backend.domain

import dev.chara.tasks.backend.ApplicationError

sealed class DomainError : ApplicationError {
    data object EmailInvalid : DomainError()

    data object EmailIncorrect : DomainError()

    data class PasswordInvalid(val details: String) : DomainError()

    data object PasswordIncorrect : DomainError()

    data object UserNotFound : DomainError()

    data class UserExists(val email: String) : DomainError()

    data object AccessTokenInvalid : DomainError()

    data object RefreshTokenInvalid : DomainError()

    data object EmailUnverified : DomainError()

    data object EmailVerified : DomainError()

    data object VerifyTokenNotFound : DomainError()

    data object VerifyTokenExpired : DomainError()

    data object ResetTokenNotFound : DomainError()

    data object ResetTokenExpired : DomainError()

    data object ListTitleRequired : DomainError()

    data object ListNotFound : DomainError()

    data object TaskNotFound : DomainError()

    data object FirebaseTokenRequired : DomainError()

    data object RateLimitExceeded : DomainError()
}
