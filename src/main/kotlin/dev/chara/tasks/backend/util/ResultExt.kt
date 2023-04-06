package dev.chara.tasks.backend.util

import com.chrynan.validator.ValidationResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.chara.tasks.backend.web.WebError
import io.ktor.http.*

inline fun <V, E> Result<V?, E>.toErrorIfNull(transform: () -> E): Result<V, E> {
    return when (this) {
        is Ok -> if (value == null) {
            Err(transform())
        } else {
            Ok(value!!)
        }
        is Err -> this
    }
}

inline fun <V, E> Result<V?, E>.toErrorIfNotNull(transform: (V) -> E): Result<Unit, E> {
    return when (this) {
        is Ok -> if (value != null) {
            Err(transform(value!!))
        } else {
            Ok(Unit)
        }
        is Err -> this
    }
}

fun <T> ValidationResult<T>.toResult() =
    if (this is ValidationResult.Valid) {
        Ok(this.value)
    } else {
        val invalid = this as ValidationResult.Invalid
        Err(invalid.errors.first())
    }

fun Parameters.getAsResult(name: String) = if (contains(name)) {
    Ok(get(name)!!)
} else {
    Err(WebError.ParameterMissing(name))
}