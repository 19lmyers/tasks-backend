package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class UserRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun ensureVerified(userId: String) =
        runCatching { database.userQueries.ensureVerified(userId).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun getAll() =
        runCatching { database.userQueries.getAll().executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun getByEmail(email: String) =
        runCatching { database.userQueries.getByEmail(email).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun getById(id: String) =
        runCatching { database.userQueries.getById(id).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun insert(email: String, displayName: String, hashedPassword: ByteArray) =
        runCatching {
                val id = UUID.randomUUID().toString()
                database.userQueries.insert(id, email, hashedPassword, displayName)
                id
            }
            .mapError { DataError.DatabaseError(it) }

    fun update(id: String, displayName: String, profilePhotoUri: String?) =
        runCatching { database.userQueries.update(displayName, profilePhotoUri, id) }
            .mapError { DataError.DatabaseError(it) }

    fun updatePhoto(id: String, profilePhotoUri: String?) =
        runCatching { database.userQueries.updatePhoto(profilePhotoUri, id) }
            .mapError { DataError.DatabaseError(it) }

    fun updateEmail(id: String, email: String) =
        runCatching { database.userQueries.updateEmail(email, id) }
            .mapError { DataError.DatabaseError(it) }

    fun setEmailVerified(id: String, emailVerified: Boolean) =
        runCatching { database.userQueries.setEmailVerified(emailVerified, id) }
            .mapError { DataError.DatabaseError(it) }

    fun updatePassword(userId: String, hashedPassword: ByteArray) =
        runCatching { database.userQueries.updatePassword(hashedPassword, userId) }
            .mapError { DataError.DatabaseError(it) }

    fun getAllEmailVerificationTokens() =
        runCatching { database.emailVerificationTokenQueries.getAll().executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun getEmailVerificationToken(verifyToken: String) =
        runCatching { database.emailVerificationTokenQueries.get(verifyToken).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun insertEmailVerificationToken(userId: String, verifyToken: String, newEmail: String) =
        runCatching {
                database.emailVerificationTokenQueries.insert(
                    verifyToken,
                    userId,
                    newEmail,
                    Clock.System.now().plus(1.hours)
                )
                verifyToken
            }
            .mapError { DataError.DatabaseError(it) }

    fun invalidateEmailVerificationToken(verifyToken: String) =
        runCatching { database.emailVerificationTokenQueries.invalidate(verifyToken) }
            .mapError { DataError.DatabaseError(it) }

    fun getAllPasswordResetTokens() =
        runCatching { database.passwordResetTokenQueries.getAll().executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun getPasswordResetToken(resetToken: String) =
        runCatching { database.passwordResetTokenQueries.get(resetToken).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun insertPasswordResetToken(userId: String, resetToken: String) =
        runCatching {
                database.passwordResetTokenQueries.insert(
                    resetToken,
                    userId,
                    Clock.System.now().plus(20.minutes)
                )
                resetToken
            }
            .mapError { DataError.DatabaseError(it) }

    fun invalidatePasswordResetToken(resetToken: String) =
        runCatching { database.passwordResetTokenQueries.invalidate(resetToken) }
            .mapError { DataError.DatabaseError(it) }

    fun getAllListInviteTokens() =
        runCatching { database.listInviteTokenQueries.getAll().executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun getListInviteToken(inviteToken: String) =
        runCatching { database.listInviteTokenQueries.get(inviteToken).executeAsOneOrNull() }
            .mapError { DataError.DatabaseError(it) }

    fun insertListInviteToken(userId: String, inviteToken: String) =
        runCatching {
                database.listInviteTokenQueries.insert(
                    inviteToken,
                    userId,
                    Clock.System.now().plus(4.hours)
                )
                inviteToken
            }
            .mapError { DataError.DatabaseError(it) }

    fun invalidateListInviteToken(inviteToken: String) =
        runCatching { database.listInviteTokenQueries.invalidate(inviteToken) }
            .mapError { DataError.DatabaseError(it) }
}
