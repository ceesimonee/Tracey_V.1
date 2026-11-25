package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.tracey.ui.theme.*

@Composable
fun LoginScreen(
    initialRegisterMode: Boolean = false,
    onLogin: (String, String) -> String?,
    onRegister: (String, String) -> String?,
    onAuthSuccess: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(initialRegisterMode) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PersianMistBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Centered Grey Cat Mascot with clean spacing above the form card
            TraceyCatMascot(modifier = Modifier.size(104.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Form Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PureWhiteSurface,
                border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) "Get Started" else "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isRegisterMode) {
                            "Let's get started by creating your Tracey account below."
                        } else {
                            "Sign in to access your wallets and budget."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedFurGrey,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        placeholder = { Text("Username or Student ID", color = MutedFurGrey) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MutedFurGrey)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        placeholder = { Text("Password", color = MutedFurGrey) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MutedFurGrey)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = MutedFurGrey
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            placeholder = { Text("Confirm Password", color = MutedFurGrey) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MutedFurGrey)
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = SoftRoseDelete,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match."
                                } else {
                                    val err = onRegister(username, password)
                                    if (err != null) errorMessage = err else onAuthSuccess()
                                }
                            } else {
                                val err = onLogin(username, password)
                                if (err != null) errorMessage = err else onAuthSuccess()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CharcoalPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create account" else "Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MutedFurGrey)) {
                                append(if (isRegisterMode) "Already got an account? " else "New to Tracey? ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = DeepGraphite,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(if (isRegisterMode) "Log in here" else "Create an account")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .clickable {
                                isRegisterMode = !isRegisterMode
                                errorMessage = null
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}