package dev.chara.tasks.backend.domain.job

import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.toErrorIfNull
import dev.chara.tasks.backend.data.client.GeminiClient
import dev.chara.tasks.backend.data.repository.TaskRepository
import dev.chara.tasks.backend.domain.Action
import dev.chara.tasks.backend.domain.DomainError
import dev.chara.tasks.backend.domain.JobScheduler
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class CategoryPredictionJob : CoroutineJob() {

    private val logger = LoggerFactory.getLogger("CategoryPredictionJob")

    private val taskRepository: TaskRepository by inject()
    private val geminiClient: GeminiClient by inject()

    private val jobScheduler: JobScheduler by inject()

    override suspend fun executeAsCoroutine(context: JobExecutionContext?) {
        if (context == null) {
            return
        }

        val dataMap = context.jobDetail.jobDataMap

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
                            .getById(taskId)
                            .toErrorIfNull { DomainError.TaskNotFound }
                            .bind()

                    taskRepository
                        .update(
                            taskId,
                            task.copy(category = prediction, lastModified = Clock.System.now())
                        )
                        .bind()

                    jobScheduler.notifyAction(listId, taskId, "NIL", Action.PREDICT_TASK_CATEGORY)
                }
                .mapBoth(
                    success = { logger.info("Prediction successful") },
                    failure = { error -> logger.error("Prediction failed: $error") }
                )

            await = false
        }

        @Suppress("ControlFlowWithEmptyBody") while (await) {}
    }

    companion object {
        const val PARAM_LIST_ID = "LIST_ID"
        const val PARAM_TASK_ID = "TASK_ID"
        const val PARAM_TASK_LABEL = "TASK_LABEL"

        const val DATA_MESSAGE_TYPE = "DATA_MESSAGE_TYPE"
        const val DATA_TASK_ID = "DATA_TASK_ID"
        const val DATA_TASK_CATEGORY = "DATA_TASK_CATEGORY"

        const val MESSAGE_TYPE_PREDICTION = "MESSAGE_TYPE_PREDICTION"
    }
}
