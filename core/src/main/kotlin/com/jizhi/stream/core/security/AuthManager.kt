package com.jizhi.stream.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class AuthManager(private val password: String) {
    private val random = SecureRandom()
    private val failedAttempts = ConcurrentHashMap<String, MutableList<Long>>()

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 60_000L
        const val CHALLENGE_SIZE = 32
    }

    fun generateChallenge(): ByteArray {
        val challenge = ByteArray(CHALLENGE_SIZE)
        random.nextBytes(challenge)
        return challenge
    }

    fun isLocked(ip: String): Boolean {
        val attempts = failedAttempts[ip] ?: return false
        val now = System.currentTimeMillis()
        attempts.removeAll { now - it > LOCKOUT_MS }
        return attempts.size >= MAX_ATTEMPTS
    }

    fun computeResponse(challenge: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(challenge)
        md.update(password.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    fun verify(challenge: ByteArray, response: ByteArray, ip: String): Boolean {
        if (isLocked(ip)) return false
        val expected = computeResponse(challenge)
        val ok = expected.contentEquals(response)
        if (!ok) {
            failedAttempts.getOrPut(ip) { mutableListOf() }.add(System.currentTimeMillis())
        } else {
            failedAttempts.remove(ip)
        }
        return ok
    }
}
