package dev.chara.tasks.backend.domain

import dev.chara.tasks.backend.domain.job.CategoryPredictionJob
import dev.chara.tasks.backend.domain.job.NotifyActionJob
import org.quartz.DateBuilder
import org.quartz.DateBuilder.futureDate
import org.quartz.JobBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

class JobScheduler {
    private val scheduler = StdSchedulerFactory.getDefaultScheduler()

    fun notifyAction(listId: String, taskId: String, actorId: String, action: Action) {
        val job =
            JobBuilder.newJob()
                .ofType(NotifyActionJob::class.java)
                .usingJobData(NotifyActionJob.PARAM_LIST_ID, listId)
                .usingJobData(NotifyActionJob.PARAM_TASK_ID, taskId)
                .usingJobData(NotifyActionJob.PARAM_ACTOR_ID, actorId)
                .usingJobData(NotifyActionJob.PARAM_ACTION, action.name)
                .build()

        val trigger: Trigger =
            TriggerBuilder.newTrigger()
                .startAt(futureDate(DELAY, DateBuilder.IntervalUnit.SECOND))
                .build()

        scheduler.scheduleJob(job, trigger)
    }

    fun predictCategory(listId: String, taskId: String, label: String) {
        val job =
            JobBuilder.newJob()
                .ofType(CategoryPredictionJob::class.java)
                .usingJobData(CategoryPredictionJob.PARAM_LIST_ID, taskId)
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
