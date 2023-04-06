package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.map
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.util.toErrorIfNull

class TaskListService(private val repository: TaskListRepository) {

    fun getIdFor(userId: String, listId: String) = repository.getByIds(userId, listId)
        .toErrorIfNull {
            DomainError.ListNotFound
        }.map { it.id!! }

    fun getForUser(userId: String) = repository.getByUser(userId)

    fun getByIds(userId: String, listId: String) = repository.getByIds(userId, listId).toErrorIfNull {
        DomainError.ListNotFound
    }

    fun insert(userId: String, taskList: TaskList) =
        if (taskList.title.isBlank()) {
            Err(DomainError.ListTitleRequired)
        } else {
            repository.insert(userId, taskList)
        }

    fun update(userId: String, listId: String, taskList: TaskList) =
        repository.update(userId, listId, taskList)

    fun delete(userId: String, listId: String) = repository.delete(userId, listId)
}