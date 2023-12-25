package dev.chara.tasks.backend.domain.job.gemini

import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toErrorIfNull
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import dev.chara.tasks.backend.data.client.GeminiClient
import dev.chara.tasks.backend.data.repository.FirebaseTokenRepository
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.job.CoroutineJob
import io.ktor.util.logging.*
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class CategoryPredictionJob : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("CategoryPredictionJob")

    private val taskRepository: TaskRepository by inject()
    private val firebaseTokenRepository: FirebaseTokenRepository by inject()
    private val geminiClient: GeminiClient by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        val dataMap = context.jobDetail.jobDataMap

        val userId = dataMap.getString(PARAM_USER_ID)
        val listId = dataMap.getString(PARAM_LIST_ID)
        val taskId = dataMap.getString(PARAM_TASK_ID)
        val label = dataMap.getString(PARAM_TASK_LABEL)

        logger.info("Predicting category with classifier")

        var await = true

        geminiClient.predict(label) { result ->
            binding {
                    val prediction = result.bind()

                    val task =
                        taskRepository
                            .getByIds(userId, listId, taskId)
                            .toErrorIfNull { DomainError.TaskNotFound }
                            .bind()

                    taskRepository
                        .update(
                            userId,
                            listId,
                            taskId,
                            task.copy(category = prediction, lastModified = Clock.System.now())
                        )
                        .bind()

                    val messages = mutableListOf<Pair<String, Message>>()

                    firebaseTokenRepository
                        .getForUser(userId)
                        .mapBoth(
                            success = { userTokens ->
                                for (token in userTokens) {
                                    val message =
                                        Message.builder()
                                            .putData(DATA_MESSAGE_TYPE, MESSAGE_TYPE_PREDICTION)
                                            .putData(DATA_TASK_ID, taskId)
                                            .putData(DATA_TASK_CATEGORY, prediction)
                                            .setToken(token)
                                            .build()

                                    messages.add(token to message)
                                }
                            },
                            failure = { logger.error(it.throwable) }
                        )

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
                }
                .mapBoth(
                    success = { logger.info("Prediction successful") },
                    failure = { error -> logger.error("Prediction failed: $error") }
                )

            await = false
        }

        while (await) {}
    }

    companion object {
        const val PARAM_USER_ID = "USER_ID"
        const val PARAM_LIST_ID = "LIST_ID"
        const val PARAM_TASK_ID = "TASK_ID"
        const val PARAM_TASK_LABEL = "TASK_LABEL"

        const val DATA_MESSAGE_TYPE = "DATA_MESSAGE_TYPE"
        const val DATA_TASK_ID = "DATA_TASK_ID"
        const val DATA_TASK_CATEGORY = "DATA_TASK_CATEGORY"

        const val MESSAGE_TYPE_PREDICTION = "MESSAGE_TYPE_PREDICTION"
    }
}
