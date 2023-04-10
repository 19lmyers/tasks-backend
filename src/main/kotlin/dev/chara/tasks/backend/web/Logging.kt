package dev.chara.tasks.backend.web

import dev.chara.tasks.backend.ApplicationError
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("Routing")

private fun PipelineContext<Unit, ApplicationCall>.getIP(): String =
    context.request.header("X-Forwarded-For") ?: context.request.origin.remoteHost

fun PipelineContext<Unit, ApplicationCall>.logTrace(action: String) =
    logger.trace("${getIP()}: $action")

fun PipelineContext<Unit, ApplicationCall>.logError(error: ApplicationError) =
    logger.error("${getIP()}: $error")

fun PipelineContext<Unit, ApplicationCall>.logError(error: Throwable) =
    logger.error("${getIP()}: ${error.message}")
