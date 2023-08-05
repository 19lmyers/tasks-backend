package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.domain.model.Profile
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.web.WebError
import dev.chara.tasks.backend.web.logTrace
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject

fun Route.profile() {
    val userService: UserService by inject()

    route("/profile") {
        get {
            logTrace("Fetching profile")
            get(userService)
        }
        put {
            logTrace("Updating profile")
            put(userService)
        }
        post("/photo") {
            logTrace("Uploading profile photo")
            uploadPhoto(userService)
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.get(userService: UserService) =
    call
        .principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal -> userService.getIdFor(principal) }
        .andThen { id -> userService.getAsProfile(id) }
        .mapBoth(
            success = { profile -> call.respond(profile) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.put(userService: UserService) {
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val profile =
                runCatching { call.receive<Profile>() }.mapError { WebError.InputInvalid }.bind()

            userService.updateProfile(userId, profile)
        }
        .mapBoth(
            success = { call.respondText("Profile updated", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )
}

suspend fun PipelineContext<Unit, ApplicationCall>.uploadPhoto(userService: UserService) {
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val photoBytes =
                runCatching { call.receiveMultipart() }
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
        }
        .mapBoth(
            success = { call.respondText("Profile photo set", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )
}
