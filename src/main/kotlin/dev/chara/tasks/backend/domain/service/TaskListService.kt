package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.GeminiJobScheduler
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.util.toErrorIfNull
import kotlinx.datetime.Instant

class TaskListService(
    private val repository: TaskListRepository,
    private val geminiJobScheduler: GeminiJobScheduler
) {

    fun getIdFor(userId: String, listId: String) =
        repository
            .getByIds(userId, listId)
            .toErrorIfNull { DomainError.ListNotFound }
            .map { it.id!! }

    fun getForUser(userId: String) = repository.getByUser(userId)

    fun getByIds(userId: String, listId: String) =
        repository.getByIds(userId, listId).toErrorIfNull { DomainError.ListNotFound }

    fun insert(userId: String, taskList: TaskList) =
        if (taskList.title.isBlank()) {
            Err(DomainError.ListTitleRequired)
        } else {
            repository.insert(userId, taskList)
        }

    fun update(
        userId: String,
        listId: String,
        taskList: TaskList
    ): Result<Unit, DataError.DatabaseError> = repository.update(userId, listId, taskList)

    fun reorder(
        userId: String,
        listId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) = repository.reorder(userId, listId, fromIndex, toIndex, lastModified)

    fun delete(userId: String, listId: String) = repository.delete(userId, listId)
}
