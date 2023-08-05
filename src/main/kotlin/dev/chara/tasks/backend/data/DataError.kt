package dev.chara.tasks.backend.data

import dev.chara.tasks.backend.ApplicationError

sealed class DataError(open val throwable: Throwable) : ApplicationError {
    data class DatabaseError(override val throwable: Throwable) : DataError(throwable)

    data class IOError(override val throwable: Throwable) : DataError(throwable)

    data class SMTPError(override val throwable: Throwable) : DataError(throwable)
}
