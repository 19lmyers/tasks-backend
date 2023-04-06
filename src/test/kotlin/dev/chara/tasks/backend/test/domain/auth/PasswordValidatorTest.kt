package dev.chara.tasks.backend.test.domain.auth

import com.chrynan.validator.assertContains
import com.chrynan.validator.isInvalid
import com.chrynan.validator.isValid
import dev.chara.tasks.backend.domain.auth.PasswordValidator
import org.junit.Test
import kotlin.test.assertTrue

class PasswordValidatorTest {

    @Test
    fun `rejects blank`() {
        val validator = PasswordValidator()

        val result = validator.validate("")
        assertTrue(result.isInvalid)
        result.assertContains(PasswordValidator.PasswordValidationError.InputIsBlank)
    }

    @Test
    fun `rejects too long`() {
        val validator = PasswordValidator()

        val result = validator.validate("GdNNJ9GTVN6ic872-2KiBkTKK7X9Y9n3gFRw8MMT2yX_gy_Mjo*")
        assertTrue(result.isInvalid)
        result.assertContains(PasswordValidator.PasswordValidationError.InputTooLong)
    }

    @Test
    fun `rejects weak`() {
        val validator = PasswordValidator()

        val result = validator.validate("my-weak-password")
        assertTrue(result.isInvalid)
    }

    @Test
    fun `accepts reasonably strong password`() {
        val validator = PasswordValidator()

        val result = validator.validate("soldier-freeze-jamaican-incident")
        assertTrue(result.isValid)
    }

}