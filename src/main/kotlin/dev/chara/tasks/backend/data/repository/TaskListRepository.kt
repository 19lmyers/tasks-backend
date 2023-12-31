package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import dev.chara.tasks.backend.domain.model.TaskList
import dev.chara.tasks.backend.domain.model.TaskListPrefs
import dev.chara.tasks.backend.domain.model.toModel
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TaskListRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun ensureAccess(userId: String, listId: String) =
        runCatching { database.taskListQueries.ensureAccess(listId, userId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun ensureOwnership(userId: String, listId: String) =
        runCatching {
                database.taskListQueries.ensureOwnership(listId, userId).executeAsOneOrNull()
            }
            .mapError { DataError.DatabaseError(it) }

    fun getById(listId: String) =
        runCatching { database.taskListQueries.getById(listId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }
            .map { it?.toModel() }

    fun getPrefsByIds(userId: String, listId: String) =
        runCatching { database.taskListQueries.getPrefsByIds(listId, userId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }
            .map { it?.toModel() }

    fun getByUser(userId: String) =
        runCatching { database.taskListQueries.getByUser(userId).executeAsList() }
            .mapError { DataError.DatabaseError(it) }
            .map { lists -> lists.map { it.toModel() } }

    private fun getMaxOrdinal(userId: String) =
        runCatching { database.taskListQueries.getMaxOrdinal(userId).executeAsOneOrNull()?.MAX }
            .mapError { DataError.DatabaseError(it) }

    fun insert(userId: String, taskList: TaskList, prefs: TaskListPrefs) = binding {
        val id = UUID.randomUUID().toString()

        val maxOrdinal = getMaxOrdinal(userId).bind()

        runCatching {
                database.taskListQueries.insert(
                    id = id,
                    owner_id = userId,
                    title = taskList.title,
                    color = taskList.color,
                    icon = taskList.icon,
                    description = taskList.description,
                    date_created = Clock.System.now(),
                    last_modified = taskList.lastModified,
                    classifier_type = taskList.classifierType
                )

                database.taskListQueries.insertPrefs(
                    list_id = id,
                    user_id = userId,
                    sort_type = TaskListPrefs.SortType.ORDINAL,
                    sort_direction = TaskListPrefs.SortDirection.ASCENDING,
                    show_index_numbers = prefs.showIndexNumbers,
                    ordinal = maxOrdinal ?: 0,
                    last_modified = taskList.lastModified
                )
            }
            .mapError { DataError.DatabaseError(it) }
            .bind()

        id
    }

    fun update(listId: String, taskList: TaskList) =
        runCatching {
                database.taskListQueries.update(
                    title = taskList.title,
                    color = taskList.color,
                    icon = taskList.icon,
                    description = taskList.description,
                    last_modified = taskList.lastModified,
                    classifier_type = taskList.classifierType,
                    list_id = listId,
                )
            }
            .mapError { DataError.DatabaseError(it) }

    fun updateListPrefs(userId: String, listId: String, prefs: TaskListPrefs) =
        runCatching {
                database.taskListQueries.updatePrefs(
                    sort_type = prefs.sortType,
                    sort_direction = prefs.sortDirection,
                    show_index_numbers = prefs.showIndexNumbers,
                    last_modified = prefs.lastModified,
                    list_id = listId,
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

    fun getProfilesByList(listId: String) =
        runCatching { database.taskListQueries.getMembers(listId).executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun addMember(userId: String, listId: String) = binding {
        val maxOrdinal = getMaxOrdinal(userId).bind()

        runCatching {
                database.taskListQueries.addMember(listId, userId)
                database.taskListQueries.insertPrefs(
                    list_id = listId,
                    user_id = userId,
                    sort_type = TaskListPrefs.SortType.ORDINAL,
                    sort_direction = TaskListPrefs.SortDirection.ASCENDING,
                    show_index_numbers = false,
                    ordinal = maxOrdinal ?: 0,
                    last_modified = Clock.System.now()
                )
            }
            .mapError { DataError.DatabaseError(it) }
            .bind()
    }

    fun removeMember(userId: String, listId: String) =
        runCatching { database.taskListQueries.removeMember(listId, userId) }
            .mapError { DataError.DatabaseError(it) }

    fun delete(listId: String) =
        runCatching {
                database.taskQueries.deleteByList(listId)
                database.taskListQueries.delete(listId)
            }
            .mapError { DataError.DatabaseError(it) }
}
