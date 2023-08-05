package dev.chara.tasks.backend.data

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.cdimascio.dotenv.Dotenv
import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail

data class Mail(val subject: String, val body: String)

class MailSender(dotenv: Dotenv) {

    private val smtpServer = dotenv["SMTP_SERVER"]
    private val smtpServerPort = dotenv["SMTP_PORT"].toInt()

    private val smtpLogin = dotenv["SMTP_LOGIN"]
    private val smtpPassword = dotenv["SMTP_PASSWORD"]

    fun send(recipient: String, name: String, mail: Mail) =
        runCatching {
                val email: Email =
                    SimpleEmail().apply {
                        hostName = smtpServer
                        setSmtpPort(smtpServerPort)
                        setAuthentication(smtpLogin, smtpPassword)
                        isSSLOnConnect = true

                        setFrom("noreply@tasks.chara.dev", "Tasks (chara.dev)")

                        subject = mail.subject
                        setMsg(mail.body)

                        addTo(recipient, name)
                    }

                email.send()

                Unit
            }
            .mapError { DataError.SMTPError(it) }
}
