package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.service.TaskListService
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.util.getAsResult
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
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("ListsKt")

fun Route.lists() {
    val userService: UserService by inject()
    val taskListService: TaskListService by inject()

    route("/lists") {
        get { getLists(userService, taskListService) }
        post { postList(userService, taskListService) }
        route("/{id}") {
            get { getListById(userService, taskListService) }
            put { putListById(userService, taskListService) }
            delete { deleteListById(userService, taskListService) }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getLists(
    userService: UserService,
    taskListService: TaskListService
) = call.principal<JWTPrincipal>()
    .toResultOr { WebError.PrincipalInvalid }
    .andThen { principal ->
        userService.getIdFor(principal)
    }.andThen { userId ->
        taskListService.getForUser(userId)
    }.mapBoth(
        success = { taskLists ->
            call.respond(taskLists)
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

suspend fun PipelineContext<Unit, ApplicationCall>.postList(
    userService: UserService,
    taskListService: TaskListService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val taskList = runCatching { call.receive<TaskList>() }
        .mapError { WebError.InputInvalid }
        .bind()

    taskListService.insert(userId, taskList).bind()
}.mapBoth(
    success = { id ->
        call.respondText("Task list $id created", status = HttpStatusCode.Created)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
            }

            WebError.InputInvalid -> {
                call.respondText("Invalid task list", status = HttpStatusCode.BadRequest)
            }

            DomainError.ListTitleRequired -> {
                call.respondText("Title cannot be blank", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.getListById(
    userService: UserService,
    taskListService: TaskListService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("id").bind()

    taskListService.getByIds(userId, listId).bind()
}.mapBoth(
    success = { list ->
        call.respond(list)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
            }

            is WebError.ParameterMissing -> {
                call.respondText("Missing parameter ${error.name}", status = HttpStatusCode.BadRequest)
            }

            DomainError.ListNotFound -> {
                call.respondText("No task list with that ID", status = HttpStatusCode.NotFound)
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


suspend fun PipelineContext<Unit, ApplicationCall>.putListById(
    userService: UserService,
    taskListService: TaskListService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("id")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskList = runCatching { call.receive<TaskList>() }
        .mapError { WebError.InputInvalid }
        .bind()

    taskListService.update(userId, listId, taskList).bind()
}.mapBoth(
    success = {
        call.respondText("Task list updated", status = HttpStatusCode.OK)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
            }

            is WebError.ParameterMissing -> {
                call.respondText("Missing parameter ${error.name}", status = HttpStatusCode.BadRequest)
            }

            DomainError.ListNotFound -> {
                call.respondText("No task list with that ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.deleteListById(
    userService: UserService,
    taskListService: TaskListService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("id")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    taskListService.delete(userId, listId).bind()
}.mapBoth(
    success = {
        call.respondText("Task list deleted", status = HttpStatusCode.Accepted)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
            }

            is WebError.ParameterMissing -> {
                call.respondText("Missing parameter ${error.name}", status = HttpStatusCode.BadRequest)
            }

            DomainError.ListNotFound -> {
                call.respondText("No task list with that ID", status = HttpStatusCode.BadRequest)
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