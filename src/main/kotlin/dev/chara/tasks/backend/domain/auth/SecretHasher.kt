package dev.chara.tasks.backend.domain.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.util.*

class SecretHasher {
    private val hasher = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()

    fun hash(secret: String): ByteArray = hasher.hash(HASH_COST, secret.toCharArray())

    fun verify(challenge: String, hashed: ByteArray): Boolean =
        verifier.verify(challenge.toCharArray(), hashed).verified

    companion object {
        private const val HASH_COST = 12
    }
}
