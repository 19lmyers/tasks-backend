package dev.chara.tasks.backend.data.client

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.chara.tasks.backend.ApplicationError
import dev.chara.tasks.backend.data.DataError
import dev.chara.tasks.backend.data.GeminiError
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Payload(val status: Int, val result: String)

class GeminiClient(dotenv: Dotenv) {

    private val host = dotenv["GEMINI_HOST"]
    private val port = dotenv["GEMINI_PORT"].toInt()

    private suspend fun execute(
        command: String,
        onResult: suspend (Result<String, ApplicationError>) -> Unit
    ) {
        try {
            val client = HttpClient(OkHttp) { install(WebSockets) }

            client.webSocket(method = HttpMethod.Get, host = host, port = port) {
                send(command)

                val frame = incoming.receive() as Frame.Text
                val json = frame.readText()

                val payload = Json.decodeFromString<Payload>(json)

                if (payload.status == 0) {
                    onResult(Ok(payload.result!!))
                } else {
                    onResult(Err(GeminiError(payload.status)))
                }
            }
        } catch (ex: Exception) {
            onResult(Err(DataError.SocketError(ex)))
        }
    }

    suspend fun predict(
        input: String,
        onResult: suspend (Result<String, ApplicationError>) -> Unit
    ) = execute("\"${input}\"", onResult)
}
