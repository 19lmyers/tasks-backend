package dev.chara.tasks.backend.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.chara.tasks.backend.data.sql.*
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.datetime.*
import org.mariadb.jdbc.MariaDbDataSource
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val instantAdapter = object : ColumnAdapter<Instant, LocalDateTime> {
    override fun decode(databaseValue: LocalDateTime) =
        databaseValue.toKotlinLocalDateTime().toInstant(TimeZone.UTC)

    override fun encode(value: Instant): LocalDateTime =
        value.toLocalDateTime(TimeZone.UTC)
            .toJavaLocalDateTime()
            .truncatedTo(ChronoUnit.MICROS)
}

class DatabaseFactory(dotenv: Dotenv) {
    private val config = HikariConfig()

    init {
        config.dataSourceClassName = MariaDbDataSource::class.java.name
        config.addDataSourceProperty("url", dotenv["TASKS_DATA_SOURCE_URL"])
        config.addDataSourceProperty("user", dotenv["MYSQL_USER"])
        config.addDataSourceProperty("password", dotenv["MYSQL_PASSWORD"])
    }

    private val dataSource: HikariDataSource = HikariDataSource(config)
    private val databaseDriver = dataSource.asJdbcDriver()

    private val database = Database(
        databaseDriver,
        FirebaseToken.Adapter(
            timestampAdapter = instantAdapter
        ),
        PasswordResetToken.Adapter(
            expiry_timeAdapter = instantAdapter
        ),
        Task.Adapter(
            reminder_dateAdapter = instantAdapter,
            due_dateAdapter = instantAdapter,
            last_modifiedAdapter = instantAdapter,
            reminder_firedAdapter = instantAdapter,
            date_createdAdapter = instantAdapter
        ),
        TaskList.Adapter(
            last_modifiedAdapter = instantAdapter,
            date_createdAdapter = instantAdapter,
            sort_typeAdapter = EnumColumnAdapter(),
            sort_directionAdapter = EnumColumnAdapter(),
            colorAdapter = EnumColumnAdapter(),
            iconAdapter = EnumColumnAdapter()
        )
    )

    private fun getDatabaseVersion(): Int = try {
        database.dbMetaQueries.getVersion().executeAsOneOrNull() ?: 0
    } catch (ignored: Exception) {
        0
    }

    init {
        val currentVersion = getDatabaseVersion()
        Database.Schema.migrate(databaseDriver, currentVersion, Database.Schema.version)
    }

    fun getDriver(): SqlDriver {
        return databaseDriver
    }

    fun getDatabase(): Database {
        return database
    }
}