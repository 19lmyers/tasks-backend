plugins {
    application

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)

    alias(libs.plugins.ktor)

    alias(libs.plugins.sqldelight)

    alias(libs.plugins.ktfmt)

    alias(libs.plugins.versions)
}

group = "dev.chara"

version = "0.3"

kotlin { jvmToolchain(21) }

application { mainClass.set("dev.chara.tasks.backend.ApplicationKt") }

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.network.tls.certificates)

    implementation(libs.logback)

    implementation(libs.koin.ktor)

    implementation(libs.sqldelight.coroutines.extensions)
    implementation(libs.sqldelight.jdbc.driver)

    implementation(libs.hikari.cp)
    implementation(libs.mariadb.java.client)

    implementation(libs.quartz.scheduler)

    implementation(libs.firebase.admin)

    implementation(libs.bcrypt)

    implementation(libs.validator.core)
    implementation(libs.validator.email)

    implementation(libs.nbvcxz)

    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlin.result)
    implementation(libs.kotlin.result.coroutines)

    implementation(libs.dotenv)

    implementation(libs.apache.commons.email)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.koin.test)
    testImplementation(libs.mockk)
}

sqldelight {
    databases {
        create("Database") {
            dialect(libs.sqldelight.dialect)

            packageName.set("dev.chara.tasks.backend.data.sql")
            version = 1

            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}

ktfmt { kotlinLangStyle() }

// Fix the wrong version of Guava listenablefuture being pulled in
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
    // and/or
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.guava:listenablefuture") {
        select("com.google.guava:guava:0")
    }
}