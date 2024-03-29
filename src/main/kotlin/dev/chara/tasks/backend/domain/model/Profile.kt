package dev.chara.tasks.backend.domain.model

import dev.chara.tasks.backend.data.sql.User as DbUser
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String,
    val profilePhotoUri: String?
)

fun DbUser.toProfile() = Profile(id, email, email_verified, display_name, profile_photo_uri)
