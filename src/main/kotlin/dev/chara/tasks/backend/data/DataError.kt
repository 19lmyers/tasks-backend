package dev.chara.tasks.backend.data

import dev.chara.tasks.backend.ApplicationError

sealed class DataError(val throwable: Throwable) : ApplicationError {
    class DatabaseError(throwable: Throwable) : DataError(throwable)
    class IOError(throwable: Throwable) : DataError(throwable)
    class SMTPError(throwable: Throwable) : DataError(throwable)
}