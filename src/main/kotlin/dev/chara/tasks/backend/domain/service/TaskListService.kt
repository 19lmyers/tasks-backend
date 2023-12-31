package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.*
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.data.repository.UserRepository
import dev.chara.tasks.backend.domain.Action
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.JobScheduler
import dev.chara.tasks.backend.domain.auth.RandomTokenGenerator
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.model.TaskListPrefs
import dev.chara.tasks.backend.domain.model.toModel
import dev.chara.tasks.backend.util.toErrorIfNotNull
import dev.chara.tasks.backend.util.toErrorIfNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TaskListService(
    private val userRepository: UserRepository,
    private val taskListRepository: TaskListRepository,
    private val jobScheduler: JobScheduler
) {
    private val randomTokenGenerator = RandomTokenGenerator()

    fun ensureUserAccess(userId: String, listId: String) =
        taskListRepository.ensureAccess(userId, listId).toErrorIfNull {
            DomainError.ListAccessDenied
        }

    fun ensureUserOwnership(userId: String, listId: String) =
        taskListRepository.ensureOwnership(userId, listId).toErrorIfNull {
            DomainError.ListAccessDenied
        }

    fun ensureUserNotOwner(userId: String, listId: String) =
        taskListRepository
            .ensureAccess(userId, listId)
            .toErrorIfNull { DomainError.ListAccessDenied }
            .flatMap {
                taskListRepository.ensureOwnership(userId, listId).toErrorIfNotNull {
                    DomainError.UserIsListOwner
                }
            }
            .map { listId }

    fun getForUser(userId: String) = taskListRepository.getByUser(userId)

    fun getById(listId: String) =
        taskListRepository.getById(listId).toErrorIfNull { DomainError.ListNotFound }

    fun getPrefsByIds(userId: String, listId: String) =
        taskListRepository.getPrefsByIds(userId, listId)

    fun insert(userId: String, taskList: TaskList, prefs: TaskListPrefs) =
        if (taskList.title.isBlank()) {
            Err(DomainError.ListTitleRequired)
        } else {
            taskListRepository.insert(userId, taskList, prefs)
        }

    fun update(listId: String, taskList: TaskList): Result<Unit, DataError.DatabaseError> =
        taskListRepository.update(listId, taskList)

    fun updateListPrefs(
        userId: String,
        listId: String,
        prefs: TaskListPrefs
    ): Result<Unit, DataError.DatabaseError> =
        taskListRepository.updateListPrefs(userId, listId, prefs)

    fun reorder(
        userId: String,
        listId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) = taskListRepository.reorder(userId, listId, fromIndex, toIndex, lastModified)

    fun delete(listId: String) = taskListRepository.delete(listId)

    fun getProfilesByList(listId: String) =
        taskListRepository.getProfilesByList(listId).map { it.toModel() }

    fun requestListInviteToken(listId: String) = binding {
        userRepository.getAllListInviteTokens().bind()

        userRepository
            .insertListInviteToken(
                listId,
                randomTokenGenerator.generateSecureToken(),
            )
            .bind()
    }

    fun getListByInvite(inviteToken: String) = binding {
        val token =
            userRepository
                .getListInviteToken(inviteToken)
                .toErrorIfNull { DomainError.InviteTokenNotFound }
                .toErrorIf(
                    predicate = { token -> token.expiry_time <= Clock.System.now() },
                    transform = { DomainError.InviteTokenExpired }
                )
                .bind()

        val list =
            taskListRepository
                .getById(token.list_id)
                .toErrorIfNull { DomainError.ListNotFound }
                .bind()

        Ok(list).bind()
    }

    fun acceptInviteToken(userId: String, inviteToken: String) = binding {
        val token =
            userRepository
                .getListInviteToken(inviteToken)
                .toErrorIfNull { DomainError.InviteTokenNotFound }
                .toErrorIf(
                    predicate = { token -> token.expiry_time <= Clock.System.now() },
                    transform = { DomainError.InviteTokenExpired }
                )
                .bind()

        val listId =
            taskListRepository
                .getById(token.list_id)
                .toErrorIfNull { DomainError.ListNotFound }
                .map { it.id!! }
                .bind()

        taskListRepository
            .addMember(userId, listId)
            .onSuccess {
                jobScheduler.notifyAction(listId, "NIL", userId, Action.CLEAR_COMPLETED_TASKS)
            }
            .bind()

        Ok(listId).bind()
    }

    fun removeMemberFromList(userId: String, listId: String) =
        taskListRepository.removeMember(userId, listId)
}
