package dev.chara.tasks.backend.web.routes

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.service.FirebaseTokenService
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.web.WebError
import dev.chara.tasks.backend.web.logError
import dev.chara.tasks.backend.web.logTrace
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject

fun Route.fcm() {
    val userService: UserService by inject()
    val firebaseTokenService: FirebaseTokenService by inject()

    route("/fcm") {
        authenticate {
            post("/link") {
                logTrace("Linking FCM token")
                linkFcmToken(userService, firebaseTokenService)
            }
            post("/invalidate") {
                logTrace("Invalidating FCM token")
                invalidateFcmToken(userService, firebaseTokenService)
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.linkFcmToken(
    userService: UserService,
    firebaseTokenService: FirebaseTokenService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val firebaseToken =
                runCatching { call.receiveText() }.mapError { WebError.InputInvalid }.bind()

            firebaseTokenService.link(userId, firebaseToken).bind()
        }
        .mapBoth(
            success = {
                call.respondText(
                    "Firebase Cloud Messaging token linked",
                    status = HttpStatusCode.OK
                )
            },
            failure = { error ->
                logError(error)

                when (error) {
                    WebError.PrincipalInvalid,
                    DomainError.AccessTokenInvalid,
                    DomainError.UserNotFound -> {
                        call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
                    }
                    WebError.InputInvalid -> {
                        call.respondText("Invalid FCM token", status = HttpStatusCode.BadRequest)
                    }
                    DomainError.FirebaseTokenRequired -> {
                        call.respondText(
                            "FCM Token cannot be blank",
                            status = HttpStatusCode.BadRequest
                        )
                    }
                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logError(error.throwable)
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

suspend fun PipelineContext<Unit, ApplicationCall>.invalidateFcmToken(
    userService: UserService,
    firebaseTokenService: FirebaseTokenService
) =
    binding {
            call
                .principal<JWTPrincipal>()
                .toResultOr { WebError.PrincipalInvalid }
                .andThen { principal -> userService.getIdFor(principal) }
                .bind()

            val firebaseToken =
                runCatching { call.receiveText() }.mapError { WebError.InputInvalid }.bind()

            firebaseTokenService.invalidate(firebaseToken).bind()
        }
        .mapBoth(
            success = {
                call.respondText(
                    "Firebase Cloud Messaging token invalidated",
                    status = HttpStatusCode.OK
                )
            },
            failure = { error ->
                logError(error)

                when (error) {
                    WebError.PrincipalInvalid,
                    DomainError.AccessTokenInvalid,
                    DomainError.UserNotFound -> {
                        call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
                    }
                    WebError.InputInvalid -> {
                        call.respondText("Invalid FCM token", status = HttpStatusCode.BadRequest)
                    }
                    DomainError.FirebaseTokenRequired -> {
                        call.respondText(
                            "FCM Token cannot be blank",
                            status = HttpStatusCode.BadRequest
                        )
                    }
                    is DataError -> {
                        call.respondText(
                            "An unexpected error occurred",
                            status = HttpStatusCode.InternalServerError
                        )
                        logError(error.throwable)
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
