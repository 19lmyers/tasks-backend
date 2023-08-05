package dev.chara.tasks.backend.domain.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.runCatching
import dev.chara.tasks.backend.domain.model.TokenPair
import io.github.cdimascio.dotenv.Dotenv
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class JwtProvider(dotenv: Dotenv) {

    private val publicKeyId: String = dotenv["TASKS_AUTH_PUBLIC_KEY_ID"]
    private val privateKeyPath: String = dotenv["TASKS_AUTH_PRIVATE_KEY_PATH"]

    private val issuer: String = dotenv["TASKS_AUTH_ISSUER"]
    val audience: String = "tasks-api-v3"

    private val jwkProvider: JwkProvider =
        JwkProviderBuilder(issuer)
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    private val publicKey: ECPublicKey = jwkProvider.get(publicKeyId).publicKey as ECPublicKey
    private val keySpecPKCS8: PKCS8EncodedKeySpec by lazy {
        PKCS8EncodedKeySpec(File(privateKeyPath).readBytes())
    }
    private val privateKey: ECPrivateKey =
        KeyFactory.getInstance("EC").generatePrivate(keySpecPKCS8) as ECPrivateKey

    val accessVerifier: JWTVerifier =
        JWT.require(Algorithm.ECDSA256(publicKey, privateKey))
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(ACCESS_SUBJECT)
            .build()

    private val refreshVerifier: JWTVerifier =
        JWT.require(Algorithm.ECDSA256(publicKey, privateKey))
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(REFRESH_SUBJECT)
            .build()

    fun generateTokenPair(userId: String): TokenPair {
        val currentTime = System.currentTimeMillis()

        val accessToken =
            JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withSubject(ACCESS_SUBJECT)
                .withClaim(CLAIM_ID, userId)
                .withExpiresAt(Date(currentTime + Duration.ofMinutes(ACCESS_LIFETIME).toMillis()))
                .sign(Algorithm.ECDSA256(publicKey, privateKey))

        val refreshToken =
            JWT.create()
                .withAudience(audience)
                .withIssuer(issuer)
                .withSubject(REFRESH_SUBJECT)
                .withClaim(CLAIM_ID, userId)
                .withExpiresAt(Date(currentTime + Duration.ofDays(REFRESH_LIFETIME).toMillis()))
                .sign(Algorithm.ECDSA256(publicKey, privateKey))

        return TokenPair(accessToken, refreshToken)
    }

    fun verify(token: String, type: TokenType) = runCatching {
        when (type) {
            TokenType.ACCESS -> {
                accessVerifier.verify(token)
            }
            TokenType.REFRESH -> {
                refreshVerifier.verify(token)
            }
        }
    }

    enum class TokenType {
        ACCESS,
        REFRESH
    }

    companion object {
        const val ACCESS_SUBJECT = "auth"
        private const val ACCESS_LIFETIME: Long = 5 // minutes

        const val REFRESH_SUBJECT = "refresh"
        private const val REFRESH_LIFETIME: Long = 175 // days

        const val CLAIM_ID = "id"
    }
}
