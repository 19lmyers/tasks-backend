package dev.chara.tasks.backend.domain.job

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toErrorIfNull
import com.google.firebase.messaging.*
import dev.chara.tasks.backend.data.repository.FirebaseTokenRepository
import dev.chara.tasks.backend.data.repository.TaskListRepository
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.data.repository.UserRepository
import dev.chara.tasks.backend.domain.Action
import dev.chara.tasks.backend.domain.DomainError
import io.ktor.util.logging.*
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class NotifyActionJob : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("NotifyActionJob")

    private val userRepository: UserRepository by inject()
    private val taskListRepository: TaskListRepository by inject()
    private val taskRepository: TaskRepository by inject()
    private val firebaseTokenRepository: FirebaseTokenRepository by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        val dataMap = context.jobDetail.jobDataMap

        val listId = dataMap.getString(PARAM_LIST_ID)
        val taskId = dataMap.getString(PARAM_TASK_ID)
        val actorId = dataMap.getString(PARAM_ACTOR_ID)
        val action = Action.valueOf(dataMap.getString(PARAM_ACTION))

        binding {
            val task =
                taskRepository.getById(taskId).toErrorIfNull { DomainError.TaskNotFound }.bind()

            val taskList =
                taskListRepository
                    .getById(task.listId)
                    .toErrorIfNull { DomainError.ListNotFound }
                    .bind()

            val actor = userRepository.getById(actorId).bind()

            val body =
                when (action) {
                    Action.ADD_TASK -> "${actor?.display_name} added"
                    Action.EDIT_TASK -> "${actor?.display_name} edited"
                    Action.REMOVE_TASK -> "${actor?.display_name} removed"
                    Action.COMPLETE_TASK -> "${actor?.display_name} complete"
                    Action.STAR_TASK -> "${actor?.display_name} starred"
                    Action.PREDICT_TASK_CATEGORY -> "${task.label} → ${task.category}"
                    Action.CLEAR_COMPLETED_TASKS ->
                        "${actor?.display_name} deleted all completed tasks"
                    Action.JOIN_LIST -> "${actor?.display_name} joined"
                }

            val messages = mutableListOf<Pair<String, Message>>()

            val alert = ApsAlert.builder().setTitle(taskList.title).setBody(body).build()

            val aps = Aps.builder().setAlert(alert).build()

            val apnsConfig = ApnsConfig.builder().setAps(aps).build()

            firebaseTokenRepository
                .getForTask(taskId)
                .mapBoth(
                    success = { userTokens ->
                        for (token in userTokens) {
                            val message =
                                Message.builder()
                                    .putData(DATA_MESSAGE_TYPE, MESSAGE_TYPE_ACTION)
                                    .putData(DATA_TASK_ID, taskId)
                                    .putData(DATA_TASK_LABEL, task.label)
                                    .putData(DATA_TASK_CATEGORY, task.category ?: "")
                                    .putData(DATA_LIST_ID, listId)
                                    .putData(DATA_LIST_TITLE, taskList.title)
                                    .putData(DATA_LIST_ICON, taskList.icon?.name ?: "")
                                    .putData(DATA_LIST_COLOR, taskList.color?.name ?: "")
                                    .putData(DATA_ACTION, action.name)
                                    .putData(DATA_ACTOR_ID, actorId)
                                    .putData(DATA_ACTOR_NAME, actor?.display_name ?: "")
                                    .putData(DATA_ACTOR_PHOTO, actor?.profile_photo_uri ?: "")
                                    .setApnsConfig(apnsConfig)
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
    }

    companion object {
        const val PARAM_LIST_ID = "LIST_ID"
        const val PARAM_TASK_ID = "TASK_ID"
        const val PARAM_ACTOR_ID = "ACTOR_ID"
        const val PARAM_ACTION = "ACTION"

        const val DATA_MESSAGE_TYPE = "DATA_MESSAGE_TYPE"

        const val DATA_TASK_ID = "DATA_TASK_ID"
        const val DATA_TASK_LABEL = "DATA_TASK_LABEL"
        const val DATA_TASK_CATEGORY = "DATA_TASK_CATEGORY"

        const val DATA_LIST_ID = "DATA_LIST_ID"
        const val DATA_LIST_TITLE = "DATA_LIST_TITLE"
        const val DATA_LIST_ICON = "DATA_LIST_ICON"
        const val DATA_LIST_COLOR = "DATA_LIST_COLOR"

        const val DATA_ACTION = "DATA_ACTION"

        const val DATA_ACTOR_ID = "DATA_ACTOR_ID"
        const val DATA_ACTOR_NAME = "DATA_ACTOR_NAME"
        const val DATA_ACTOR_PHOTO = "DATA_ACTOR_PHOTO"

        const val MESSAGE_TYPE_ACTION = "MESSAGE_TYPE_ACTION"
    }
}
