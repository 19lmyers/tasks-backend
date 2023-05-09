package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import kotlinx.datetime.Clock
import java.util.*
import kotlin.time.Duration.Companion.hours

class UserRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun getByEmail(email: String) = runCatching {
        database.userQueries
            .getByEmail(email)
            .executeAsOneOrNull()
    }.mapError { DataError.DatabaseError(it) }

    fun getById(id: String) = runCatching {
        database.userQueries
            .getById(id)
            .executeAsOneOrNull()
    }.mapError { DataError.DatabaseError(it) }

    fun insert(email: String, displayName: String, hashedPassword: ByteArray) = runCatching {
        database.userQueries.insert(UUID.randomUUID().toString(), email, hashedPassword, displayName, null)
    }.mapError { DataError.DatabaseError(it) }

    fun update(id: String, displayName: String, profilePhotoUri: String?) = runCatching {
        database.userQueries.update(displayName, profilePhotoUri, id)
    }.mapError { DataError.DatabaseError(it) }

    fun updatePhoto(id: String, profilePhotoUri: String?) = runCatching {
        database.userQueries.updatePhoto(profilePhotoUri, id)
    }.mapError { DataError.DatabaseError(it) }

    fun updatePassword(userId: String, hashedPassword: ByteArray) = runCatching {
        database.userQueries.updatePassword(hashedPassword, userId)
    }.mapError { DataError.DatabaseError(it) }

    fun getAllPasswordResetTokens() = runCatching {
        database.passwordResetTokenQueries.getAll().executeAsList()
    }.mapError { DataError.DatabaseError(it) }

    fun getPasswordResetToken(resetToken: String) = runCatching {
        database.passwordResetTokenQueries.get(resetToken).executeAsOneOrNull()
    }.mapError { DataError.DatabaseError(it) }

    fun insertPasswordResetToken(userId: String, resetToken: String) = runCatching {
        database.passwordResetTokenQueries.insert(resetToken, userId, Clock.System.now().plus(1.hours))
        resetToken
    }.mapError { DataError.DatabaseError(it) }

    fun invalidatePasswordResetToken(resetToken: String) = runCatching {
        database.passwordResetTokenQueries.invalidate(resetToken)
    }.mapError { DataError.DatabaseError(it) }
}