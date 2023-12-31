package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onSuccess
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.domain.Action
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.JobScheduler
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.util.toErrorIfNull
import kotlinx.datetime.Instant

class TaskService(
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository,
    private val jobScheduler: JobScheduler
) {
    fun getTasksByList(listId: String) = taskRepository.getByList(listId)

    fun getTaskById(taskId: String) =
        taskRepository.getById(taskId).toErrorIfNull { DomainError.TaskNotFound }

    fun insert(userId: String, listId: String, task: Task) = binding {
        val list = taskListRepository.getById(listId).bind()

        taskRepository
            .insert(userId, listId, task)
            .onSuccess { taskId ->
                if (
                    list?.classifierType == TaskList.ClassifierType.SHOPPING &&
                        task.category == null
                ) {
                    jobScheduler.predictCategory(listId, taskId, task.label)
                }

                jobScheduler.notifyAction(listId, taskId, userId, Action.ADD_TASK)
            }
            .bind()
    }

    fun update(userId: String, listId: String, taskId: String, task: Task) = binding {
        val existingTask = taskRepository.getById(taskId).bind()

        taskRepository
            .update(taskId, task)
            .onSuccess {
                if (existingTask?.isCompleted != task.isCompleted && task.isCompleted) {
                    jobScheduler.notifyAction(listId, taskId, userId, Action.COMPLETE_TASK)
                } else if (
                    existingTask?.isStarred != task.isStarred && !task.isCompleted && task.isStarred
                ) {
                    jobScheduler.notifyAction(listId, taskId, userId, Action.STAR_TASK)
                } else if (!task.isCompleted) {
                    jobScheduler.notifyAction(listId, taskId, userId, Action.EDIT_TASK)
                }
            }
            .bind()
    }

    fun move(newListId: String, taskId: String, lastModified: Instant) =
        taskRepository.move(newListId, taskId, lastModified)

    fun reorder(
        listId: String,
        taskId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) = taskRepository.reorder(listId, taskId, fromIndex, toIndex, lastModified)

    fun delete(userId: String, listId: String, taskId: String) =
        taskRepository.delete(taskId).onSuccess {
            jobScheduler.notifyAction(listId, taskId, userId, Action.REMOVE_TASK)
        }

    fun clearCompletedTasksByList(userId: String, listId: String) =
        taskRepository.clearCompletedTasksByList(listId).onSuccess {
            jobScheduler.notifyAction(listId, "NIL", userId, Action.CLEAR_COMPLETED_TASKS)
        }
}
