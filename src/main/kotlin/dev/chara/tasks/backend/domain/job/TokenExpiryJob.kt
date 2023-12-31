package dev.chara.tasks.backend.domain.job

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import dev.chara.tasks.backend.data.repository.UserRepository
import io.ktor.util.logging.*
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class TokenExpiryJob : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("TokenExpiryJob")

    private val repository: UserRepository by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        repository
            .getAllEmailVerificationTokens()
            .mapBoth(
                success = { tokens ->
                    for (verifyToken in tokens) {
                        if (verifyToken.expiry_time <= Clock.System.now()) {
                            repository
                                .invalidateEmailVerificationToken(verifyToken.verify_token)
                                .mapError { logger.error(it.throwable) }
                        }
                    }
                },
                failure = { logger.error(it.throwable) }
            )

        repository
            .getAllPasswordResetTokens()
            .mapBoth(
                success = { tokens ->
                    for (resetToken in tokens) {
                        if (resetToken.expiry_time <= Clock.System.now()) {
                            repository
                                .invalidatePasswordResetToken(resetToken.reset_token)
                                .mapError { logger.error(it.throwable) }
                        }
                    }
                },
                failure = { logger.error(it.throwable) }
            )

        repository
            .getAllListInviteTokens()
            .mapBoth(
                success = { tokens ->
                    for (inviteToken in tokens) {
                        if (inviteToken.expiry_time <= Clock.System.now()) {
                            repository
                                .invalidateListInviteToken(inviteToken.invite_token)
                                .mapError { logger.error(it.throwable) }
                        }
                    }
                },
                failure = { logger.error(it.throwable) }
            )
    }
}
