package dev.chara.tasks.backend.domain

import dev.chara.tasks.backend.v2.scheduler.job.PasswordResetTokenJob
import dev.chara.tasks.backend.v2.scheduler.job.PushReminderJob
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

    val passwordResetTokenJob = JobBuilder.newJob()
        .ofType(PasswordResetTokenJob::class.java)
        .withIdentity("PasswordResetTokenJob")
        .build()

    val passwordResetTokenTrigger = TriggerBuilder.newTrigger()
        .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
        .withIdentity("PasswordResetTokenTrigger")
        .build()

    scheduler.scheduleJob(passwordResetTokenJob, passwordResetTokenTrigger)

    scheduler.start()
}