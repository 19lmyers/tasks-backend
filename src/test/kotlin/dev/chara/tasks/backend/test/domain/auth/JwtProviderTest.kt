package dev.chara.tasks.backend.test.domain.auth

import com.auth0.jwt.interfaces.DecodedJWT
import com.github.michaelbull.result.expectError
import com.github.michaelbull.result.getOrThrow
import dev.chara.tasks.backend.domain.auth.JwtProvider
import dev.chara.tasks.backend.test.testModule
import io.github.cdimascio.dotenv.Dotenv
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class JwtProviderTest : KoinTest {

    @Before
    fun init() {
        startKoin { modules(testModule()) }
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `instantiates without exception`() {
        val dotenv: Dotenv by inject()

        JwtProvider(dotenv)
    }

    @Test
    fun `verifies access token generated + token fields`() {
        val dotenv: Dotenv by inject()
        val jwtProvider = JwtProvider(dotenv)

        val user = "user@email.com"

        val tokenPair = jwtProvider.generateTokenPair(user)

        val decoded: DecodedJWT =
            jwtProvider.verify(tokenPair.access, JwtProvider.TokenType.ACCESS).getOrThrow()

        assertEquals(decoded.algorithm, "ES256")

        assertEquals(decoded.issuer, dotenv["TASKS_AUTH_ISSUER"])
        assertEquals(decoded.subject, JwtProvider.ACCESS_SUBJECT)
        assertContains(decoded.audience, jwtProvider.audience)

        assertEquals(decoded.getClaim(JwtProvider.CLAIM_ID).asString(), user)
    }

    @Test
    fun `verifies refresh token generated & token fields are valid`() {
        val dotenv: Dotenv by inject()
        val jwtProvider = JwtProvider(dotenv)

        val user = "user@email.com"

        val tokenPair = jwtProvider.generateTokenPair(user)

        val decoded: DecodedJWT =
            jwtProvider.verify(tokenPair.refresh, JwtProvider.TokenType.REFRESH).getOrThrow()

        assertEquals(decoded.algorithm, "ES256")

        assertEquals(decoded.issuer, dotenv["TASKS_AUTH_ISSUER"])
        assertEquals(decoded.subject, JwtProvider.REFRESH_SUBJECT)
        assertContains(decoded.audience, jwtProvider.audience)

        assertEquals(decoded.getClaim(JwtProvider.CLAIM_ID).asString(), user)
    }

    @Test
    fun `fail to validate token with different signature`() {
        val dotenv: Dotenv by inject()
        val jwtProvider = JwtProvider(dotenv)

        val jwt =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBdXRoIiwiaXNzIjoiaHR0cHM6Ly9sdWtlbXllcnMuZGV2IiwiaWF0IjoxNTE2MjM5MDIyLCJhdWQiOiJ0YXNrcy1hcGktdjMiLCJpZCI6InVzZXJAZW1haWwuY29tIn0.bQIv-OpYc5UrliyLlSmN8d35ffbKVluKaFQ5G2klfAGzI90OLHShFDaIh2LIpc6_0dedrThjKiyyWItppMV_Kg"

        jwtProvider.verify(jwt, JwtProvider.TokenType.ACCESS).expectError {}
    }
}
