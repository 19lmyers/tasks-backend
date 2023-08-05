package dev.chara.tasks.backend.data

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.cdimascio.dotenv.Dotenv
import java.io.File
import kotlinx.datetime.Clock

class AssetManager(dotenv: Dotenv) {
    private val baseUrl = dotenv["TASKS_BASE_URL"]
    private val assetsFolder = dotenv["TASKS_ASSETS_FOLDER"]

    fun saveProfilePhoto(userId: String, photoBytes: ByteArray) =
        runCatching {
                val file = File("$assetsFolder/profiles/${userId}.jpg")
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeBytes(photoBytes)

                val timestamp = Clock.System.now().toEpochMilliseconds()
                "$baseUrl/assets/profiles/${userId}.jpg?$timestamp"
            }
            .mapError { DataError.IOError(it) }
}
