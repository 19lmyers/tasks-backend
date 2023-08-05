package dev.chara.tasks.backend.web.routes.api

import dev.chara.tasks.backend.ApplicationError
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.web.WebError
import dev.chara.tasks.backend.web.logError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

suspend fun PipelineContext<Unit, ApplicationCall>.handleError(error: ApplicationError) {
    logError(error)

    when (error) {
        WebError.PrincipalInvalid,
        DomainError.AccessTokenInvalid,
        DomainError.UserNotFound -> {
            call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
        }
        WebError.InputInvalid -> {
            call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
        }
        is WebError.ParameterMissing -> {
            call.respondText("Missing parameter ${error.name}", status = HttpStatusCode.BadRequest)
        }
        DomainError.ListNotFound -> {
            call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
        }
        DomainError.ListTitleRequired -> {
            call.respondText("Title cannot be blank", status = HttpStatusCode.BadRequest)
        }
        DomainError.TaskNotFound -> {
            call.respondText("No task with ID", status = HttpStatusCode.BadRequest)
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
