package dev.chara.tasks.backend.data

import dev.chara.tasks.backend.domain.job.gemini.CategoryPredictionJob
import org.quartz.DateBuilder
import org.quartz.DateBuilder.futureDate
import org.quartz.JobBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

class GeminiJobScheduler {

    private val scheduler = StdSchedulerFactory.getDefaultScheduler()

    fun predict(userId: String, listId: String, taskId: String, label: String) {
        val job =
            JobBuilder.newJob()
                .ofType(CategoryPredictionJob::class.java)
                .usingJobData(CategoryPredictionJob.PARAM_USER_ID, userId)
                .usingJobData(CategoryPredictionJob.PARAM_LIST_ID, listId)
                .usingJobData(CategoryPredictionJob.PARAM_TASK_ID, taskId)
                .usingJobData(CategoryPredictionJob.PARAM_TASK_LABEL, label)
                .build()

        val trigger: Trigger =
            TriggerBuilder.newTrigger()
                .startAt(futureDate(DELAY, DateBuilder.IntervalUnit.SECOND))
                .build()

        scheduler.scheduleJob(job, trigger)
    }

    companion object {
        const val DELAY = 0
    }
}
