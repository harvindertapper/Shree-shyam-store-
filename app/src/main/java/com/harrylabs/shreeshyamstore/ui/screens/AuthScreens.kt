package com.harrylabs.shreeshyamstore.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.ui.theme.*
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel
import com.harrylabs.shreeshyamstore.viewmodel.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isCredentialRequestInProgress by remember { mutableStateOf(false) }

    val khatuImageId = remember {
        context.resources.getIdentifier("khatu_shyam_baba", "drawable", context.packageName)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.login_title),
                        fontWeight = FontWeight.Black,
                        color = SaffronDark
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFDF7), // Pure light cream
                            Color(0xFFFFEDD5), // Soft saffron yellow
                            Color(0xFFFFD6A5)  // Saffron
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Babaji or Fallback visual
                if (khatuImageId != 0) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = khatuImageId),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SaffronPrimary)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Text(
                    text = settings.shopName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.login_intro),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                // Login card containing Google button
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (authState is AuthState.Loading || isCredentialRequestInProgress) {
                            CircularProgressIndicator(
                                color = SaffronPrimary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("login_progress")
                            )
                            Text(
                                text = stringResource(R.string.login_intro), // Loading state hint
                                fontWeight = FontWeight.Bold,
                                color = TextMediumGray,
                                fontSize = 14.sp
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (isCredentialRequestInProgress) return@Button
                                    coroutineScope.launch {
                                        isCredentialRequestInProgress = true
                                        try {
                                            val credentialManager = CredentialManager.create(context)
                                            val webClientId = context.getString(R.string.default_web_client_id)

                                            val idToken = requestGoogleIdToken(
                                                context = context,
                                                credentialManager = credentialManager,
                                                webClientId = webClientId
                                            )

                                            viewModel.signInWithGoogle(
                                                idToken = idToken,
                                                context = context,
                                                onSuccess = {
                                                    Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { error ->
                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } catch (e: GetCredentialException) {
                                            val errorMsg = when (e) {
                                                is NoCredentialException -> context.getString(R.string.error_google_sign_in_cancelled)
                                                else -> context.getString(R.string.error_google_sign_in_failed, e.localizedMessage)
                                            }
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        } catch (e: GoogleIdTokenParsingException) {
                                            Toast.makeText(context, context.getString(R.string.error_google_token_parse_failed), Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.error_sign_in_general, e.localizedMessage), Toast.LENGTH_LONG).show()
                                        } finally {
                                            isCredentialRequestInProgress = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = BorderStroke(1.5.dp, BorderStrong),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("google_login_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Google Icon (painterResource uses launcher_background or custom drawable if exists)
                                    val googleLogoId = remember {
                                        context.resources.getIdentifier("ic_google_logo", "drawable", context.packageName)
                                    }
                                    if (googleLogoId != 0) {
                                        Icon(
                                            painter = painterResource(id = googleLogoId),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.login_with_google),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNearBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun requestGoogleIdToken(
    context: Context,
    credentialManager: CredentialManager,
    webClientId: String
): String {
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    val result = credentialManager.getCredential(context, request)
    val credential = result.credential

    if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw IllegalStateException(context.getString(R.string.error_unexpected_credential_type))
    }

    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
