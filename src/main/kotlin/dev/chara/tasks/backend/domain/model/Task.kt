package dev.chara.tasks.backend.domain.model

import dev.chara.tasks.backend.data.sql.Task as DbTask
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String?,
    val listId: String,
    val label: String,
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val details: String? = null,
    val reminderDate: Instant? = null,
    val dueDate: Instant? = null,
    val dateCreated: Instant? = null,
    val lastModified: Instant,
    val ordinal: Int = -1,
    val category: String? = null
)

fun DbTask.toModel() =
    Task(
        id,
        list_id,
        label,
        is_completed,
        is_starred,
        details,
        reminder_date,
        due_date,
        date_created,
        last_modified,
        ordinal,
        category
    )
