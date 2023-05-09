package dev.chara.tasks.backend.domain.service

import com.chrynan.validator.EmailValidator
import com.github.michaelbull.result.*
import dev.chara.tasks.backend.ApplicationError
import dev.chara.tasks.backend.data.AssetManager
import dev.chara.tasks.backend.data.Mail
import dev.chara.tasks.backend.data.MailSender
import dev.chara.tasks.backend.data.repository.UserRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.auth.JwtProvider
import dev.chara.tasks.backend.domain.auth.PasswordValidator
import dev.chara.tasks.backend.domain.auth.RandomTokenGenerator
import dev.chara.tasks.backend.domain.auth.SecretHasher
import dev.chara.tasks.backend.domain.model.Profile
import dev.chara.tasks.backend.domain.model.toModel
import dev.chara.tasks.backend.domain.model.toProfile
import dev.chara.tasks.backend.util.toErrorIfNotNull
import dev.chara.tasks.backend.util.toErrorIfNull
import dev.chara.tasks.backend.util.toResult
import io.ktor.server.auth.jwt.*
import kotlinx.datetime.Clock

class UserService(
    private val assetManager: AssetManager,
    private val repository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val mailSender: MailSender
) {
    private val secretHasher = SecretHasher()
    private val randomTokenGenerator = RandomTokenGenerator()

    private val emailValidator = EmailValidator()
    private val passwordValidator = PasswordValidator()

    fun create(email: String, displayName: String, password: String) = binding {
        val validEmail = emailValidator.validate(email).toResult()
            .mapError {
                DomainError.EmailInvalid
            }
            .bind()

        val validPassword = passwordValidator.validate(password).toResult()
            .mapError { error -> DomainError.PasswordInvalid(error.details ?: "Improper password") }
            .bind()

        repository.getByEmail(validEmail)
            .toErrorIfNotNull { user ->
                DomainError.UserExists(user.email)
            }
            .bind()

        val hashedPassword = secretHasher.hash(validPassword)
        repository.insert(validEmail, displayName, hashedPassword)
    }

    fun authenticate(email: String, password: String) =
        emailValidator.validate(email).toResult()
            .mapError { DomainError.EmailInvalid }
            .andThen { repository.getByEmail(email) }
            .toErrorIfNull {
                DomainError.UserNotFound
            }
            .andThen { user ->
                if (secretHasher.verify(password, user.hashed_password)) {
                    Ok(user)
                } else {
                    Err(DomainError.PasswordIncorrect)
                }
            }
            .andThen { user ->
                Ok(jwtProvider.generateTokenPair(user.id))
            }

    fun refresh(token: String) =
        jwtProvider.verify(token, JwtProvider.TokenType.REFRESH)
            .mapError { DomainError.RefreshTokenInvalid }
            .andThen { jwt ->
                jwt.getClaim(JwtProvider.CLAIM_ID).asString().toResultOr { DomainError.RefreshTokenInvalid }
            }
            .andThen { id -> repository.getById(id) }
            .toErrorIfNull {
                DomainError.UserNotFound
            }.andThen { user -> Ok(jwtProvider.generateTokenPair(user.id)) }

    fun getIdFor(principal: JWTPrincipal): Result<String, ApplicationError> {
        return if (principal.subject == JwtProvider.ACCESS_SUBJECT) {
            val id =
                principal.getClaim(JwtProvider.CLAIM_ID, String::class) ?: return Err(DomainError.AccessTokenInvalid)

            repository.getById(id)
                .toErrorIfNull {
                    DomainError.UserNotFound
                }.map { it.id }
        } else {
            Err(DomainError.AccessTokenInvalid)
        }
    }

    fun getById(id: String) = repository.getById(id)
        .toErrorIfNull {
            DomainError.UserNotFound
        }.map { it.toModel() }

    fun getByEmail(email: String) = repository.getByEmail(email)
        .toErrorIfNull {
            DomainError.UserNotFound
        }
        .map { it.toModel() }

    fun getAsProfile(id: String) = repository.getById(id)
        .toErrorIfNull {
            DomainError.UserNotFound
        }.map { it.toProfile() }

    fun updateProfile(id: String, profile: Profile) =
        repository.update(id, profile.displayName, profile.profilePhotoUri)

    fun uploadProfilePhoto(id: String, photoBytes: ByteArray) =
        assetManager.saveProfilePhoto(id, photoBytes)
            .andThen { uri ->
                repository.updatePhoto(id, uri)
            }

    fun requestPasswordResetToken(id: String, email: String, displayName: String) =
        repository.insertPasswordResetToken(id, randomTokenGenerator.generateSecureToken())
            .andThen { token ->
                mailSender.send(
                    email, displayName, Mail(
                        "Password reset requested",
                        "To reset your password, please visit the following link on your mobile device: https://tasks.chara.dev/reset?token=$token"
                    )
                )
            }

    fun updatePassword(userId: String, currentPassword: String, newPassword: String) =
        passwordValidator.validate(newPassword).toResult()
            .mapError { DomainError.PasswordInvalid(it.details ?: "Improper password") }
            .toErrorIf(
                predicate = { validPassword ->
                    currentPassword == validPassword
                },
                transform = {
                    DomainError.PasswordInvalid("Passwords must not match")
                }
            )
            .andThen { repository.getById(userId) }
            .toErrorIfNull {
                DomainError.UserNotFound
            }.andThen { user ->
                if (secretHasher.verify(currentPassword, user.hashed_password)) {
                    Ok(Unit)
                } else {
                    Err(DomainError.PasswordIncorrect)
                }
            }.andThen {
                val hashedPassword = secretHasher.hash(newPassword)
                repository.updatePassword(userId, hashedPassword)
                Ok(Unit)
            }

    fun resetPassword(resetToken: String, newPassword: String) =
        passwordValidator.validate(newPassword).toResult()
            .mapError { DomainError.PasswordInvalid(it.details ?: "Improper password") }
            .andThen { repository.getPasswordResetToken(resetToken) }
            .toErrorIfNull { DomainError.ResetTokenNotFound }
            .toErrorIf(
                predicate = { token ->
                    token.expiry_time <= Clock.System.now()
                },
                transform = {
                    DomainError.ResetTokenExpired
                }
            ).andThen { token -> repository.getById(token.user_id) }
            .toErrorIfNull { DomainError.UserNotFound }
            .andThen { user ->
                val hashedPassword = secretHasher.hash(newPassword)
                repository.updatePassword(user.id, hashedPassword)
            }.andThen {
                repository.invalidatePasswordResetToken(resetToken)
            }
}