package com.harrylabs.shreeshyamstore.ui.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.ui.theme.*
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    var visible by remember { mutableStateOf(false) }

    // Media player state safe release
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    // Look for assets dynamically to avoid compilation failures if missing
    val drawableId = remember {
        context.resources.getIdentifier("khatu_shyam_baba", "drawable", context.packageName)
    }
    val audioId = remember {
        context.resources.getIdentifier("jai_shree_shyam_chant", "raw", context.packageName)
    }

    LaunchedEffect(Unit) {
        visible = true
        
        // Safely play chant if enabled and file is placed
        if (settings.welcomeChantEnabled && audioId != 0) {
            try {
                mediaPlayer = MediaPlayer.create(context, audioId)?.apply {
                    isLooping = false
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Wait 3 seconds then navigate
        delay(3000)
        
        // Release audio safely
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (e: Exception) {
                // ignore
            }
            try {
                it.release()
            } catch (e: Exception) {
                // ignore
            }
            mediaPlayer = null
        }

        viewModel.checkSessionAndRoute(
            context = context,
            onSuccess = {},
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Clean up if we skip or leave earlier
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                } catch (e: Exception) {
                    // silent
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFDF7), // Pure light cream
                        Color(0xFFFFEDD5), // Soft saffron yellow
                        Color(0xFFFFD6A5)  // Robust warm saffron
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Babaji image block
                if (drawableId != 0) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = drawableId),
                            contentDescription = stringResource(R.string.app_name),
                            tint = Color.Unspecified,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Fallback visual
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SaffronPrimary)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = stringResource(R.string.content_description_store),
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.welcome_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaffronDark, // Warm dark saffron red
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = settings.shopName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNearBlack, // Sharp high contrast dark
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Continue button
                Button(
                    onClick = {
                        mediaPlayer?.let {
                            try {
                                if (it.isPlaying) {
                                    it.stop()
                                }
                                it.release()
                            } catch (e: Exception) { /* ... */ }
                            mediaPlayer = null
                        }
                        viewModel.checkSessionAndRoute(
                            context = context,
                            onSuccess = {},
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8F)
                        .height(56.dp)
                        .testTag("skip_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_continue),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.content_description_next),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
