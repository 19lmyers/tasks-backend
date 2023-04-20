package dev.chara.tasks.backend.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import dev.chara.tasks.backend.data.sql.TaskList as DbTaskList

@Serializable
data class TaskList(
    val id: String?,

    val title: String,

    val color: Color? = null,
    val icon: Icon? = null,
    val description: String? = null,

    val isPinned: Boolean = false,
    val showIndexNumbers: Boolean = false,

    val sortType: SortType = SortType.DATE_CREATED,
    val sortDirection: SortDirection = SortDirection.ASCENDING,

    val dateCreated: Instant? = null,
    val lastModified: Instant
) {
    @Serializable
    enum class Color {
        RED,
        ORANGE,
        YELLOW,
        GREEN,
        BLUE,
        PURPLE,
        PINK,
    }

    @Serializable
    enum class Icon {
        BACKPACK,
        BOOK,
        BOOKMARK,
        BRUSH,
        CAKE,
        CALL,
        CAR,
        CELEBRATION,
        CLIPBOARD,
        FLIGHT,
        FOOD_BEVERAGE,
        FOOTBALL,
        FOREST,
        GROUP,
        HANDYMAN,
        HOME_REPAIR_SERVICE,
        LIGHT_BULB,
        MEDICAL_SERVICES,
        MUSIC_NOTE,
        PERSON,
        PETS,
        PIANO,
        RESTAURANT,
        SCISSORS,
        SHOPPING_CART,
        SMILE,
        WORK
    }

    @Serializable
    enum class SortType {
        ORDINAL,
        LABEL,
        DATE_CREATED,
        UPCOMING,
        STARRED;
    }

    @Serializable
    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }
}

fun DbTaskList.toModel() = TaskList(
    id,
    title,
    color,
    icon,
    description,
    is_pinned,
    show_index_numbers,
    sort_type,
    sort_direction,
    date_created,
    last_modified
)