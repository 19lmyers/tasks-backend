package dev.chara.tasks.backend.web

import dev.chara.tasks.backend.ApplicationError

sealed class WebError : ApplicationError {
    data object InputInvalid : WebError()
    data object PrincipalInvalid : WebError()

    data class ParameterMissing(val name: String) : WebError()
}