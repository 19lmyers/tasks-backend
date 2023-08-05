package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.map
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.util.toErrorIfNull
import kotlinx.datetime.Instant

class TaskService(private val repository: TaskRepository) {

    fun getIdFor(userId: String, listId: String, taskId: String) =
        repository
            .getByIds(userId, listId, taskId)
            .toErrorIfNull { DomainError.TaskNotFound }
            .map { it.id!! }

    fun getTasksByList(userId: String, listId: String) = repository.getByList(userId, listId)

    fun getTaskByIds(userId: String, listId: String, taskId: String) =
        repository.getByIds(userId, listId, taskId).toErrorIfNull { DomainError.TaskNotFound }

    fun insert(userId: String, listId: String, task: Task) = repository.insert(userId, listId, task)

    fun update(userId: String, listId: String, taskId: String, task: Task) =
        repository.update(userId, listId, taskId, task)

    fun move(userId: String, newListId: String, taskId: String, lastModified: Instant) =
        repository.move(userId, newListId, taskId, lastModified)

    fun reorder(
        userId: String,
        listId: String,
        taskId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) = repository.reorder(userId, listId, taskId, fromIndex, toIndex, lastModified)

    fun delete(userId: String, listId: String, taskId: String) =
        repository.delete(userId, listId, taskId)

    fun clearCompletedTasksByList(userId: String, listId: String) =
        repository.clearCompletedTasksByList(userId, listId)
}
