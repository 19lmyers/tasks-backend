package dev.chara.tasks.backend.test.domain.auth

import dev.chara.tasks.backend.domain.auth.RandomTokenGenerator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RandomTokenGeneratorTest() {
    @Test
    fun `tokens are unique`() {
        val randomTokenGenerator = RandomTokenGenerator()
        assertNotEquals(randomTokenGenerator.generateSecureToken(), randomTokenGenerator.generateSecureToken())
    }

    @Test
    fun `tokens are proper length`() {
        val randomTokenGenerator = RandomTokenGenerator()

        val token = randomTokenGenerator.generateSecureToken()
        assertEquals(token.length, 36)
    }
}