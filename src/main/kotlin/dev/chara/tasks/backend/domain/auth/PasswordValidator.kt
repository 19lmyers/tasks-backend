package dev.chara.tasks.backend.domain.auth

import com.chrynan.validator.*
import me.gosimple.nbvcxz.Nbvcxz

class PasswordValidator : Validator<String, String> {

    override fun validate(input: String): ValidationResult<String> {
        if (input.isBlank()) return Invalid(PasswordValidationError.InputIsBlank)

        if (input.count() > 50) return Invalid(PasswordValidationError.InputTooLong)

        val result = nbvcxz.estimate(input)

        if (!result.isMinimumEntropyMet)
            return Invalid(PasswordValidationError.MinimumEntropyNotMet(result.feedback.warning))

        return Valid(input)
    }

    companion object {
        private val nbvcxz = Nbvcxz()
    }

    sealed class PasswordValidationError(override val details: String? = null) : ValidationError {

        object InputIsBlank : PasswordValidationError(details = "Password must not be blank")
        object InputTooLong : PasswordValidationError(details = "Password must be less than 50 characters")

        class MinimumEntropyNotMet(details: String?) : PasswordValidationError(details = details)
    }
}