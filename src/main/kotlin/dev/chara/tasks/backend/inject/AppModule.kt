package dev.chara.tasks.backend.inject

import dev.chara.tasks.backend.data.AssetManager
import dev.chara.tasks.backend.data.DatabaseFactory
import dev.chara.tasks.backend.data.MailSender
import dev.chara.tasks.backend.data.repository.*
import dev.chara.tasks.backend.domain.auth.JwtProvider
import dev.chara.tasks.backend.domain.service.FirebaseTokenService
import dev.chara.tasks.backend.domain.service.TaskListService
import dev.chara.tasks.backend.domain.service.TaskService
import dev.chara.tasks.backend.domain.service.UserService
import io.github.cdimascio.dotenv.dotenv
import org.koin.dsl.module

fun appModule() = module {
    single {
        dotenv {
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
    }

    single { AssetManager(get()) }
    factory { DatabaseFactory(get()) }

    single { JwtProvider(get()) }

    single { MailSender(get()) }

    single { UserRepository(get()) }
    single { TaskListRepository(get()) }
    single { TaskRepository(get()) }
    single { ReminderRepository(get()) }
    single { FirebaseTokenRepository(get()) }

    single { UserService(get(), get(), get(), get()) }
    single { TaskListService(get()) }
    single { TaskService(get()) }
    single { FirebaseTokenService(get()) }
}
