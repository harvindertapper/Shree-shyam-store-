package com.example.utils

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

object AuthManager {

    private const val TAG = "AuthManager"

    fun getFirebaseAuth(context: Context? = null): FirebaseAuth? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase is not initialized or configured: ${e.message}")
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Throwable) {
            null
        }

    val isUserAuthenticated: Boolean
        get() = currentUser != null

    /**
     * Sign In with Google via Android Credential Manager + Firebase Auth
     */
    suspend fun signInWithGoogle(
        context: Context,
        serverClientId: String? = null
    ): Result<FirebaseUser> {
        return try {
            val auth = getFirebaseAuth(context)
                ?: return Result.failure(Exception("Google Sign-In requires Firebase configuration."))
            val credentialManager = CredentialManager.create(context)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val resolvedClientId = serverClientId?.trim().takeUnless { it.isNullOrBlank() }
                ?: context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    .takeIf { it != 0 }
                    ?.let { context.getString(it).trim() }
                    ?.takeUnless { it.isBlank() }

            val googleIdOption: GetCredentialRequest = if (resolvedClientId != null) {
                val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(resolvedClientId)
                    .setAutoSelectEnabled(true)
                    .setNonce(hashedNonce)
                    .build()

                GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOptionBuilder)
                    .build()
            } else {
                return Result.failure(Exception("Google Sign-In is not configured. Add a Firebase web client ID."))
            }

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = googleIdOption
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("Firebase User is null after Google Auth")
                Result.success(user)
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign in was cancelled by user."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Credential error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send password / PIN reset link to the registered shop email address
     */
    suspend fun sendPasswordResetEmail(email: String, context: Context? = null): Result<Unit> {
        return try {
            val auth = getFirebaseAuth(context)
                ?: return Result.failure(Exception("Firebase Auth is not available in offline mode."))
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out user from Firebase and clear Credential Manager state
     */
    suspend fun signOut(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore credential cleanup error
        }
        try {
            getFirebaseAuth(context)?.signOut()
        } catch (e: Exception) {
            // Ignore offline sign out
        }
    }
}
