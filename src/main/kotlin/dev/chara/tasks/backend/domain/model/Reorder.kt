package dev.chara.tasks.backend.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable data class Reorder(val fromIndex: Int, val toIndex: Int, val lastModified: Instant)
