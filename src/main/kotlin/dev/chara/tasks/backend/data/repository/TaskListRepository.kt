package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.model.toModel
import kotlinx.datetime.Clock
import java.util.*

class TaskListRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun getByIds(userId: String, listId: String) = runCatching {
        database.taskListQueries
            .getByIds(listId, userId)
            .executeAsOneOrNull()
    }.mapError { DataError.DatabaseError(it) }
        .map { it?.toModel() }

    fun getByUser(userId: String) = runCatching {
        database.taskListQueries
            .getByUser(userId)
            .executeAsList()
    }.mapError { DataError.DatabaseError(it) }
        .map { lists -> lists.map { it.toModel() } }

    fun insert(userId: String, taskList: TaskList) = binding {
        val id = UUID.randomUUID().toString()

        runCatching {
            database.taskListQueries.insert(
                id,
                userId,
                taskList.title,
                color = taskList.color,
                icon = taskList.icon,
                description = taskList.description,
                is_pinned = taskList.isPinned,
                sort_type = TaskList.SortType.ORDINAL,
                sort_direction = TaskList.SortDirection.ASCENDING,
                show_index_numbers = taskList.showIndexNumbers,
                date_created = Clock.System.now(),
                last_modified = taskList.lastModified
            )
        }.mapError { DataError.DatabaseError(it) }.bind()

        id
    }

    fun update(userId: String, listId: String, taskList: TaskList) = runCatching {
        database.taskListQueries.update(
            taskList.title,
            taskList.color,
            taskList.icon,
            taskList.description,
            taskList.isPinned,
            taskList.sortType,
            taskList.sortDirection,
            taskList.showIndexNumbers,
            taskList.lastModified,
            listId,
            userId
        )
    }.mapError { DataError.DatabaseError(it) }

    fun delete(userId: String, listId: String) = runCatching {
        database.taskQueries.deleteByList(userId, listId)
        database.taskListQueries.delete(listId, userId)
    }.mapError { DataError.DatabaseError(it) }
}