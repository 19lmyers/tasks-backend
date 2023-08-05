package dev.chara.tasks.backend.domain.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*

val Firebase =
    createApplicationPlugin(name = "FirebasePlugin") {
        val credentials = GoogleCredentials.getApplicationDefault()

        val options: FirebaseOptions = FirebaseOptions.builder().setCredentials(credentials).build()

        FirebaseApp.initializeApp(options)
    }
