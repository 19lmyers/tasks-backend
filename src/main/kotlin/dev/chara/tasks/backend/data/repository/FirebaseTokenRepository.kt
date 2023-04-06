package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory
import kotlinx.datetime.Clock

class FirebaseTokenRepository(databaseFactory: DatabaseFactory) {
    private val database = databaseFactory.getDatabase()

    fun getForUser(userId: String) = runCatching {
        database.firebaseTokenQueries
            .getForUser(userId)
            .executeAsList()
    }.mapError { DataError.DatabaseError(it) }
        .map { tasks -> tasks.map { it.fcm_token } }

    fun link(userId: String, fcmToken: String) = runCatching {
        database.firebaseTokenQueries.update(fcmToken, userId, Clock.System.now())
    }.mapError { DataError.DatabaseError(it) }

    fun invalidate(fcmToken: String) = runCatching {
        database.firebaseTokenQueries.invalidate(fcmToken)
    }.mapError { DataError.DatabaseError(it) }
}