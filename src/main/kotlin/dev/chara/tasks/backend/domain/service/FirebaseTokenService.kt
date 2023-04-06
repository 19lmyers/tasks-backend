package dev.chara.tasks.backend.domain.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import dev.chara.tasks.backend.ApplicationError
import dev.chara.tasks.backend.data.repository.FirebaseTokenRepository
import dev.chara.tasks.backend.domain.DomainError

class FirebaseTokenService(private val repository: FirebaseTokenRepository) {

    fun getForUser(userId: String) = repository.getForUser(userId)

    fun link(userId: String, token: String): Result<Unit, ApplicationError> {
        return if (token.isBlank()) {
            Err(DomainError.FirebaseTokenRequired)
        } else {
            repository.link(userId, token)
        }
    }

    fun invalidate(token: String) = repository.invalidate(token)
}