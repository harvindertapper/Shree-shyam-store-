package com.aistudio.shreeshyamstore.pqwzkb.data

import java.security.MessageDigest

/**
 * The authority that authenticated the current device session.
 *
 * A session must have exactly one authority. Firebase and local credentials are
 * never silently combined for one active session.
 */
enum class IdentityProvider {
    FIREBASE,
    LOCAL;

    companion object {
        fun fromStored(value: String?): IdentityProvider? = value
            ?.trim()
            ?.uppercase()
            ?.let { normalized -> values().firstOrNull { it.name == normalized } }
    }
}

data class IdentitySession(
    val provider: IdentityProvider,
    val uid: String,
    val username: String,
    val email: String,
    val role: String = "OWNER"
) {
    val shopUid: String
        get() = uid.trim()

    fun normalized(): IdentitySession = copy(
        uid = uid.trim(),
        username = username.trim(),
        email = email.trim().lowercase(),
        role = role.trim().ifEmpty { "OWNER" }.uppercase()
    )

    fun isUsable(): Boolean = normalized().shopUid.isNotEmpty()

    companion object {
        fun localForUser(username: String, email: String, existingUid: String = ""): IdentitySession {
            val normalizedEmail = email.trim().lowercase()
            val uid = existingUid.trim().ifEmpty { localUidForEmail(normalizedEmail) }
            return IdentitySession(
                provider = IdentityProvider.LOCAL,
                uid = uid,
                username = username,
                email = normalizedEmail
            ).normalized()
        }

        fun localUidForEmail(email: String): String {
            val normalizedEmail = email.trim().lowercase()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(normalizedEmail.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
            return "local:$digest"
        }
    }
}

fun StoreSettings.identitySessionOrNull(): IdentitySession? {
    if (!isUserLoggedIn) return null
    val provider = identityProvider ?: return null
    return IdentitySession(
        provider = provider,
        uid = loggedInUid,
        username = loggedInUsername,
        email = loggedInEmail,
        role = loggedInRole
    ).normalized().takeIf { it.isUsable() }
}
