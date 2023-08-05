package dev.chara.tasks.backend.data.repository

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.DatabaseFactory

class ReminderRepository(databaseFactory: DatabaseFactory) {

    private val database = databaseFactory.getDatabase()

    fun get() =
        runCatching { database.reminderQueries.get().executeAsList() }
            .mapError { DataError.DatabaseError(it) }

    fun setFired(userId: String, taskId: String) =
        runCatching { database.reminderQueries.setFired(userId, taskId) }
            .mapError { DataError.DatabaseError(it) }
}
