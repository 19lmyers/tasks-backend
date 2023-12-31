package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import dev.chara.tasks.backend.domain.model.Task
import dev.chara.tasks.backend.domain.model.toModel
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TaskRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun getById(taskId: String) =
        runCatching { database.taskQueries.getById(taskId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }
            .map { it?.toModel() }

    fun getByList(listId: String) =
        runCatching { database.taskQueries.getByList(listId).executeAsList() }
            .mapError { DataError.DatabaseError(it) }
            .map { tasks -> tasks.map { it.toModel() } }

    private fun getMaxOrdinal(listId: String) =
        runCatching { database.taskQueries.getMaxOrdinal(listId).executeAsOneOrNull()?.MAX }
            .mapError { DataError.DatabaseError(it) }

    fun insert(userId: String, listId: String, task: Task) = binding {
        val id = UUID.randomUUID().toString()

        val maxOrdinal = getMaxOrdinal(listId).bind()

        runCatching {
                database.taskQueries.insert(
                    id,
                    list_id = listId,
                    label = task.label,
                    is_completed = task.isCompleted,
                    is_starred = task.isStarred,
                    details = task.details,
                    reminder_date = task.reminderDate,
                    due_date = task.dueDate,
                    last_modified = task.lastModified,
                    reminder_fired = null,
                    date_created = Clock.System.now(),
                    ordinal = maxOrdinal ?: 0,
                    category = task.category
                )
            }
            .mapError { DataError.DatabaseError(it) }
            .bind()

        id
    }

    fun update(taskId: String, task: Task) =
        runCatching {
                database.taskQueries.update(
                    label = task.label,
                    is_completed = task.isCompleted,
                    is_starred = task.isStarred,
                    details = task.details,
                    reminder_date = task.reminderDate,
                    due_date = task.dueDate,
                    last_modified = task.lastModified,
                    id = taskId,
                    category = task.category
                )
            }
            .mapError { DataError.DatabaseError(it) }

    fun move(newListId: String, taskId: String, lastModified: Instant) =
        runCatching { database.taskQueries.move(newListId, lastModified, taskId) }
            .mapError { DataError.DatabaseError(it) }

    fun reorder(
        listId: String,
        taskId: String,
        fromIndex: Int,
        toIndex: Int,
        lastModified: Instant
    ) =
        runCatching {
                database.taskQueries.reorder(
                    list_id = listId,
                    task_id = taskId,
                    ordinal = toIndex.toLong(),
                    difference_sign = (fromIndex - toIndex).sign,
                    lower_bound = min(fromIndex, toIndex),
                    upper_bound = max(fromIndex, toIndex),
                    last_modified = lastModified
                )
            }
            .mapError { DataError.DatabaseError(it) }

    fun delete(taskId: String) =
        runCatching { database.taskQueries.delete(taskId) }.mapError { DataError.DatabaseError(it) }

    fun clearCompletedTasksByList(listId: String) =
        runCatching { database.taskQueries.clearCompletedByList(listId) }
            .mapError { DataError.DatabaseError(it) }
}
