package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.domain.service.TaskListService
import dev.chara.tasks.backend.domain.service.TaskService
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
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("TasksKt")

fun Route.tasks() {
    val userService: UserService by inject()
    val taskListService: TaskListService by inject()
    val taskService: TaskService by inject()

    route("/lists/{listId}/tasks") {
        get { getTasksByList(userService, taskListService, taskService) }
        post { postTasksByList(userService, taskListService, taskService) }
        post("/clear") { clearTasksByList(userService, taskListService, taskService) }
        route("/{taskId}") {
            get { getTaskByIds(userService, taskListService, taskService) }
            put { putTaskByIds(userService, taskListService, taskService) }
            delete { deleteTaskByIds(userService, taskListService, taskService) }
            post("/move") { moveTaskByIds(userService, taskListService, taskService) }
            post("/reorder") { reorderTaskByIds(userService, taskListService, taskService) }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    taskService.getTasksByList(userId, listId).bind()
}.mapBoth(
    success = { tasks ->
        call.respond(tasks)
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
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.postTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val task = runCatching { call.receive<Task>() }
        .mapError { WebError.InputInvalid }
        .bind()

    taskService.insert(userId, listId, task).bind()
}.mapBoth(
    success = { id ->
        call.respondText("Task $id created", status = HttpStatusCode.Created)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
                call.respondText("Invalid user", status = HttpStatusCode.Unauthorized)
            }

            WebError.InputInvalid -> {
                call.respondText("Invalid task", status = HttpStatusCode.BadRequest)
            }

            is WebError.ParameterMissing -> {
                call.respondText("Missing parameter ${error.name}", status = HttpStatusCode.BadRequest)
            }

            DomainError.ListNotFound -> {
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.clearTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    taskService.clearCompletedTasksByList(userId, listId).bind()
}.mapBoth(
    success = {
        call.respondText("Task list cleared", status = HttpStatusCode.OK)
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
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.getTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskId = call.parameters.getAsResult("taskId").bind()

    taskService.getTaskByIds(userId, listId, taskId).bind()
}.mapBoth(
    success = { task ->
        call.respond(task)
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
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.putTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskId = call.parameters.getAsResult("taskId")
        .andThen { taskId ->
            taskService.getIdFor(userId, listId, taskId)
        }.bind()

    val task = runCatching { call.receive<Task>() }
        .mapError { WebError.InputInvalid }
        .bind()

    taskService.update(userId, listId, taskId, task).bind()
}.mapBoth(
    success = {
        call.respondText("Task updated", status = HttpStatusCode.OK)
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
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
            }

            DomainError.TaskNotFound -> {
                call.respondText("No task with ID", status = HttpStatusCode.BadRequest)
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

suspend fun PipelineContext<Unit, ApplicationCall>.deleteTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskId = call.parameters.getAsResult("taskId")
        .andThen { taskId ->
            taskService.getIdFor(userId, listId, taskId)
        }.bind()

    taskService.delete(userId, listId, taskId).bind()
}.mapBoth(
    success = {
        call.respondText("Task deleted", status = HttpStatusCode.OK)
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
                call.respondText("No task list with ID", status = HttpStatusCode.BadRequest)
            }

            DomainError.TaskNotFound -> {
                call.respondText("No task with ID", status = HttpStatusCode.BadRequest)
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
data class Move(val newListId: String, val lastModified: Instant)

suspend fun PipelineContext<Unit, ApplicationCall>.moveTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val move = runCatching { call.receive<Move>() }
        .mapError { WebError.InputInvalid }
        .bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskId = call.parameters.getAsResult("taskId")
        .andThen { taskId ->
            taskService.getIdFor(userId, listId, taskId)
        }.bind()

    val newListId = taskListService.getIdFor(userId, move.newListId).bind()

    taskService.move(userId, newListId, taskId, move.lastModified).bind()
}.mapBoth(
    success = {
        call.respondText("Task moved", status = HttpStatusCode.OK)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
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

            DomainError.TaskNotFound -> {
                call.respondText("No task with ID", status = HttpStatusCode.BadRequest)
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
data class Reorder(val fromIndex: Int, val toIndex: Int, val lastModified: Instant)

suspend fun PipelineContext<Unit, ApplicationCall>.reorderTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) = binding {
    val userId = call.principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal ->
            userService.getIdFor(principal)
        }.bind()

    val reorder = runCatching { call.receive<Reorder>() }
        .mapError { WebError.InputInvalid }
        .bind()

    val listId = call.parameters.getAsResult("listId")
        .andThen { listId ->
            taskListService.getIdFor(userId, listId)
        }.bind()

    val taskId = call.parameters.getAsResult("taskId")
        .andThen { taskId ->
            taskService.getIdFor(userId, listId, taskId)
        }.bind()

    taskService.reorder(userId, listId, taskId, reorder.fromIndex, reorder.toIndex, reorder.lastModified).bind()
}.mapBoth(
    success = {
        call.respondText("Task list reordered", status = HttpStatusCode.OK)
    },
    failure = { error ->
        when (error) {
            WebError.PrincipalInvalid, DomainError.AccessTokenInvalid, DomainError.UserNotFound -> {
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

            DomainError.TaskNotFound -> {
                call.respondText("No task with ID", status = HttpStatusCode.BadRequest)
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