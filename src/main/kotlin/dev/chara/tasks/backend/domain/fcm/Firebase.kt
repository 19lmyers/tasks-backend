package dev.chara.tasks.backend.domain.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream

val Firebase = createApplicationPlugin(
    name = "FirebasePlugin",
    createConfiguration = ::FirebaseConfiguration
) {
    if (pluginConfig.configFilePath.isNullOrEmpty()) {
        throw IllegalArgumentException("Invalid configuration file path!")
    }

    val serviceAccount = FileInputStream(pluginConfig.configFilePath!!)

    val credentials = GoogleCredentials.fromStream(serviceAccount)

    val options: FirebaseOptions = FirebaseOptions.builder()
        .setCredentials(credentials)
        .build()

    FirebaseApp.initializeApp(options)
}

class FirebaseConfiguration {
    var configFilePath: String? = null
}