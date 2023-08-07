package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.model.toModel
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TaskListRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun getByIds(userId: String, listId: String) =
        runCatching { database.taskListQueries.getByIds(listId, userId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }
            .map { it?.toModel() }

    fun getByUser(userId: String) =
        runCatching { database.taskListQueries.getByUser(userId).executeAsList() }
            .mapError { DataError.DatabaseError(it) }
            .map { lists -> lists.map { it.toModel() } }

    private fun getMaxOrdinal(userId: String) =
        runCatching { database.taskListQueries.getMaxOrdinal(userId).executeAsOneOrNull()?.MAX }
            .mapError { DataError.DatabaseError(it) }

    fun insert(userId: String, taskList: TaskList) = binding {
        val id = UUID.randomUUID().toString()

        val maxOrdinal = getMaxOrdinal(userId).bind()

        runCatching {
                database.taskListQueries.insert(
                    id,
                    userId,
                    taskList.title,
                    color = taskList.color,
                    icon = taskList.icon,
                    description = taskList.description,
                    sort_type = TaskList.SortType.ORDINAL,
                    sort_direction = TaskList.SortDirection.ASCENDING,
                    show_index_numbers = taskList.showIndexNumbers,
                    date_created = Clock.System.now(),
                    last_modified = taskList.lastModified,
                    ordinal = maxOrdinal ?: 0
                )
            }
            .mapError { DataError.DatabaseError(it) }
            .bind()

        id
    }

    fun update(userId: String, listId: String, taskList: TaskList) =
        runCatching {
                database.taskListQueries.update(
                    title = taskList.title,
                    color = taskList.color,
                    icon = taskList.icon,
                    description = taskList.description,
                    sort_type = taskList.sortType,
                    sort_direction = taskList.sortDirection,
                    show_index_numbers = taskList.showIndexNumbers,
                    last_modified = taskList.lastModified,
                    id = listId,
                    user_id = userId
                )
            }
            .mapError { DataError.DatabaseError(it) }

    fun reorder(
        userId: String,
        listId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) =
        runCatching {
                database.taskListQueries.reorder(
                    user_id = userId,
                    list_id = listId,
                    ordinal = toIndex.toLong(),
                    difference_sign = (fromIndex - toIndex).sign,
                    lower_bound = min(fromIndex, toIndex),
                    upper_bound = max(fromIndex, toIndex),
                    last_modified = lastModified
                )
            }
            .mapError { DataError.DatabaseError(it) }

    fun delete(userId: String, listId: String) =
        runCatching {
                database.taskQueries.deleteByList(userId, listId)
                database.taskListQueries.delete(listId, userId)
            }
            .mapError { DataError.DatabaseError(it) }
}
