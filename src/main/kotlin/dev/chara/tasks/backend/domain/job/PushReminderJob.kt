package dev.chara.tasks.backend.domain.job

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.google.firebase.messaging.*
import dev.chara.tasks.backend.data.repository.FirebaseTokenRepository
import dev.chara.tasks.backend.data.repository.ReminderRepository
import io.ktor.util.logging.*
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class PushReminderJob : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("PushReminderJob")

    private val reminderRepository: ReminderRepository by inject()
    private val firebaseTokenRepository: FirebaseTokenRepository by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        reminderRepository
            .get()
            .mapBoth(
                success = { reminders ->
                    val messages = mutableListOf<Pair<String, Message>>()

                    for (reminder in reminders) {
                        logger.info("Dispatching reminder for task ${reminder.task_id}")

                        val alert =
                            ApsAlert.builder()
                                .setTitle(reminder.task_label)
                                .setBody(reminder.list_title)
                                .build()

                        val aps = Aps.builder().setAlert(alert).setCategory("reminder").build()

                        val apnsConfig = ApnsConfig.builder().setAps(aps).build()

                        firebaseTokenRepository
                            .getForTask(reminder.task_id)
                            .mapBoth(
                                success = { userTokens ->
                                    for (token in userTokens) {
                                        val message =
                                            Message.builder()
                                                .putData(DATA_MESSAGE_TYPE, MESSAGE_TYPE_REMINDER)
                                                .putData(DATA_TASK_ID, reminder.task_id)
                                                .putData(DATA_TASK_LABEL, reminder.task_label)
                                                .putData(DATA_LIST_TITLE, reminder.list_title)
                                                .putData(
                                                    DATA_LIST_COLOR,
                                                    reminder.list_color?.name ?: ""
                                                )
                                                .putData(
                                                    DATA_LIST_ICON,
                                                    reminder.list_icon?.name ?: ""
                                                )
                                                .setApnsConfig(apnsConfig)
                                                .setToken(token)
                                                .build()

                                        messages.add(token to message)

                                        reminderRepository.setFired(reminder.task_id).mapError {
                                            logger.error(it.throwable)
                                        }
                                    }
                                },
                                failure = { logger.error(it.throwable) }
                            )
                    }

                    for (batch in messages.chunked(500)) {
                        val batchResponse =
                            FirebaseMessaging.getInstance().sendEach(batch.map { it.second })

                        if (batchResponse.failureCount > 0) {
                            for ((index, response) in batchResponse.responses.withIndex()) {
                                if (!response.isSuccessful) {
                                    val errorCode = response.exception.messagingErrorCode

                                    logger.warn("Error code: {}", errorCode)

                                    if (
                                        errorCode == MessagingErrorCode.UNREGISTERED ||
                                            errorCode == MessagingErrorCode.INVALID_ARGUMENT
                                    ) {
                                        val failedToken = batch[index].first
                                        firebaseTokenRepository.invalidate(failedToken).mapError {
                                            logger.error(it.throwable)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                failure = { logger.error(it.throwable) }
            )
    }

    companion object {
        const val DATA_MESSAGE_TYPE = "DATA_MESSAGE_TYPE"
        const val DATA_TASK_ID = "DATA_TASK_ID"
        const val DATA_TASK_LABEL = "DATA_TASK_LABEL"
        const val DATA_LIST_TITLE = "DATA_LIST_TITLE"
        const val DATA_LIST_COLOR = "DATA_LIST_COLOR"
        const val DATA_LIST_ICON = "DATA_LIST_ICON"

        const val MESSAGE_TYPE_REMINDER = "MESSAGE_TYPE_REMINDER"
    }
}
