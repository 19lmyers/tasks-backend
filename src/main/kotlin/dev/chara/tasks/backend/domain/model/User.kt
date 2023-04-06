package dev.chara.tasks.backend.domain.model

import kotlinx.serialization.Serializable
import dev.chara.tasks.backend.data.sql.User as DbUser

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String
)

fun DbUser.toModel() = User(this.id, this.email, this.display_name)