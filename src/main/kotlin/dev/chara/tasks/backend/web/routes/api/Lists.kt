package dev.chara.tasks.backend.web.routes.api

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import dev.chara.tasks.backend.domain.model.Reorder
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.model.TaskListPrefs
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
import kotlinx.serialization.Serializable
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
            post("/reorder") {
                logTrace("Reordering lists")
                reorderListById(userService, taskListService)
            }
            route("/prefs") {
                get {
                    logTrace("Fetching list prefs")
                    getListPrefsById(userService, taskListService)
                }
                put {
                    logTrace("Updating list prefs")
                    putListPrefsById(userService, taskListService)
                }
            }
            get("/members") {
                logTrace("Fetching list members")
                getListMembers(userService, taskListService)
            }
            post("/invite") {
                logTrace("Requesting list invite")
                requestListInvite(userService, taskListService)
            }
            post("/leave") {
                logTrace("Leaving list")
                leaveList(userService, taskListService)
            }
            post("/remove") {
                logTrace("Removing member from list")
                removeMemberFromList(userService, taskListService)
            }
        }
        post("/invite") {
            logTrace("Fetching list invite details")
            getJoinInfo(userService, taskListService)
        }
        post("/join") {
            logTrace("Requesting list join")
            requestListJoin(userService, taskListService)
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

@Serializable data class Insert(val taskList: TaskList, val prefs: TaskListPrefs)

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

            val (taskList, prefs) =
                runCatching { call.receive<Insert>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.insert(userId, taskList, prefs).bind()
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

            val listId =
                call.parameters
                    .getAsResult("id")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskListService.getById(listId).bind()
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
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val taskList =
                runCatching { call.receive<TaskList>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.update(listId, taskList).bind()
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
                    .andThen { listId -> taskListService.ensureUserOwnership(userId, listId) }
                    .bind()

            taskListService.delete(listId).bind()
        }
        .mapBoth(
            success = { call.respondText("Task list deleted", status = HttpStatusCode.Accepted) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.reorderListById(
    userService: UserService,
    taskListService: TaskListService,
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
                    .getAsResult("id")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskListService
                .reorder(userId, listId, reorder.fromIndex, reorder.toIndex, reorder.lastModified)
                .bind()
        }
        .mapBoth(
            success = { call.respondText("Task lists reordered", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.getListPrefsById(
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
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskListService.getPrefsByIds(userId, listId).bind()
        }
        .mapBoth(
            success = { prefs -> call.respondNullable(prefs) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.putListPrefsById(
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
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            val prefs =
                runCatching { call.receive<TaskListPrefs>() }
                    .mapError { WebError.InputInvalid }
                    .bind()

            taskListService.updateListPrefs(userId, listId, prefs).bind()
        }
        .mapBoth(
            success = { call.respondText("List preferences updated", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.getListMembers(
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
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskListService.getProfilesByList(listId).bind()
        }
        .mapBoth(
            success = { profiles -> call.respond(profiles) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.requestListInvite(
    userService: UserService,
    taskListService: TaskListService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .andThen { id -> userService.ensureVerified(id) }
                    .bind()

            val listId =
                call.parameters
                    .getAsResult("id")
                    .andThen { listId -> taskListService.ensureUserAccess(userId, listId) }
                    .bind()

            taskListService.requestListInviteToken(listId).bind()
        }
        .mapBoth(
            success = { token -> call.respondText(token, status = HttpStatusCode.Created) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.getJoinInfo(
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

            val inviteToken =
                runCatching { call.receive<String>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.getListByInvite(inviteToken).bind()
        }
        .mapBoth(
            success = { list -> call.respond(list) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.requestListJoin(
    userService: UserService,
    taskListService: TaskListService
) =
    binding {
            val userId =
                call
                    .principal<JWTPrincipal>()
                    .toResultOr { WebError.PrincipalInvalid }
                    .andThen { principal -> userService.getIdFor(principal) }
                    .andThen { id -> userService.ensureVerified(id) }
                    .bind()

            val inviteToken =
                runCatching { call.receive<String>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.acceptInviteToken(userId, inviteToken).bind()
        }
        .mapBoth(
            success = { id -> call.respondText("List $id joined", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.leaveList(
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
                    .andThen { listId -> taskListService.ensureUserNotOwner(userId, listId) }
                    .bind()

            taskListService.removeMemberFromList(userId, listId).bind()
        }
        .mapBoth(
            success = { call.respondText("List left", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )

suspend fun PipelineContext<Unit, ApplicationCall>.removeMemberFromList(
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
                    .andThen { listId -> taskListService.ensureUserOwnership(userId, listId) }
                    .bind()

            val memberId =
                runCatching { call.receive<String>() }.mapError { WebError.InputInvalid }.bind()

            taskListService.removeMemberFromList(memberId, listId).bind()
        }
        .mapBoth(
            success = { call.respondText("Member removed", status = HttpStatusCode.OK) },
            failure = { error -> handleError(error) }
        )
