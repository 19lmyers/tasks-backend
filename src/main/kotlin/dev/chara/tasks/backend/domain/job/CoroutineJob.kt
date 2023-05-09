package dev.chara.tasks.backend.domain.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.quartz.Job
import org.quartz.JobExecutionContext

abstract class CoroutineJob : Job, KoinComponent {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    final override fun execute(context: JobExecutionContext?) {
        coroutineScope.launch {
            executeAsCoroutine(context)
        }
    }

    abstract suspend fun executeAsCoroutine(context: JobExecutionContext?)
}