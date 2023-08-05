package dev.chara.tasks.backend.test.domain.auth

import dev.chara.tasks.backend.domain.auth.SecretHasher
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SecretHasherTest {
    @Test
    fun `verifies input matches`() {
        val secretHasher = SecretHasher()

        val hash = secretHasher.hash("my-secure-password")

        assertTrue(secretHasher.verify("my-secure-password", hash))
    }

    @Test
    fun `rejects unequal input`() {
        val secretHasher = SecretHasher()

        val hash = secretHasher.hash("my-secure-password")

        assertFalse(secretHasher.verify("my-secure-passw0rd", hash))
    }
}
