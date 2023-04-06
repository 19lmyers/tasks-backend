package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.Profile
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.web.WebError
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("ProfileKt")

fun Route.profile() {
    val userService: UserService by inject()

    route("/profile") {
        get { get(userService) }
        put { put(userService) }
        post("/photo") { uploadPhoto(userService) }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.get(userService: UserService) =
    call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal -> userService.getIdFor(principal) }
        .andThen { id -> userService.getAsProfile(id) }
        .mapBoth(
            success = { profile ->
                call.respond(profile)
            },
            failure = { error ->

                when (error) {
                    WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                        call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
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

suspend fun PipelineContext<Unit, ApplicationCall>.put(userService: UserService) {
    binding {
        val userId = call.principal<JWTPrincipal>()
            .toResultOr { WebError.PrincipalInvalid }
            .andThen { principal ->
                userService.getIdFor(principal)
            }
            .bind()

        val profile = runCatching { call.receive<Profile>() }
            .mapError { WebError.InputInvalid }
            .bind()

        userService.updateProfile(userId, profile)
    }.mapBoth(
        success = {
            call.respondText("Profile updated", status = HttpStatusCode.OK)
        },
        failure = { error ->
            when (error) {
                WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                    call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
                }

                WebError.InputInvalid -> {
                    call.respondText("Invalid profile", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.uploadPhoto(userService: UserService) {
    binding {
        val userId = call.principal<JWTPrincipal>()
            .toResultOr { WebError.PrincipalInvalid }
            .andThen { principal ->
                userService.getIdFor(principal)
            }
            .bind()

        val photoBytes = runCatching { call.receiveMultipart() }
            .mapError { WebError.InputInvalid }
            .andThen { multiPartData ->
                multiPartData.readPart().toResultOr { WebError.InputInvalid }
            }
            .andThen { part ->
                if (part is PartData.FileItem) {
                    Ok(part.streamProvider().readBytes())
                } else {
                    Err(WebError.InputInvalid)
                }
            }
            .bind()

        userService.uploadProfilePhoto(userId, photoBytes).bind()
    }.mapBoth(
        success = {
            call.respondText("Profile photo set", status = HttpStatusCode.OK)
        },
        failure = { error ->
            when (error) {
                WebError.InputInvalid -> {
                    call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
                }

                WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                    call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
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