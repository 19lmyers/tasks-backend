package dev.chara.tasks.backend.domain.model

import kotlinx.serialization.Serializable
import dev.chara.tasks.backend.data.sql.User as DbUser

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val displayName: String,
    val profilePhotoUri: String?
)

fun DbUser.toProfile() = Profile(id, email, display_name, profile_photo_uri)