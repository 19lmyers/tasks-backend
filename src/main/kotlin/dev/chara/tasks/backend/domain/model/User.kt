package dev.chara.tasks.backend.domain.model

import dev.chara.tasks.backend.data.sql.User as DbUser
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val profilePhotoUri: String?
)

@JvmName("DbUserToModel") fun Collection<DbUser>.toModel() = map { it.toModel() }

fun DbUser.toModel() = User(this.id, this.email, this.display_name, this.profile_photo_uri)
