package dev.chara.tasks.backend

import dev.chara.tasks.backend.domain.auth.JwtProvider
import dev.chara.tasks.backend.domain.fcm.Firebase
import dev.chara.tasks.backend.domain.initScheduler
import dev.chara.tasks.backend.inject.appModule
import dev.chara.tasks.backend.web.main
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

@Suppress("ExtractKtorModule")
fun main() {
    embeddedServer(Netty, port = 8123) {
        install(Koin) {
            modules(appModule())
        }
        module()
    }.start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            this.explicitNulls = false
            this.ignoreUnknownKeys = true
        })
    }

    // TODO is this fixed
    install(Firebase) {
        configFilePath = "service-account.json"
    }

    val jwtProvider by inject<JwtProvider>()
    install(Authentication) {
        jwt {
            verifier(jwtProvider.accessVerifier)
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respondText("Access token invalid or expired", status = HttpStatusCode.Unauthorized)
            }
        }
    }

    install(Routing) {
        main()
    }

    initScheduler()
}