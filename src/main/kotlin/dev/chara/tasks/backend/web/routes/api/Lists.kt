package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.service.TaskListService
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
import org.koin.ktor.ext.inject

fun Route.lists() {
    val userService: UserService by inject()
    val taskListService: TaskListService by inject()

    route("/lists") {
        get {
            logTrace("Fetching lists")
            getLists(userService, taskListService)
        }
        post {
            logTrace("Creating list")
            postList(userService, taskListService)
        }
        route("/{id}") {
            get {
                logTrace("Fetching list")
                getListById(userService, taskListService)
            }
            put {
                logTrace("Updating list")
                putListById(userService, taskListService)
            }
            delete {
                logTrace("Deleting list")
                deleteListById(userService, taskListService)
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getLists(
    userService: UserService,
    taskListService: TaskListService
) =
    call
        .principal<JWTPrincipal>()
        .toResultOr { WebError.PrincipalInvalid }
        .andThen { principal -> userService.getIdFor(principal) }
        .andThen { userId -> taskListService.getForUser(userId) }
        .mapBoth(
            success = { taskLists -> call.respond(taskLists) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.postList(
    userService: UserService,
    taskListService: TaskListService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val taskList =
                runCatching { call.receive<TaskList>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.insert(userId, taskList).bind()
        }
        .mapBoth(
            success = { id ->
                call.respondText("Task list $id created", status = HttpStatusCode.Created)
            },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.getListById(
    userService: UserService,
    taskListService: TaskListService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .bind()

            val listId = call.parameters.getAsResult("id").bind()

            taskListService.getByIds(userId, listId).bind()
        }
        .mapBoth(
            success = { list -> call.respond(list) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.putListById(
    userService: UserService,
    taskListService: TaskListService
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
                    .getAsResult("id")
                    .andThen { listId -> taskListService.getIdFor(userId, listId) }
                    .bind()

            val taskList =
                runCatching { call.receive<TaskList>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.update(userId, listId, taskList).bind()
        }
        .mapBoth(
            success = { call.respondText("Task list updated", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.deleteListById(
    userService: UserService,
    taskListService: TaskListService
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
                    .getAsResult("id")
                    .andThen { listId -> taskListService.getIdFor(userId, listId) }
                    .bind()

            taskListService.delete(userId, listId).bind()
        }
        .mapBoth(
            success = { call.respondText("Task list deleted", status = HttpStatusCode.Accepted) },
            failure = { error -> handleError(error) }
        )
