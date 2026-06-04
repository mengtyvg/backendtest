package com.mengty.report.service

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.dto.LoginResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JwtClaims(
    val userId: UUID,
    val username: String,
    val role: String,
    val expiresAt: Instant
)

@Service
class JwtService(
    private val objectMapper: ObjectMapper,
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-minutes:60}") private val expirationMinutes: Long
) {
    private val signingKey = secret.toByteArray(StandardCharsets.UTF_8)
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()

    init {
        require(signingKey.size >= 32) { "JWT_SECRET must contain at least 32 UTF-8 bytes." }
        require(expirationMinutes > 0) { "jwt.expiration-minutes must be greater than zero." }
    }

    fun createToken(user: AppUserDTO): LoginResponse {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(expirationMinutes * 60)
        val header = encodeJson(mapOf("alg" to "HS256", "typ" to "JWT"))
        val payload = encodeJson(
            mapOf(
                "sub" to user.id.toString(),
                "username" to user.username,
                "role" to user.role,
                "iat" to issuedAt.epochSecond,
                "exp" to expiresAt.epochSecond
            )
        )
        val unsignedToken = "$header.$payload"
        val token = "$unsignedToken.${sign(unsignedToken)}"

        return LoginResponse(
            accessToken = token,
            expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC),
            user = user
        )
    }

    fun parseAndValidate(token: String): JwtClaims {
        val parts = token.split('.')
        require(parts.size == 3) { "Malformed JWT." }

        val unsignedToken = "${parts[0]}.${parts[1]}"
        val expectedSignature = sign(unsignedToken)
        require(
            MessageDigest.isEqual(
                expectedSignature.toByteArray(StandardCharsets.US_ASCII),
                parts[2].toByteArray(StandardCharsets.US_ASCII)
            )
        ) { "Invalid JWT signature." }

        val payload = objectMapper.readTree(base64Decoder.decode(parts[1]))
        val expiresAt = Instant.ofEpochSecond(payload["exp"].asLong())
        require(expiresAt.isAfter(Instant.now())) { "JWT has expired." }

        return JwtClaims(
            userId = UUID.fromString(payload["sub"].asText()),
            username = payload["username"].asText(),
            role = payload["role"].asText(),
            expiresAt = expiresAt
        )
    }

    private fun encodeJson(value: Any): String =
        base64Encoder.encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(unsignedToken: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingKey, "HmacSHA256"))
        return base64Encoder.encodeToString(mac.doFinal(unsignedToken.toByteArray(StandardCharsets.US_ASCII)))
    }
}
