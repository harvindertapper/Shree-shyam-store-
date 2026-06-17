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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    val enterUsernameOrEmailError = stringResource(R.string.error_enter_username_or_email)
    val enterPasswordError = stringResource(R.string.error_enter_password)
    val loginSuccessMessage = stringResource(R.string.login_success)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
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
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SaffronDark,
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
                    text = stringResource(R.string.login_intro),
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
                            label = stringResource(R.string.login_username_or_email_label),
                            placeholder = stringResource(R.string.login_username_or_email_placeholder),
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
                                    text = stringResource(R.string.password_label),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.login_password_placeholder),
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
                                    Icon(imageVector = image, contentDescription = stringResource(R.string.content_description_toggle_password_visibility))
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
                        text = stringResource(R.string.login_button),
                        onClick = {
                            if (usernameOrEmail.isBlank()) {
                                errorMsg = enterUsernameOrEmailError
                            } else if (password.isBlank()) {
                                errorMsg = enterPasswordError
                            } else {
                                isLoading = true
                                viewModel.loginUser(
                                    usernameOrEmail = usernameOrEmail,
                                    password = password,
                                    onSuccess = {
                                        isLoading = false
                                        Toast.makeText(context, loginSuccessMessage, Toast.LENGTH_SHORT).show()
                                        if (settings.firstLaunchCompleted) {
                                            viewModel.navigateTo(Screen.Home)
                                        } else {
                                            viewModel.navigateTo(Screen.Setup)
                                        }
                                    },
                                    onError = { errorResId ->
                                        isLoading = false
                                        errorMsg = context.getString(errorResId)
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
                            text = stringResource(R.string.login_no_account) + " ",
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray,
                            fontSize = 15.sp
                        )
                        Text(
                            text = stringResource(R.string.login_register_here),
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
    val requiredFieldsError = stringResource(R.string.error_required_fields)
    val usernameMinLengthError = stringResource(R.string.error_username_min_length)
    val validEmailError = stringResource(R.string.error_valid_email)
    val passwordMinLengthError = stringResource(R.string.error_password_min_length)
    val passwordsDoNotMatchError = stringResource(R.string.error_passwords_do_not_match)
    val registerSuccessMessage = stringResource(R.string.register_success)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.register_title),
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
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SaffronDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.register_heading),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNearBlack
                )

                Text(
                    text = stringResource(R.string.register_intro),
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
                            label = stringResource(R.string.username_label),
                            placeholder = stringResource(R.string.username_placeholder),
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
                            label = stringResource(R.string.email_label),
                            placeholder = stringResource(R.string.email_placeholder),
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
                                    text = stringResource(R.string.register_password_label),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.register_password_placeholder),
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
                                    Icon(imageVector = image, contentDescription = stringResource(R.string.content_description_toggle_password))
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
                                    text = stringResource(R.string.confirm_password_label),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.confirm_password_placeholder),
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
                        text = stringResource(R.string.register_button),
                        onClick = {
                            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMsg = requiredFieldsError
                            } else if (username.trim().length < 3) {
                                errorMsg = usernameMinLengthError
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                errorMsg = validEmailError
                            } else if (password.length < 6) {
                                errorMsg = passwordMinLengthError
                            } else if (password != confirmPassword) {
                                errorMsg = passwordsDoNotMatchError
                            } else {
                                isLoading = true
                                viewModel.registerUser(
                                    username = username,
                                    email = email,
                                    password = password,
                                    onSuccess = {
                                        isLoading = false
                                        Toast.makeText(context, registerSuccessMessage, Toast.LENGTH_SHORT).show()
                                        if (settings.firstLaunchCompleted) {
                                            viewModel.navigateTo(Screen.Home)
                                        } else {
                                            viewModel.navigateTo(Screen.Setup)
                                        }
                                    },
                                    onError = { errorResId ->
                                        isLoading = false
                                        errorMsg = context.getString(errorResId)
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
                            text = stringResource(R.string.register_have_account) + " ",
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray,
                            fontSize = 15.sp
                        )
                        Text(
                            text = stringResource(R.string.register_login_here),
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
