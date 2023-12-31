package dev.chara.tasks.backend.web

import dev.chara.tasks.backend.web.routes.api.lists
import dev.chara.tasks.backend.web.routes.api.profile
import dev.chara.tasks.backend.web.routes.api.tasks
import dev.chara.tasks.backend.web.routes.auth
import dev.chara.tasks.backend.web.routes.fcm
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import org.koin.ktor.ext.inject

fun Routing.main() {
    status()
    auth()
    api()
    assets()
    fcm()
}

fun Route.status() {
    get { call.respondText("[v4] Hello, world!") }
}

fun Route.api() {
    authenticate {
        profile()
        lists()
        tasks()
    }
}

fun Route.assets() {
    val dotenv by inject<Dotenv>()
    val assetsFolder = dotenv["TASKS_ASSETS_FOLDER"]

    staticFiles("/assets", File(assetsFolder))
}
