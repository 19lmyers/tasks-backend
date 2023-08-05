package dev.chara.tasks.backend.domain.auth

import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class RandomTokenGenerator {
    private val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val secureRandom = SecureRandom().asKotlinRandom()

    fun generateSecureToken(): String =
        (1..TOKEN_LENGTH).map { charset.random(secureRandom) }.joinToString("")

    companion object {
        private const val TOKEN_LENGTH = 36
    }
}
