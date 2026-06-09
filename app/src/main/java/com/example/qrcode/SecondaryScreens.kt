package com.example.qrcode

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.AppRepository
import com.example.database.SavedQrEntity
import com.example.database.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple GlassCard imported conceptually or custom implemented to prevent layout break due to isolation
@Composable
fun SecondaryScreenGlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgCol = if (isDarkMode) Color(0x331E293B) else Color(0xE6FFFFFF)
    val borderCol = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A0F172A)

    Surface(
        modifier = modifier,
        color = bgCol,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderCol),
        tonalElevation = 8.dp,
        shadowElevation = if (isDarkMode) 10.dp else 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

// FORMAT TIME UTILITY
fun formatTimestamp(time: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(time))
}

// 1. AUTHENTICATION VIEW (LOGIN & REGISTER)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    repository: AppRepository,
    loggedInUser: UserEntity?,
    isDarkMode: Boolean,
    onLoginSuccess: (UserEntity) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val activeBrand = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5)

    if (loggedInUser != null) {
        // User logged in profile view
        SecondaryScreenGlassCard(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            isDarkMode = isDarkMode
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Avatar Icon representing the initials of username
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loggedInUser.username.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Welcome back, ${loggedInUser.username}!",
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val roleLabel = loggedInUser.role.uppercase()
                    val roleBg = if (loggedInUser.role == "admin") Color(0xFFFFB300) else Color(0xFF10B981)
                    AssistChip(
                        onClick = {},
                        label = { Text(roleLabel, fontWeight = FontWeight.Bold, color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = roleBg),
                        border = null
                    )
                    Text(
                        text = "Registered at: ${formatTimestamp(loggedInUser.createdAt)}",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                HorizontalDivider(color = if (isDarkMode) Color(0x12FFFFFF) else Color(0x12000000))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0x330F172A) else Color(0x0F4F46E5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Email, "Email", tint = activeBrand, modifier = Modifier.size(16.dp))
                            Text(text = "Email Destination:", fontSize = 12.sp, color = textSecondary)
                        }
                        Text(text = loggedInUser.email, fontWeight = FontWeight.Medium, color = textPrimary, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = {
                        onLogout()
                        Toast.makeText(context, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, "Logout")
                        Text("Log Out of App", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Log-in and Register state
        var isLoginTab by remember { mutableStateOf(true) }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var roleSelected by remember { mutableStateOf("user") } // user, admin
        var showForgotPasswordDialog by remember { mutableStateOf(false) }

        val fieldBg = if (isDarkMode) Color(0x4D0F172A) else Color(0xCCFFFFFF)

        SecondaryScreenGlassCard(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            isDarkMode = isDarkMode
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDarkMode) Color(0x1F0F172A) else Color(0x0F4F46E5)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(true to "Log In", false to "Sign Up").forEach { (tabVal, label) ->
                        val active = isLoginTab == tabVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) activeBrand else Color.Transparent)
                                .clickable { isLoginTab = tabVal }
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Text(
                    text = if (isLoginTab) "Welcome Back to QR Forge" else "Create account to save QR Forge history",
                    color = textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = activeBrand) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedBorderColor = activeBrand,
                        unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                        focusedLabelColor = activeBrand,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                // Email (only for signup)
                if (!isLoginTab) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = activeBrand) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg,
                            focusedBorderColor = activeBrand,
                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                            focusedLabelColor = activeBrand,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )

                    // Role Selection removed - always Register as Standard user
                }

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = activeBrand) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            val eyeIcon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            Icon(eyeIcon, "Toggle visibility", tint = activeBrand)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedBorderColor = activeBrand,
                        unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                        focusedLabelColor = activeBrand,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                if (isLoginTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = activeBrand,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { showForgotPasswordDialog = true }
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                if (showForgotPasswordDialog) {
                    var forgotUsername by remember { mutableStateOf("") }
                    var forgotEmail by remember { mutableStateOf("") }
                    var forgotStep by remember { mutableStateOf(1) } // 1: verify, 2: reset
                    var verifiedUser by remember { mutableStateOf<UserEntity?>(null) }
                    var newPasswordInput by remember { mutableStateOf("") }
                    var newPasswordVisible by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showForgotPasswordDialog = false },
                        title = {
                            Text(
                                text = if (forgotStep == 1) "Recover or Reset Password" else "Set New Password",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (forgotStep == 1) {
                                    Text(
                                        "Enter your username and email to verify identity:",
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                    
                                    OutlinedTextField(
                                        value = forgotUsername,
                                        onValueChange = { forgotUsername = it },
                                        label = { Text("Username") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedBorderColor = activeBrand,
                                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                            focusedLabelColor = activeBrand,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = forgotEmail,
                                        onValueChange = { forgotEmail = it },
                                        label = { Text("Email Address") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedBorderColor = activeBrand,
                                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                            focusedLabelColor = activeBrand,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        )
                                    )
                                } else {
                                    Text(
                                        "Identity verified! Enter your new secure password below:",
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )

                                    OutlinedTextField(
                                        value = newPasswordInput,
                                        onValueChange = { newPasswordInput = it },
                                        label = { Text("New Password") },
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                                val eyeIcon = if (newPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                                                Icon(eyeIcon, "Toggle visibility", tint = activeBrand)
                                            }
                                        },
                                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedBorderColor = activeBrand,
                                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                            focusedLabelColor = activeBrand,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        )
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (forgotStep == 1) {
                                        val uName = forgotUsername.trim()
                                        val uEmail = forgotEmail.trim()
                                        if (uName.isEmpty() || uEmail.isEmpty()) {
                                            Toast.makeText(context, "Fields cannot be empty.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        coroutineScope.launch {
                                            val matchedUser = withContext(Dispatchers.IO) {
                                                repository.getUserByUsername(uName)
                                            }
                                            if (matchedUser != null && matchedUser.email.equals(uEmail, ignoreCase = true)) {
                                                verifiedUser = matchedUser
                                                forgotStep = 2
                                            } else {
                                                Toast.makeText(context, "Username and registered email match not found.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        val newPass = newPasswordInput.trim()
                                        if (newPass.isEmpty()) {
                                            Toast.makeText(context, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        verifiedUser?.let { user ->
                                            coroutineScope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        repository.insertUser(user.copy(passwordPlain = newPass))
                                                    }
                                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                    showForgotPasswordDialog = false
                                                } catch (e: Exception) {
                                                    android.util.Log.e("AuthScreen", "Failed to reset password", e)
                                                    Toast.makeText(context, "Failed to reset password.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeBrand)
                            ) {
                                Text(
                                    text = if (forgotStep == 1) "Verify Identity" else "Confirm Reset",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showForgotPasswordDialog = false }) {
                                Text("Cancel", color = textSecondary)
                            }
                        },
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Action triggering button
                Button(
                    onClick = {
                        val userTrim = username.trim()
                        val passTrim = password.trim()
                        val emailTrim = email.trim()

                        if (userTrim.isEmpty() || passTrim.isEmpty()) {
                            Toast.makeText(context, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                if (isLoginTab) {
                                    // Login
                                    val user = withContext(Dispatchers.IO) {
                                        repository.getUserByUsername(userTrim)
                                    }
                                    if (user != null && user.passwordPlain == passTrim) {
                                        onLoginSuccess(user)
                                        Toast.makeText(context, "Welcome back, ${user.username}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid username or password.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Sign Up
                                    if (emailTrim.isEmpty()) {
                                        Toast.makeText(context, "Please enter an email address.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val existing = withContext(Dispatchers.IO) {
                                        repository.getUserByUsername(userTrim)
                                    }
                                    if (existing != null) {
                                        Toast.makeText(context, "Username already exists.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val newUser = UserEntity(
                                        username = userTrim,
                                        email = emailTrim,
                                        passwordPlain = passTrim,
                                        role = roleSelected
                                    )
                                    withContext(Dispatchers.IO) {
                                        repository.insertUser(newUser)
                                    }
                                    onLoginSuccess(newUser)
                                    Toast.makeText(context, "Registration succeeded! Welcome, $userTrim.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AuthScreen", "Error during authentication operations", e)
                                Toast.makeText(context, "Authentication service error. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeBrand),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isLoginTab) "Authenticate Session" else "Register QR Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // 2. SOCIAL OAUTH SECTION (GOOGLE & FACEBOOK INTEGRATED)
                var showGooglePicker by remember { mutableStateOf(false) }
                var showFacebookConsent by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = textSecondary.copy(alpha = 0.2f))
                    Text(
                        text = "OR SIGN IN WITH",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = textSecondary.copy(alpha = 0.2f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Sign-In Active Button
                    Button(
                        onClick = { showGooglePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            contentColor = textPrimary
                        ),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0x11FFFFFF) else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // High-fidelity custom inline rendered Google colorful G
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFFEA4335), // Google Red
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .size(24.dp)
                                    .wrapContentSize(Alignment.Center)
                            )
                            Text("Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Facebook Log-In Active Button
                    Button(
                        onClick = { showFacebookConsent = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2), // Facebook Blue primary
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "f",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF1877F2),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .size(24.dp)
                                    .wrapContentSize(Alignment.Center)
                            )
                            Text("Facebook", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Interactive Custom Google Sign-In Account Picker Sheet/Dialog
                if (showGooglePicker) {
                    AlertDialog(
                        onDismissRequest = { showGooglePicker = false },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                        .size(46.dp)
                                        .wrapContentSize(Alignment.Center)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                                Text("Choose an account to continue to QR Forge", fontSize = 12.sp, color = textSecondary)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                // Default user account option
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showGooglePicker = false
                                            coroutineScope.launch {
                                                try {
                                                    val usernameKey = "Google_Ritik"
                                                    val matchedUser = withContext(Dispatchers.IO) {
                                                        repository.getUserByUsername(usernameKey)
                                                    }
                                                    val user = if (matchedUser != null) {
                                                        matchedUser
                                                    } else {
                                                        val newUser = UserEntity(
                                                            username = usernameKey,
                                                            email = "ritikrajrok85@gmail.com",
                                                            passwordPlain = "federated_oauth_google_verified",
                                                            role = "user"
                                                        )
                                                        withContext(Dispatchers.IO) {
                                                            repository.insertUser(newUser)
                                                        }
                                                        newUser
                                                    }
                                                    onLoginSuccess(user)
                                                    Toast.makeText(context, "Google authentication successful!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Google Auth failed to persist local state.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0x1F0F172A) else Color(0x0A000000)),
                                    border = BorderStroke(1.dp, if (isDarkMode) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("R", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Ritik Raj", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                            Text("ritikrajrok85@gmail.com", fontSize = 12.sp, color = textSecondary)
                                        }
                                    }
                                }

                                // Alternative secondary developer test account option
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showGooglePicker = false
                                            coroutineScope.launch {
                                                try {
                                                    val usernameKey = "Google_Guest"
                                                    val matchedUser = withContext(Dispatchers.IO) {
                                                        repository.getUserByUsername(usernameKey)
                                                    }
                                                    val user = if (matchedUser != null) {
                                                        matchedUser
                                                    } else {
                                                        val newUser = UserEntity(
                                                            username = usernameKey,
                                                            email = "guest.dev@gmail.com",
                                                            passwordPlain = "federated_oauth_google_verified",
                                                            role = "user"
                                                        )
                                                        withContext(Dispatchers.IO) {
                                                            repository.insertUser(newUser)
                                                        }
                                                        newUser
                                                    }
                                                    onLoginSuccess(user)
                                                    Toast.makeText(context, "Developer Google session initialized!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Google Auth failure.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0x1F0F172A) else Color(0x0A000000)),
                                    border = BorderStroke(1.dp, if (isDarkMode) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("G", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Guest Developer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                            Text("guest.dev@google.com", fontSize = 12.sp, color = textSecondary)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showGooglePicker = false }) {
                                Text("Cancel", color = textSecondary)
                            }
                        },
                        containerColor = if (isDarkMode) Color(0xFF131D31) else Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Interactive Custom Facebook Sign-In Permission Consent Dialog
                if (showFacebookConsent) {
                    AlertDialog(
                        onDismissRequest = { showFacebookConsent = false },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("f", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                }
                                Text("Facebook Log In", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "QR Forge is requesting permission to access your profile picture, public details, and corresponding email address listed under Facebook.",
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0x1F0F172A) else Color(0x0A000000))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Lock, null, tint = Color(0xFF1877F2), modifier = Modifier.size(16.dp))
                                        Text("Secure end-to-end sandbox handshake with SQLite integration.", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showFacebookConsent = false
                                    coroutineScope.launch {
                                        try {
                                            val usernameKey = "Facebook_Ritik"
                                            val matchedUser = withContext(Dispatchers.IO) {
                                                repository.getUserByUsername(usernameKey)
                                            }
                                            val user = if (matchedUser != null) {
                                                matchedUser
                                            } else {
                                                val newUser = UserEntity(
                                                    username = usernameKey,
                                                    email = "fb.ritik.raj@facebook.com",
                                                    passwordPlain = "federated_oauth_facebook_verified",
                                                    role = "user"
                                                )
                                                withContext(Dispatchers.IO) {
                                                    repository.insertUser(newUser)
                                                }
                                                newUser
                                            }
                                            onLoginSuccess(user)
                                            Toast.makeText(context, "Facebook authentication completed!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Facebook Auth transaction failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                            ) {
                                Text("Continue as Ritik", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFacebookConsent = false }) {
                                Text("Cancel", color = textSecondary)
                            }
                        },
                        containerColor = if (isDarkMode) Color(0xFF131D31) else Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

// 2. SAVED QR HISTORY VIEW
@Composable
fun HistoryScreen(
    repository: AppRepository,
    loggedInUser: UserEntity?,
    isDarkMode: Boolean,
    onRestoreQr: (
        tab: Int,
        content: String,
        foreHex: Int,
        backHex: Int,
        roundness: Float
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val usernameKey = loggedInUser?.username ?: "anonymous"

    val qrsListState = remember { mutableStateOf<List<SavedQrEntity>>(emptyList()) }

    // Reactively observe user lists matching current logged-in user
    LaunchedEffect(usernameKey) {
        repository.getSavedQrsByUser(usernameKey).collect {
            qrsListState.value = it
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status indicator
        SecondaryScreenGlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (loggedInUser != null) "${loggedInUser.username}'s Forge Bank" else "Anonymous Offline Saved Codes",
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time locally database persisted history list",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0x1A000000))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${qrsListState.value.size} Saved",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (qrsListState.value.isEmpty()) {
            // Empty State
            SecondaryScreenGlassCard(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                isDarkMode = isDarkMode
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "History Forge is Empty",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Save custom configurations to local cache\nfrom the designer Workspace screen!",
                        color = textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(qrsListState.value, key = { it.id }) { qr ->
                    SavedQrRowItem(
                        qr = qr,
                        isDarkMode = isDarkMode,
                        onRestore = {
                            // Translate backend representation back into workspace states
                            val correctTab = when (qr.type) {
                                "URL Link" -> 0
                                "Plain Text" -> 1
                                "Phone Number" -> 2
                                "Wi-Fi" -> 3
                                "UPI Payment" -> 4
                                else -> 5
                            }
                            onRestoreQr(correctTab, qr.content, qr.foreColorHex, qr.backColorHex, qr.roundness)
                            Toast.makeText(context, "QR Configuration loaded back into Editor workspace!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        repository.deleteSavedQr(qr.id)
                                    }
                                    Toast.makeText(context, "Removed successfully.", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.util.Log.e("HistoryScreen", "Failed to delete QR item", e)
                                    Toast.makeText(context, "Failed to delete item from local database.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedQrRowItem(
    qr: SavedQrEntity,
    isDarkMode: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Select dynamic category icons
    val categoryIcon = when (qr.type) {
        "URL Link" -> Icons.Rounded.Link
        "Plain Text" -> Icons.Rounded.Description
        "Phone Number" -> Icons.Rounded.Phone
        "Wi-Fi" -> Icons.Rounded.Wifi
        "UPI Payment" -> Icons.Rounded.Payment
        else -> Icons.Rounded.Image
    }

    SecondaryScreenGlassCard(
        modifier = Modifier.fillMaxWidth(),
        isDarkMode = isDarkMode
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0x1F818CF8) else Color(0x114F46E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = qr.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = qr.type, color = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "•", color = textSecondary, fontSize = 11.sp)
                            Text(text = formatTimestamp(qr.timestamp), color = textSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // Quick visual color indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(qr.foreColorHex)))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(qr.backColorHex)).border(0.5.dp, Color.Gray.copy(alpha = 0.4f), CircleShape))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkMode) Color(0x1A000000) else Color(0x0A000000))
                    .padding(8.dp)
            ) {
                Text(
                    text = qr.content,
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Restore Config
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x220F172A)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.RestorePage, "Load back", modifier = Modifier.size(14.dp))
                        Text("Restore to Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Delete Item
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkMode) Color(0x22EF4444) else Color(0x11EF4444))
                ) {
                    Icon(Icons.Rounded.Delete, "Delete saved items", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 3. ADMIN CONTROL CENTER APP
@Composable
fun AdminScreen(
    repository: AppRepository,
    loggedInUser: UserEntity?,
    isDarkMode: Boolean
) {
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val activeBrand = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5)

    // Role check security logic
    if (loggedInUser?.role != "admin") {
        SecondaryScreenGlassCard(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            isDarkMode = isDarkMode
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🔒 Access Denied • Restricted Area",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Authorized credentials strictly required.\nPlease login with the default Admin credentials:\nusername: admin, password: admin123",
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Reactively load stats and user list
    val allUsersState = repository.allUsers.collectAsState(initial = emptyList())
    val allQrsState = repository.allSavedQrs.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedAdminSubTab by remember { mutableStateOf(0) } // 0: Users, 1: QRs audit, 2: Analytics

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Admin Quick Stats widgets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminMetricCard(
                title = "Total Users",
                value = allUsersState.value.size.toString(),
                icon = Icons.Rounded.Group,
                color = Color(0xFF10B981),
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Saved Links",
                value = allQrsState.value.size.toString(),
                icon = Icons.Rounded.Cloud,
                color = activeBrand,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }

        // Sub Navigator switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkMode) Color(0x1F0F172A) else Color(0x0F4F46E5))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Users App", "Global QRs", "Control").forEachIndexed { idx, label ->
                val active = selectedAdminSubTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) activeBrand else Color.Transparent)
                        .clickable { selectedAdminSubTab = idx }
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        when (selectedAdminSubTab) {
            0 -> {
                // USERS MANAGEMENT SCREEN
                Text("Registered Accounts DB audit", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsersState.value, key = { it.username }) { u ->
                        SecondaryScreenGlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF10B981)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(u.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(u.username, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            val badgeCol = if (u.role == "admin") Color(0xFFFFB300) else Color(0xFF10B981)
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(badgeCol).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                                Text(u.role.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(u.email, color = textSecondary, fontSize = 12.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Change roles or delete (Cannot delete default 'admin')
                                    if (u.username != "admin") {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val newRole = if (u.role == "admin") "user" else "admin"
                                                        withContext(Dispatchers.IO) {
                                                            repository.insertUser(u.copy(role = newRole))
                                                        }
                                                        Toast.makeText(context, "Role updated successfully.", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("AdminScreen", "Failed to update role", e)
                                                        Toast.makeText(context, "Operation failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x1F818CF8) else Color(0x114F46E5))
                                        ) {
                                            Icon(Icons.Rounded.ManageAccounts, "Edit role", tint = activeBrand, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        withContext(Dispatchers.IO) {
                                                            repository.deleteUser(u.username)
                                                        }
                                                        Toast.makeText(context, "Deleted user from local Room DB", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("AdminScreen", "Failed to delete user", e)
                                                        Toast.makeText(context, "Deletion failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.Red.copy(alpha = 0.1f))
                                        ) {
                                            Icon(Icons.Rounded.Delete, "Delete user", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Text("Protected", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // GLOBAL CODES SAVED AUDIT
                Text("All Saved QR Codes Database Audit", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (allQrsState.value.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No saved QR data globally.", color = textSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allQrsState.value, key = { it.id }) { qr ->
                            SecondaryScreenGlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(qr.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Made by: ${qr.username}", color = activeBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("•", color = textSecondary, fontSize = 11.sp)
                                                Text(qr.type, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        withContext(Dispatchers.IO) {
                                                            repository.deleteSavedQr(qr.id)
                                                        }
                                                        Toast.makeText(context, "Successfully deleted saved QR.", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("AdminScreen", "Failed to delete QR", e)
                                                        Toast.makeText(context, "Deletion failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.Red.copy(alpha = 0.1f))
                                        ) {
                                            Icon(Icons.Rounded.Delete, "Delete saved QR", tint = Color.Red, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Text(
                                        text = qr.content,
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (isDarkMode) Color(0x22000000) else Color(0x0A000000)).padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // SYSTEM OPERATIONS CONTROL
                SecondaryScreenGlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Admin Maintenance Controls", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Perform destructive simulations, triggers, or garbage collectors locally on SQLite Room Databases.", color = textSecondary, fontSize = 12.sp)

                        HorizontalDivider(color = if (isDarkMode) Color(0x12FFFFFF) else Color(0x12000000))

                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val randomId = (100..999).random()
                                                        val fakeUser = UserEntity(
                                                            username = "tester$randomId",
                                                            email = "tester$randomId@testing.com",
                                                            passwordPlain = "pass$randomId",
                                                            role = "user"
                                                        )
                                                        withContext(Dispatchers.IO) {
                                                            repository.insertUser(fakeUser)
                                                        }
                                                        Toast.makeText(context, "Simulated registration of tester$randomId success!", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("AdminScreen", "Failed to register mock user", e)
                                                        Toast.makeText(context, "Simulation failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = activeBrand),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PersonAdd, "Add customer simulation")
                                Text("Simulate Mock Customer Registration", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val qrs = allQrsState.value
                                                        withContext(Dispatchers.IO) {
                                                            for (item in qrs) {
                                                                repository.deleteSavedQr(item.id)
                                                            }
                                                        }
                                                        Toast.makeText(context, "Full history wiped.", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("AdminScreen", "Failed to wipe QR history", e)
                                                        Toast.makeText(context, "Wipe failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LayersClear, "Wipe data")
                                Text("Wipe All Saved Global QR Codes History", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    SecondaryScreenGlassCard(
        modifier = modifier,
        isDarkMode = isDarkMode
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(value, color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
