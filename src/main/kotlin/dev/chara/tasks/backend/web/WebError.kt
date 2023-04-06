package dev.chara.tasks.backend.web

import dev.chara.tasks.backend.ApplicationError

sealed class WebError : ApplicationError {
    object InputInvalid : WebError()
    object PrincipalInvalid : WebError()

    class ParameterMissing(val name: String) : WebError()
}