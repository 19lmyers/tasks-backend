package dev.chara.tasks.backend.domain.model

import dev.chara.tasks.backend.data.sql.TaskListPrefs as DbTaskListPrefs
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TaskListPrefs(
    val listId: String,
    val showIndexNumbers: Boolean = false,
    val sortType: SortType = SortType.ORDINAL,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val ordinal: Int = -1,
    val lastModified: Instant
) {
    @Serializable
    enum class SortType {
        ORDINAL,
        LABEL,
        CATEGORY,
        DATE_CREATED,
        UPCOMING,
        STARRED
    }

    @Serializable
    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }
}

fun DbTaskListPrefs.toModel() =
    TaskListPrefs(list_id, show_index_numbers, sort_type, sort_direction, ordinal, last_modified)
