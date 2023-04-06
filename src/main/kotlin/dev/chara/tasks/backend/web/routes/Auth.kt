package dev.chara.tasks.backend.web.routes

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.web.WebError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("AuthKt")

fun Route.auth() {
    val userService: UserService by inject()

    route("/auth") {
        post("/register") {
            register(userService)
        }
        post("/login") {
            login(userService)
        }
        post("/refresh") {
            refresh(userService)
        }

        authenticate {
            // TODO change email endpoint(s)

            post("/password") {
                changePassword(userService)
            }
        }

        post("/forgot") {
            forgotPassword(userService)
        }
        post("/reset") {
            resetPassword(userService)
        }
    }
}

@Serializable
data class RegisterCredentials(val email: String, val displayName: String, val password: String)

suspend fun PipelineContext<Unit, ApplicationCall>.register(userService: UserService) =
    runCatching { call.receive<RegisterCredentials>() }
        .mapError { WebError.InputInvalid }
        .andThen { credentials -> userService.create(credentials.email, credentials.displayName, credentials.password) }
        .mapBoth(
            success = {
                call.respondText("User created", status = HttpStatusCode.Created)
            },
            failure = { error ->
                when (error) {
                    WebError.InputInvalid -> {
                        call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
                    }

                    DomainError.EmailInvalid -> {
                        call.respondText("Improper email", status = HttpStatusCode.BadRequest)
                    }

                    is DomainError.PasswordInvalid -> {
                        call.respondText(error.details, status = HttpStatusCode.BadRequest)
                    }

                    is DomainError.UserExists -> {
                        call.respondText(
                            "A user with email ${error.email} already exists",
                            status = HttpStatusCode.Conflict
                        )
                    }

                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logger.error(error.throwable)
                    }

                    else -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        )

@Serializable
data class LoginCredentials(val email: String, val password: String)

suspend fun PipelineContext<Unit, ApplicationCall>.login(userService: UserService) =
    runCatching { call.receive<LoginCredentials>() }
        .mapError { WebError.InputInvalid }
        .andThen { credentials -> userService.authenticate(credentials.email, credentials.password) }
        .mapBoth(
            success = { tokenPair ->
                call.respond(tokenPair)
            },
            failure = { error ->
                when (error) {
                    WebError.InputInvalid -> {
                        call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
                    }

                    DomainError.EmailInvalid -> {
                        call.respondText("Improper email", status = HttpStatusCode.BadRequest)
                    }

                    DomainError.UserNotFound, DomainError.PasswordIncorrect -> {
                        call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                    }

                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logger.error(error.throwable)
                    }

                    else -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.refresh(userService: UserService) =
    runCatching { call.receiveText() }
        .mapError { WebError.InputInvalid }
        .andThen { token -> userService.refresh(token) }
        .mapBoth(
            success = { tokenPair ->
                call.respond(tokenPair)
            },
            failure = { error ->
                when (error) {
                    WebError.InputInvalid, DomainError.RefreshTokenInvalid, DomainError.UserNotFound -> {
                        call.respondText("Invalid refresh token", status = HttpStatusCode.BadRequest)
                    }

                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logger.error(error.throwable)
                    }

                    else -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        )

@Serializable
data class PasswordChange(val currentPassword: String, val newPassword: String)

suspend fun PipelineContext<Unit, ApplicationCall>.changePassword(userService: UserService) {
    binding {
        val userId = call.principal<JWTPrincipal>()
            .toResultOr { WebError.PrincipalInvalid }
            .andThen { principal ->
                userService.getIdFor(principal)
            }
            .bind()

        val passwordChange = runCatching { call.receive<PasswordChange>() }
            .mapError { WebError.InputInvalid }
            .bind()

        userService.updatePassword(userId, passwordChange.currentPassword, passwordChange.newPassword)
    }.mapBoth(
        success = {
            call.respondText("Password changed", status = HttpStatusCode.OK)
        },
        failure = { error ->
            when (error) {
                WebError.InputInvalid -> {
                    call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
                }

                WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                    call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
                }

                is DomainError.PasswordInvalid -> {
                    call.respondText(error.details, status = HttpStatusCode.BadRequest)
                }

                DomainError.PasswordIncorrect -> {
                    call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                }

                is DataError -> {
                    call.respondText(
                        "An unexpected error occurred",
                        status = HttpStatusCode.InternalServerError
                    )
                    logger.error(error.throwable)
                }

                else -> {
                    call.respondText(
                        "An unexpected error occurred",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    )
}

private suspend fun PipelineContext<Unit, ApplicationCall>.forgotPassword(userService: UserService) =
    runCatching { call.receiveText() }
        .mapError { WebError.InputInvalid }
        .andThen { email -> userService.getByEmail(email) }
        .andThen { user -> userService.requestPasswordResetToken(user.id, user.email, user.displayName) }
        .mapBoth(
            success = {
                call.respondText("Password reset email sent", status = HttpStatusCode.Accepted)
            },
            failure = { error ->
                when (error) {
                    WebError.InputInvalid, DomainError.UserNotFound -> {
                        call.respondText("Invalid email", status = HttpStatusCode.BadRequest)
                    }

                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logger.error(error.throwable)
                    }

                    else -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        )

@Serializable
data class PasswordReset(val resetToken: String, val newPassword: String)

suspend fun PipelineContext<Unit, ApplicationCall>.resetPassword(userService: UserService) =
    runCatching { call.receive<PasswordReset>() }
        .mapError { WebError.InputInvalid }
        .andThen { passwordReset -> userService.resetPassword(passwordReset.resetToken, passwordReset.newPassword) }
        .mapBoth(
            success = {
                call.respondText("Password reset successful", status = HttpStatusCode.OK)
            },
            failure = { error ->
                when (error) {
                    WebError.InputInvalid -> {
                        call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
                    }

                    is DomainError.PasswordInvalid -> {
                        call.respondText(error.details, status = HttpStatusCode.BadRequest)
                    }

                    DomainError.ResetTokenNotFound, DomainError.UserNotFound, DomainError.ResetTokenExpired -> {
                        call.respondText("Invalid reset token", status = HttpStatusCode.BadRequest)
                    }

                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logger.error(error.throwable)
                    }

                    else -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
        )