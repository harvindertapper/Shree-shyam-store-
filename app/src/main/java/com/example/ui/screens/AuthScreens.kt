package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.*
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()

    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "सदस्य लॉगिन (User Login) 🔑",
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
                .background(WarmCreamBg)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Babaji / Store welcome emoji
                Text(
                    text = "🙏",
                    fontSize = 54.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = settings.shopName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "डिजिटल खाता बही का उपयोग करने के लिए लॉगिन करें।",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            color = ErrorRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Login Credentials Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Username or Email field
                        AppOutlinedTextField(
                            value = usernameOrEmail,
                            onValueChange = {
                                usernameOrEmail = it
                                errorMsg = null
                            },
                            label = "Username or Email *",
                            placeholder = "Enter username or email",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_or_email_input")
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMsg = null
                            },
                            label = {
                                Text(
                                    text = "Password *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Enter account password",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    // Login submit button
                    AppPrimaryButton(
                        text = "लॉगिन करें (Login) 🔓",
                        onClick = {
                            if (usernameOrEmail.isBlank()) {
                                errorMsg = "Please enter Username or Email!"
                            } else if (password.isBlank()) {
                                errorMsg = "Please enter Password!"
                            } else {
                                isLoading = true
                                viewModel.loginUser(
                                    usernameOrEmail = usernameOrEmail,
                                    password = password,
                                    onSuccess = {
                                        isLoading = false
                                        Toast.makeText(context, "लॉगिन सफल! (Login successful)", Toast.LENGTH_SHORT).show()
                                        if (settings.firstLaunchCompleted) {
                                            viewModel.navigateTo(Screen.Home)
                                        } else {
                                            viewModel.navigateTo(Screen.Setup)
                                        }
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMsg = err
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text option to go to sign up
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "खाता नहीं है? (No account?) ",
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "नया बनाएं (Register Here)",
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronDark,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.navigateTo(Screen.Register)
                                }
                                .testTag("go_to_register_button")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "पंजीकरण (New Registration) ✍️",
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
                .background(WarmCreamBg)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📝",
                    fontSize = 54.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "नया ओनर अकाउंट बनाएं",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNearBlack
                )

                Text(
                    text = "सुरक्षित लॉगिन के लिए क्रेडेंशियल दर्ज करें।",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            color = ErrorRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Register Credentials Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BorderStrong),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Username field
                        AppOutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMsg = null
                            },
                            label = "Username *",
                            placeholder = "Min 3 characters",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        // Email field
                        AppOutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMsg = null
                            },
                            label = "Email Address *",
                            placeholder = "e.g. name@example.com",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMsg = null
                            },
                            label = {
                                Text(
                                    text = "Password (Min 6 chars) *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Enter master password",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle password")
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input_register")
                        )

                        // Confirm Password field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMsg = null
                            },
                            label = {
                                Text(
                                    text = "Confirm Password *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Re-enter password",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LockClock,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    // Register submit button
                    AppPrimaryButton(
                        text = "पंजीकरण करें (Register Account) 📝",
                        onClick = {
                            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMsg = "All fields marked with * are required!"
                            } else if (username.trim().length < 3) {
                                errorMsg = "Username must be at least 3 characters!"
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                errorMsg = "Please enter a valid email address!"
                            } else if (password.length < 6) {
                                errorMsg = "Password must be at least 6 characters!"
                            } else if (password != confirmPassword) {
                                errorMsg = "Passwords do not match!"
                            } else {
                                isLoading = true
                                viewModel.registerUser(
                                    username = username,
                                    email = email,
                                    password = password,
                                    onSuccess = {
                                        isLoading = false
                                        Toast.makeText(context, "पंजीकरण सफल! (Registration successful)", Toast.LENGTH_SHORT).show()
                                        if (settings.firstLaunchCompleted) {
                                            viewModel.navigateTo(Screen.Home)
                                        } else {
                                            viewModel.navigateTo(Screen.Setup)
                                        }
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMsg = err
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_button")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text option to go to login
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "पहले से खाता है? (Have account?) ",
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "लॉगिन करें (Login Here)",
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronDark,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.navigateTo(Screen.Login)
                                }
                                .testTag("go_to_login_button")
                        )
                    }
                }
            }
        }
    }
}
