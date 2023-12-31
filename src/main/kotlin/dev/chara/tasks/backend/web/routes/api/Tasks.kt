package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.domain.model.Reorder
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.domain.service.TaskListService
import dev.chara.tasks.backend.domain.service.TaskService
import dev.chara.tasks.backend.domain.service.UserService
import dev.chara.tasks.backend.util.getAsResult
import dev.chara.tasks.backend.web.WebError
import dev.chara.tasks.backend.web.logTrace
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.tasks() {
    val userService: UserService by inject()
    val taskListService: TaskListService by inject()
    val taskService: TaskService by inject()

    route("/lists/{listId}/tasks") {
        get {
            logTrace("Fetching tasks for list")
            getTasksByList(userService, taskListService, taskService)
        }
        post {
            logTrace("Creating task")
            postTasksByList(userService, taskListService, taskService)
        }
        post("/clear") {
            logTrace("Clearing tasks for list")
            clearTasksByList(userService, taskListService, taskService)
        }
        route("/{taskId}") {
            get {
                logTrace("Fetching task by ID")
                getTaskByIds(userService, taskListService, taskService)
            }
            put {
                logTrace("Updating task")
                putTaskByIds(userService, taskListService, taskService)
            }
            delete {
                logTrace("Deleting task")
                deleteTaskByIds(userService, taskListService, taskService)
            }
            post("/move") {
                logTrace("Moving task")
                moveTaskByIds(userService, taskListService, taskService)
            }
            post("/reorder") {
                logTrace("Reordering tasks")
                reorderTaskByIds(userService, taskListService, taskService)
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskService.getTasksByList(listId).bind()
        }
        .mapBoth(
            success = { tasks -> call.respond(tasks) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.postTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val task =
                runCatching { call.receive<Task>() }.mapError { WebError.InputInvalid }.bind()

            taskService.insert(userId, listId, task).bind()
        }
        .mapBoth(
            success = { id ->
                call.respondText("Task $id created", status = HttpStatusCode.Created)
            },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.clearTasksByList(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskService.clearCompletedTasksByList(userId, listId).bind()
        }
        .mapBoth(
            success = { call.respondText("Task list cleared", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.getTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            @Suppress("UNUSED_VARIABLE")
            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskId = call.parameters.getAsResult("taskId").bind()

            taskService.getTaskById(taskId).bind()
        }
        .mapBoth(
            success = { task -> call.respond(task) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.putTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            @Suppress("UNUSED_VARIABLE")
            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskId = call.parameters.getAsResult("taskId").bind()

            val task =
                runCatching { call.receive<Task>() }.mapError { WebError.InputInvalid }.bind()

            taskService.update(userId, listId, taskId, task).bind()
        }
        .mapBoth(
            success = { call.respondText("Task updated", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.deleteTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskId = call.parameters.getAsResult("taskId").bind()

            taskService.delete(userId, listId, taskId).bind()
        }
        .mapBoth(
            success = { call.respondText("Task deleted", status = HttpStatusCode.Accepted) },
            failure = { error -> handleError(error) }
        )

@Serializable data class Move(val newListId: String, val lastModified: Instant)

suspend fun PipelineContext<Unit, ApplicationCall>.moveTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val move =
                runCatching { call.receive<Move>() }.mapError { WebError.InputInvalid }.bind()

            @Suppress("UNUSED_VARIABLE")
            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskId = call.parameters.getAsResult("taskId").bind()

            val newListId = taskListService.ensureUserAccess(userId, move.newListId).bind()

            taskService.move(newListId, taskId, move.lastModified).bind()
        }
        .mapBoth(
            success = { call.respondText("Task moved", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.reorderTaskByIds(
    userService: UserService,
    taskListService: TaskListService,
    taskService: TaskService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val reorder =
                runCatching { call.receive<Reorder>() }.mapError { WebError.InputInvalid }.bind()

            val listId =
                call.parameters
                    .getAsResult("listId")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskId = call.parameters.getAsResult("taskId").bind()

            taskService
                .reorder(listId, taskId, reorder.fromIndex, reorder.toIndex, reorder.lastModified)
                .bind()
        }
        .mapBoth(
            success = { call.respondText("Task list reordered", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )
