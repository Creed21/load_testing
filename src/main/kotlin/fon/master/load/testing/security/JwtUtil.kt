package fon.master.load.testing.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtUtil(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration}") private val expirationMs: Long
) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun generate(username: String, role: String): String {
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val now = Instant.now().toEpochMilli()
        val payload = encoder.encodeToString(
            """{"sub":"$username","role":"$role","iat":$now,"exp":${now + expirationMs}}""".toByteArray()
        )
        return "$header.$payload.${sign("$header.$payload")}"
    }

    fun isValid(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false
            if (sign("${parts[0]}.${parts[1]}") != parts[2]) return false
            val exp = claims(parts[1])["exp"]?.toLong() ?: return false
            Instant.now().toEpochMilli() < exp
        } catch (e: Exception) { false }
    }

    fun extractUsername(token: String): String? = runCatching {
        claims(token.split(".")[1])["sub"]
    }.getOrNull()

    fun extractRole(token: String): String? = runCatching {
        claims(token.split(".")[1])["role"]
    }.getOrNull()

    private fun claims(payloadB64: String): Map<String, String> {
        val json = String(decoder.decode(payloadB64))
        val result = mutableMapOf<String, String>()
        Regex(""""(\w+)"\s*:\s*(?:"([^"]*)"|([\d.]+))""").findAll(json).forEach { m ->
            result[m.groupValues[1]] = m.groupValues[2].ifEmpty { m.groupValues[3] }
        }
        return result
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(data.toByteArray()))
    }
}
