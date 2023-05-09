package dev.chara.tasks.backend.domain.job

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import dev.chara.tasks.backend.data.repository.UserRepository
import io.ktor.util.logging.*
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class PasswordResetTokenJob() : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("PasswordResetTokenJob")

    private val repository: UserRepository by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        repository.getAllPasswordResetTokens().mapBoth(
            success = { tokens ->
                for (resetToken in tokens) {
                    if (resetToken.expiry_time <= Clock.System.now()) {
                        repository.invalidatePasswordResetToken(resetToken.reset_token).mapError {
                            logger.error(it.throwable)
                        }
                    }
                }
            },
            failure = {
                logger.error(it.throwable)
            }
        )
    }
}