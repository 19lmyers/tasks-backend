package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onSuccess
import dev.chara.tasks.backend.data.GeminiJobScheduler
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.util.toErrorIfNull
import kotlinx.datetime.Instant

class TaskService(
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository,
    private val geminiJobScheduler: GeminiJobScheduler
) {

    fun getIdFor(userId: String, listId: String, taskId: String) =
        taskRepository
            .getByIds(userId, listId, taskId)
            .toErrorIfNull { DomainError.TaskNotFound }
            .map { it.id!! }

    fun getTasksByList(userId: String, listId: String) = taskRepository.getByList(userId, listId)

    fun getTaskByIds(userId: String, listId: String, taskId: String) =
        taskRepository.getByIds(userId, listId, taskId).toErrorIfNull { DomainError.TaskNotFound }

    fun insert(userId: String, listId: String, task: Task) = binding {
        val list = taskListRepository.getByIds(userId, listId).bind()

        taskRepository
            .insert(userId, listId, task)
            .onSuccess { taskId ->
                if (
                    list?.classifierType == TaskList.ClassifierType.SHOPPING &&
                        task.category == null
                ) {
                    geminiJobScheduler.predict(userId, listId, taskId, task.label)
                }
            }
            .bind()
    }

    fun update(userId: String, listId: String, taskId: String, task: Task) =
        taskRepository.update(userId, listId, taskId, task)

    fun move(userId: String, newListId: String, taskId: String, lastModified: Instant) =
        taskRepository.move(userId, newListId, taskId, lastModified)

    fun reorder(
        userId: String,
        listId: String,
        taskId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) = taskRepository.reorder(userId, listId, taskId, fromIndex, toIndex, lastModified)

    fun delete(userId: String, listId: String, taskId: String) =
        taskRepository.delete(userId, listId, taskId)

    fun clearCompletedTasksByList(userId: String, listId: String) =
        taskRepository.clearCompletedTasksByList(userId, listId)
}
