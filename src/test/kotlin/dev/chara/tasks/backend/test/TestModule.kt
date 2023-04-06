package dev.chara.tasks.backend.test

import io.github.cdimascio.dotenv.dotenv
import org.koin.dsl.module

fun testModule() = module {
    single {
        dotenv {
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
    }
}