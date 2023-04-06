package dev.chara.tasks.backend.domain

import dev.chara.tasks.backend.ApplicationError

sealed class DomainError : ApplicationError {
    object EmailInvalid : DomainError()

    class PasswordInvalid(val details: String) : DomainError()
    object PasswordIncorrect : DomainError()

    object UserNotFound : DomainError()
    class UserExists(val email: String) : DomainError()

    object AccessTokenInvalid : DomainError()
    object RefreshTokenInvalid : DomainError()

    object ResetTokenNotFound : DomainError()
    object ResetTokenExpired : DomainError()

    object ListTitleRequired : DomainError()
    object ListNotFound : DomainError()

    object TaskNotFound : DomainError()

    object FirebaseTokenRequired : DomainError()
}