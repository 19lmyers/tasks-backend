package dev.chara.tasks.backend.domain

import dev.chara.tasks.backend.domain.job.PushReminderJob
import dev.chara.tasks.backend.domain.job.TokenExpiryJob
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

fun initScheduler() {
    val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    // Reminder notifications (FCM)

    val reminderJob = JobBuilder.newJob()
        .ofType(PushReminderJob::class.java)
        .withIdentity("PushReminderJob")
        .build()

    val reminderTrigger = TriggerBuilder.newTrigger()
        .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever())
        .withIdentity("PushReminderTrigger")
        .build()

    scheduler.scheduleJob(reminderJob, reminderTrigger)

    // Password reset token expiry

    val tokenExpiryJob = JobBuilder.newJob()
        .ofType(TokenExpiryJob::class.java)
        .withIdentity("TokenExpiryJob")
        .build()

    val tokenExpiryTrigger = TriggerBuilder.newTrigger()
        .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
        .withIdentity("TokenExpiryTrigger")
        .build()

    scheduler.scheduleJob(tokenExpiryJob, tokenExpiryTrigger)

    scheduler.start()
}