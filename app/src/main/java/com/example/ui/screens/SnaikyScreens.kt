package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.data.repository.RoomConfig
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AbuseReportEntity
import com.example.data.database.ChatEntity
import com.example.data.database.MatchHistoryEntity
import com.example.data.database.UserEntity
import com.example.ui.game.*
import com.example.viewmodel.SnaikyScreen
import com.example.viewmodel.SnaikyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- 1. Cyber Custom Color Definitions ---
object CyberColor {
    val DarkCanvas = Color(0xFF03070C)      // Extreme Deep Cyber Slate Black
    val SurfaceDark = Color(0xFF0D1520)     // Premium Hex Panel background
    val SurfaceLight = Color(0xFF162535)    // Lighter highlight panel
    val NeonGreen = Color(0xFF39FF14)       // Classic Laser Green
    val DarkNeonGreen = Color(0xFF1B6A0E)   // Low-glow Green accents
    val NeonCyan = Color(0xFF00FFCC)        // Shield / Electric Cyan
    val NeonPink = Color(0xFFFF007F)        // Hot Cyber Pink
    val NeonYellow = Color(0xFFFFD700)      // Gold coin / Trophy Orange-Gold
    val DarkMuted = Color(0xFF4A5A6A)       // Sleek grey text
    val GlitchBlue = Color(0xFF00A2FF)
    val TransparentGreen = Color(0x1539FF14)
    val TransparentPink = Color(0x15FF007F)
}

// Helper formatting function
private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Composable
fun SnaikyAppContent(viewModel: SnaikyViewModel) {
    val currentScreen = viewModel.currentScreen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberColor.DarkCanvas)
    ) {
        // Futuristic Star/Grid Background Behind Everything
        CyberGridDecoration()

        // Persistent High Fidelity Top Header navigation for active sessions
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentScreen != SnaikyScreen.Landing && currentScreen != SnaikyScreen.Auth) {
                CyberNavBarHeader(viewModel = viewModel)
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        SnaikyScreen.Landing -> LandingPageScreen(viewModel)
                        SnaikyScreen.Auth -> AuthScreen(viewModel)
                        SnaikyScreen.HomeLobby -> HomeLobbyScreen(viewModel)
                        SnaikyScreen.UserProfile -> UserProfileScreen(viewModel)
                        SnaikyScreen.RoomLobby -> RoomLobbyScreen(viewModel)
                        SnaikyScreen.GamePlay -> GameScreen(viewModel)
                        SnaikyScreen.Leaderboard -> LeaderboardScreen(viewModel)
                        SnaikyScreen.AdminPanel -> AdminDashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}

// --- 2. Cyber Style Background Decorator ---
@Composable
fun CyberGridDecoration() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.08f)) {
        val stepX = 40.dp.toPx()
        val stepY = 40.dp.toPx()
        
        // Draw grid vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = CyberColor.NeonGreen,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += stepX
        }
        
        // Draw grid horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = CyberColor.NeonGreen,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += stepY
        }
    }
}

// --- 3. Persistent Header NavBar ---
@Composable
fun CyberNavBarHeader(viewModel: SnaikyViewModel) {
    val user = viewModel.currentUser
    val isReady = user != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(0.dp))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(CyberColor.NeonGreen.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            ),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Glowing Application Name Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(SnaikyScreen.HomeLobby) }
                    .testTag("nav_home_logo")
            ) {
                Icon(
                    imageVector = Icons.Default.Cyclone,
                    contentDescription = "Snaiky Logo",
                    tint = CyberColor.NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SNAIKY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // User Info Badge
            if (isReady) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Link
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberColor.SurfaceLight)
                            .clickable { viewModel.navigateTo(SnaikyScreen.UserProfile) }
                            .padding(8.dp)
                            .testTag("nav_profile_badge")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(getSkinColor(user!!.avatar))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = user.username,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (user.role == "Admin") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Admin Token",
                                tint = CyberColor.NeonPink,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Log Out Action
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberColor.SurfaceLight)
                            .testTag("nav_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- 4. Landing Page Screen ---
@Composable
fun LandingPageScreen(viewModel: SnaikyViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Large Premium Title with glitch glows
        Text(
            text = "SNAIKY",
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = CyberColor.NeonGreen,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            modifier = Modifier.shadow(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "“Play, Compete, Grow.”",
            fontSize = 18.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Giant Arena Play Button
        Button(
            onClick = {
                if (viewModel.currentUser == null) {
                    viewModel.navigateTo(SnaikyScreen.Auth)
                } else {
                    viewModel.navigateTo(SnaikyScreen.HomeLobby)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp),
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(12.dp))
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .testTag("play_now_button")
        ) {
            Text(
                text = "INITIALIZE ARENA",
                color = CyberColor.DarkCanvas,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Feature cards
        Text(
            text = "TACTICAL CAPABILITIES",
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.alpha(0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Single list features with custom borders
        CapabilityCard(
            title = "Dynamic Multiplayer Grid",
            description = "Compete alongside 4 smart local bots representing live competitors globally. Features head collisions where the LARGEST snake wins!",
            icon = Icons.Default.Hub,
            color = CyberColor.NeonGreen
        )
        Spacer(modifier = Modifier.height(12.dp))

        CapabilityCard(
            title = "Gemini AI Opponents Chat",
            description = "Simulates authentic reactive player dialogue. Competitors mock, praise and celebrate dynamically during the countdown and matches based on gameplay events!",
            icon = Icons.Default.Psychology,
            color = CyberColor.NeonCyan
        )
        Spacer(modifier = Modifier.height(12.dp))

        CapabilityCard(
            title = "Cyber Skin System & Power-Ups",
            description = "Customize dynamic colors matching skins like Hex Pulse, Matrix Rain, or Laser Grid. Collect speed modifiers, invincible shields, and dimensional passes!",
            icon = Icons.Default.Palette,
            color = CyberColor.NeonPink
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Testimony Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "“The real-time banter of bots makes you forget you're playing alone. The glitchy style is absolutely spectacular!”",
                    color = Color.White,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- X-Rider, Grand Champion Rank #3",
                    color = CyberColor.NeonPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "v1.4.2 PREMIUM BUILD. DEPLOYED ON RAILWAY CERTIFICATE",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = CyberColor.DarkMuted
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
fun CapabilityCard(title: String, description: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- 5. Auth / Registration / Password Reset Screen ---
@Composable
fun AuthScreen(viewModel: SnaikyViewModel) {
    val isRegister = viewModel.authIsRegister
    val error = viewModel.authError
    val otpSent = viewModel.otpSent
    val otpSubmitted = viewModel.otpSubmitted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegister) "REGISTER CREDENTIALS" else "COGNITIVE LOGIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter secure tokens to connect to matching lobby",
            fontSize = 12.sp,
            color = CyberColor.DarkMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberColor.NeonPink.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        border = BorderStroke(1.dp, CyberColor.NeonPink)
                    ) {
                        Text(
                            text = error,
                            color = CyberColor.NeonPink,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!otpSent) {
                    OutlinedTextField(
                        value = viewModel.authEmail,
                        onValueChange = { viewModel.authEmail = it },
                        label = { Text("Cyber Address (Email)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CyberColor.NeonGreen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CyberColor.SurfaceLight,
                            focusedBorderColor = CyberColor.NeonGreen,
                            focusedLabelColor = CyberColor.NeonGreen
                        )
                    )

                    if (isRegister) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.authUsername,
                            onValueChange = { viewModel.authUsername = it },
                            label = { Text("Display Handle (Username)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_username_input"),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberColor.NeonGreen) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = CyberColor.SurfaceLight,
                                focusedBorderColor = CyberColor.NeonGreen,
                                focusedLabelColor = CyberColor.NeonGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.authPassword,
                        onValueChange = { viewModel.authPassword = it },
                        label = { Text("Decryption Token (Password)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CyberColor.NeonGreen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CyberColor.SurfaceLight,
                            focusedBorderColor = CyberColor.NeonGreen,
                            focusedLabelColor = CyberColor.NeonGreen
                        )
                    )

                    if (isRegister) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.authConfirmPassword,
                            onValueChange = { viewModel.authConfirmPassword = it },
                            label = { Text("Confirm Decryption Token") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input"),
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = CyberColor.NeonGreen) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = CyberColor.SurfaceLight,
                                focusedBorderColor = CyberColor.NeonGreen,
                                focusedLabelColor = CyberColor.NeonGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reset password options
                    if (!isRegister) {
                        Text(
                            text = "Forgot password? Send Otp Reset Link",
                            fontWeight = FontWeight.Bold,
                            color = CyberColor.NeonCyan,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { viewModel.resetPasswordSimulation() }
                                .padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.submitAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (isRegister) "REGISTER PROFILE" else "DECRYPT SESSION",
                            color = CyberColor.DarkCanvas,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    // OTP Verification flow
                    Text(
                        text = "A secure verification code has been dispatched to ${viewModel.authEmail}.",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.otpCode,
                        onValueChange = { viewModel.otpCode = it },
                        label = { Text("Digital OTP Token") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CyberColor.SurfaceLight,
                            focusedBorderColor = CyberColor.NeonCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.submitOtpAndReset() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("VERIFY & ATTAIN ACCESS", color = CyberColor.DarkCanvas, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Switch and Social login inside single container
        Text(
            text = if (isRegister) "Already registered? Login Here" else "Need a Profile? Register Here",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier
                .clickable { viewModel.authIsRegister = !viewModel.authIsRegister }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, CyberColor.SurfaceLight, RoundedCornerShape(8.dp))
                .clickable { viewModel.launchGoogleLogin() }
                .background(CyberColor.SurfaceDark)
                .padding(12.dp)
                .testTag("google_login_button"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null,
                tint = CyberColor.NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SIGN IN INTEGRATION (GOOGLE)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { viewModel.navigateTo(SnaikyScreen.Landing) }) {
            Text("← ARCHIVE PORTAL (LANDING PAGE)", color = CyberColor.NeonGreen, fontSize = 12.sp)
        }
    }
}

// --- 6. Home Lobby / Rooms Matching Screen ---
@Composable
fun HomeLobbyScreen(viewModel: SnaikyViewModel) {
    val user = viewModel.currentUser ?: return
    val rooms = viewModel.activeRooms
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcoming card with active stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberColor.NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getSkinColor(user.avatar))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "COMMANDER: ${user.username}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LOC: ${user.country} | SYSTEM LEVEL: ${if (user.role == "Admin") "CORE ADMIN" else "STANDARD OPERATOR"}",
                            fontSize = 12.sp,
                            color = CyberColor.NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberColor.SurfaceLight)
                Spacer(modifier = Modifier.height(12.dp))

                // Stats row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatNode("TOTAL MATCHES", user.totalGames.toString())
                    StatNode("WINS", user.wins.toString())
                    StatNode("LOSSES", user.losses.toString())
                    val rate = if (user.totalGames > 0) "${(user.wins * 100) / user.totalGames}%" else "0%"
                    StatNode("WIN RATE", rate)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "“${user.bio}”",
                    color = Color.White.copy(alpha = 0.6f),
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Action Buttons Dashboard Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.navigateTo(SnaikyScreen.Leaderboard) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceDark),
                border = BorderStroke(1.dp, CyberColor.NeonYellow),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CyberColor.NeonYellow)
                Spacer(modifier = Modifier.width(6.dp))
                Text("LEADERBOARD", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { viewModel.navigateTo(SnaikyScreen.UserProfile) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceDark),
                border = BorderStroke(1.dp, CyberColor.NeonCyan),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = CyberColor.NeonCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("SKINS / PROFILE", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            if (user.role == "Admin") {
                Button(
                    onClick = { viewModel.navigateTo(SnaikyScreen.AdminPanel) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceDark),
                    border = BorderStroke(1.dp, CyberColor.NeonPink),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CyberColor.NeonPink)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADMIN DESK", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lobby Matching Rooms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE MATCHMAKING ROOMS",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CyberColor.NeonGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create matching room", tint = CyberColor.DarkCanvas)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Rooms Grid List
        if (rooms.isEmpty()) {
            Text(
                "No live matching rooms active. Add a custom room to start!",
                color = CyberColor.DarkMuted,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )
        } else {
            for (room in rooms) {
                RoomCard(room = room, onJoin = { viewModel.selectRoomToJoin(room) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Global Chat integration
        Text(
            text = "GLOBAL TERMINAL CHAT",
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            val chats by viewModel.globalChats.collectAsState()
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false
                    ) {
                        items(chats) { chat ->
                            GlobalChatNode(chat, onDelete = {
                                if (user.role == "Admin") {
                                    viewModel.adminDeleteChat(chat.id)
                                }
                            }, isAdmin = user.role == "Admin")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = viewModel.chatMessageInput,
                        onValueChange = { viewModel.chatMessageInput = it },
                        placeholder = { Text("Send global signal...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_chat_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CyberColor.SurfaceLight,
                            focusedBorderColor = CyberColor.NeonGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.sendChatMessage(isGlobal = true) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyberColor.NeonGreen)
                            .testTag("global_chat_send")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Message", tint = CyberColor.DarkCanvas)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }

    // CREATE ROOM CUSTOM DIALOG
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CyberColor.SurfaceDark,
            title = {
                Text(
                    "CONSTRUCT CUSTOM ROOM",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.customRoomName,
                        onValueChange = { viewModel.customRoomName = it },
                        label = { Text("Network Room Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberColor.NeonGreen,
                            unfocusedBorderColor = CyberColor.SurfaceLight
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Room Access Scope", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.customRoomType = "PUBLIC" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.customRoomType == "PUBLIC") CyberColor.NeonGreen else CyberColor.SurfaceLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "PUBLIC",
                                color = if (viewModel.customRoomType == "PUBLIC") CyberColor.DarkCanvas else Color.White
                            )
                        }

                        Button(
                            onClick = { viewModel.customRoomType = "PRIVATE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.customRoomType == "PRIVATE") CyberColor.NeonCyan else CyberColor.SurfaceLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "PRIVATE",
                                color = if (viewModel.customRoomType == "PRIVATE") CyberColor.DarkCanvas else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createRoom()
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen)
                ) {
                    Text("DEPLOY ARENA", color = CyberColor.DarkCanvas, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("ABORT", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun StatNode(label: String, valStr: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = CyberColor.DarkMuted, fontFamily = FontFamily.Monospace)
        Text(
            text = valStr,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RoomCard(room: RoomConfig, onJoin: () -> Unit) {
    val typeColor = if (room.type == "PUBLIC") CyberColor.NeonGreen else CyberColor.NeonCyan

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() },
        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
        border = BorderStroke(1.dp, CyberColor.SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.name,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = room.type,
                        fontSize = 9.sp,
                        color = typeColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .border(1.dp, typeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "INVITE SIGNAL: ${room.code}",
                    fontSize = 11.sp,
                    color = CyberColor.DarkMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceLight),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "CONNECT", fontSize = 11.sp, color = CyberColor.NeonGreen, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun GlobalChatNode(chat: ChatEntity, onDelete: () -> Unit, isAdmin: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(getSkinColor(chat.senderAvatar))
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row {
                    Text(
                        text = "${chat.senderName}: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CyberColor.NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = formatTime(chat.timestamp),
                        fontSize = 9.sp,
                        color = CyberColor.DarkMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = chat.message,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
        if (isAdmin) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete chat", tint = CyberColor.NeonPink, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// --- 7. Matching Room Lobby Countdown & Chat Screen ---
@Composable
fun RoomLobbyScreen(viewModel: SnaikyViewModel) {
    val room = viewModel.selectedRoom ?: return
    val players = viewModel.lobbyPlayers
    val chats = viewModel.roomChats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ROOM: ${room.name} (${room.type})",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Status: ESTABLISHING NETWORK CHANNELS | Code: ${room.code}",
            fontSize = 11.sp,
            color = CyberColor.NeonGreen,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Players roster list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CONNECTED USERS (${players.size}/4)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                for (player in players) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(getSkinColor(player.avatar))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = player.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (player.isYou) "READY" else "CONNECTED",
                            color = if (player.isYou) CyberColor.NeonGreen else CyberColor.NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lobby chat Box
        Text(
            "ROOM MATCH CHAT (AI BANTER DISPATCH)",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(chats) { chat ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "${chat.senderName}: ",
                                    color = if (chat.senderName == "SYSTEM") CyberColor.NeonCyan else CyberColor.NeonPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = chat.message,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = viewModel.chatMessageInput,
                        onValueChange = { viewModel.chatMessageInput = it },
                        placeholder = { Text("Signals to lobby players...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberColor.NeonPink,
                            unfocusedBorderColor = CyberColor.SurfaceLight
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.sendChatMessage(isGlobal = false) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyberColor.NeonPink)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Message", tint = CyberColor.DarkCanvas)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.navigateTo(SnaikyScreen.HomeLobby) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceLight),
                modifier = Modifier.weight(1f)
            ) {
                Text("DISCONNECT", color = Color.White)
            }

            Button(
                onClick = { viewModel.startGameMatch() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen),
                modifier = Modifier.weight(1.5f),
                enabled = players.size >= 2
            ) {
                Text("DECOMPRENS & LAUNCH!", color = CyberColor.DarkCanvas, fontWeight = FontWeight.Black)
            }
        }
    }
}

// --- 8. Core Arena Game Screen ---
@Composable
fun GameScreen(viewModel: SnaikyViewModel) {
    val engine = viewModel.gameEngine ?: return
    val alivePlayer = engine.snakes.find { it.id == "player" }
    val chats = viewModel.roomChats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Neon Game HUD top details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SCORE: ${viewModel.playerScore}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = CyberColor.NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LENGTH: ${viewModel.playerLength}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Power up active tile
                if (viewModel.playerPowerUp != PowerUpType.NONE) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(getPowerColor(viewModel.playerPowerUp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getPowerIcon(viewModel.playerPowerUp),
                            contentDescription = "Active Power",
                            tint = CyberColor.DarkCanvas,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${viewModel.playerPowerUp.name} [0:${viewModel.playerPowerUpSecondsLeft}]",
                            color = CyberColor.DarkCanvas,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TIME: ${viewModel.gameSecondsElapsed}s",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.pauseGame() },
                        modifier = Modifier
                            .size(24.dp)
                            .background(CyberColor.SurfaceLight, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (engine.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause match",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main game landscape block (Coordinate grid drawing)
        Box(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxWidth()
                .border(2.dp, CyberColor.SurfaceLight, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(CyberColor.DarkCanvas)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockWidth = size.width / engine.gridWidth
                val blockHeight = size.height / engine.gridHeight

                // Draw digital laser grid
                for (x in 0..engine.gridWidth) {
                    drawLine(
                        color = CyberColor.SurfaceLight.copy(alpha = 0.5f),
                        start = Offset(x * blockWidth, 0f),
                        end = Offset(x * blockWidth, size.height),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                for (y in 0..engine.gridHeight) {
                    drawLine(
                        color = CyberColor.SurfaceLight.copy(alpha = 0.5f),
                        start = Offset(0f, y * blockHeight),
                        end = Offset(size.width, y * blockHeight),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // Draw scatter foods
                for (food in engine.foods) {
                    val radius = blockWidth / 2.3f
                    val centerX = food.position.x * blockWidth + blockWidth / 2f
                    val centerY = food.position.y * blockHeight + blockHeight / 2f

                    drawCircle(
                        color = food.type.color,
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }

                // Draw Snakes
                for (snake in engine.snakes) {
                    if (!snake.isAlive) continue

                    val opacity = if (snake.activePowerUp == PowerUpType.GHOST_MODE) 0.45f else 1.0f

                    for (j in snake.body.indices) {
                        val part = snake.body[j]
                        val isHead = j == 0

                        val pWidth = blockWidth * 0.85f
                        val pHeight = blockHeight * 0.85f
                        val pX = part.x * blockWidth + (blockWidth - pWidth) / 2f
                        val pY = part.y * blockHeight + (blockHeight - pHeight) / 2f

                        // Draw snake cell body
                        drawRect(
                            color = snake.color.copy(alpha = opacity),
                            topLeft = Offset(pX, pY),
                            size = Size(pWidth, pHeight)
                        )

                        // If shield active, draw extra protective ring around head
                        if (isHead && snake.activePowerUp == PowerUpType.SHIELD) {
                            drawCircle(
                                color = CyberColor.NeonCyan,
                                radius = blockWidth * 1.3f,
                                center = Offset(pX + pWidth / 2f, pY + pHeight / 2f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Spectating mode banner when player is dead
            if (alivePlayer == null || !alivePlayer.isAlive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SPECTATING ONLINE BOT CONFLICT",
                            color = CyberColor.NeonPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You are eliminated. Waiting for match final closure.",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Pause Overlay
            if (engine.isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "GRID ENERGETICS SUSPENDED (PAUSED)",
                        color = CyberColor.NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Match Stream Comment chats (Gemini dynamically speaking)
        Box(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CyberColor.SurfaceDark)
                .border(1.dp, CyberColor.SurfaceLight)
                .padding(6.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val matchBanter = chats.filter { it.message.startsWith("[IN-MATCH]") }
                items(matchBanter) { chat ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "${chat.senderName}: ",
                            color = CyberColor.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = chat.message.removePrefix("[IN-MATCH]").trim(),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cyber movement virtual controller pad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction controller
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.handleGameDirectionInput(Direction.UP) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberColor.SurfaceDark)
                        .border(1.dp, CyberColor.NeonGreen)
                        .testTag("direction_up")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = CyberColor.NeonGreen)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { viewModel.handleGameDirectionInput(Direction.LEFT) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberColor.SurfaceDark)
                            .border(1.dp, CyberColor.NeonGreen)
                            .testTag("direction_left")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = CyberColor.NeonGreen)
                    }

                    Box(modifier = Modifier.size(48.dp)) // Center core empty

                    IconButton(
                        onClick = { viewModel.handleGameDirectionInput(Direction.RIGHT) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberColor.SurfaceDark)
                            .border(1.dp, CyberColor.NeonGreen)
                            .testTag("direction_right")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = CyberColor.NeonGreen)
                    }
                }

                IconButton(
                    onClick = { viewModel.handleGameDirectionInput(Direction.DOWN) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberColor.SurfaceDark)
                        .border(1.dp, CyberColor.NeonGreen)
                        .testTag("direction_down")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = CyberColor.NeonGreen)
                }
            }

            // Hyper Speed toggle / Abuse report desk links
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Triggers mock report filing on opposing bot players!
                        viewModel.submitAbuseReport(
                            reportedUser = "CyberSamurai",
                            reason = listOf("Speed Hack Speeding", "Cheating Coin Magnet", "Intense Trash Talk spamming").random()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceLight),
                    border = BorderStroke(1.dp, CyberColor.NeonPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Report, contentDescription = null, tint = CyberColor.NeonPink, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REPORT PLR", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    // GAME OVER ALERT POPUP
    if (viewModel.gameEngine?.isGameOver == true) {
        val won = viewModel.activeWinnerName == "You"

        AlertDialog(
            onDismissRequest = { },
            containerColor = CyberColor.SurfaceDark,
            title = {
                Text(
                    text = if (won) "VICTORY! GRID DOMINANCE ESTABLISHED" else "ARENA CLOSED / ELIMINATED",
                    color = if (won) CyberColor.NeonGreen else CyberColor.NeonPink,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = "Final Winner: ${viewModel.activeWinnerName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Eaten score size limit: ${viewModel.playerScore}", color = Color.White)
                    Text(text = "Engagement duration: ${viewModel.gameSecondsElapsed}s", color = Color.White)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.navigateTo(SnaikyScreen.HomeLobby) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen)
                ) {
                    Text("RETURN LOBBY", color = CyberColor.DarkCanvas)
                }
            }
        )
    }
}

// Power helper properties mapping
private fun getPowerColor(type: PowerUpType): Color {
    return when (type) {
        PowerUpType.SPEED_BOOST -> CyberColor.NeonYellow
        PowerUpType.SHIELD -> CyberColor.NeonCyan
        PowerUpType.DOUBLE_POINTS -> CyberColor.NeonPink
        PowerUpType.GHOST_MODE -> Color.White
        else -> Color.Transparent
    }
}

private fun getPowerIcon(type: PowerUpType): ImageVector {
    return when (type) {
        PowerUpType.SPEED_BOOST -> Icons.Default.FlashOn
        PowerUpType.SHIELD -> Icons.Default.Shield
        PowerUpType.DOUBLE_POINTS -> Icons.Default.Star
        PowerUpType.GHOST_MODE -> Icons.Default.VisibilityOff
        else -> Icons.Default.ArrowForward
    }
}

// --- 9. Leaderboard Rankings Screen ---
@Composable
fun LeaderboardScreen(viewModel: SnaikyViewModel) {
    val riders by viewModel.leaderboard.collectAsState()
    var selectedTab by remember { mutableStateOf("ALL TIME") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "GLOBAL SECTOR RANKS",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Active metrics compiled from all connected regions",
            fontSize = 11.sp,
            color = CyberColor.DarkMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ranks selector tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberColor.SurfaceDark)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("DAILY", "WEEKLY", "ALL TIME")
            for (tab in tabs) {
                val active = selectedTab == tab
                Button(
                    onClick = { selectedTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) CyberColor.SurfaceLight else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        color = if (active) CyberColor.NeonGreen else Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard Scroll List
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                itemsIndexed(riders) { index, rider ->
                    val rank = index + 1
                    val highlight = rank <= 3

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (highlight) CyberColor.SurfaceLight.copy(alpha = 0.4f) else Color.Transparent)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank Trophy indication
                            if (rank == 1) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CyberColor.NeonYellow, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "#$rank",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (highlight) CyberColor.NeonCyan else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.width(24.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            // Skin dot color indication
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getSkinColor(rider.avatar))
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = rider.username,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "LOC: ${rider.country}",
                                    fontSize = 10.sp,
                                    color = CyberColor.DarkMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Highscore value
                        Text(
                            text = "${rider.highestScore} PTS",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = CyberColor.NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// --- 10. User Profile Customize & Match History Screen ---
@Composable
fun UserProfileScreen(viewModel: SnaikyViewModel) {
    val history by viewModel.selectedPlayerHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "CUSTOM ENGINE SKIN & ACCOUNT",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Select Skin Theme Accent
                Text(
                    text = "SELECT ACTIVE SNAKE CUSTOM SKIN/COLOR",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val skins = listOf(
                    Pair("Neon green", "cyber_green"),
                    Pair("Neon Pink", "neon_pink"),
                    Pair("Neon Cyan", "retro_cyan"),
                    Pair("Matrix Code", "matrix_rain"),
                    Pair("Hologram Gold", "cyber_gold")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (skin in skins) {
                        val active = viewModel.pAvatar == skin.second
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(getSkinColor(skin.second))
                                .border(
                                    width = if (active) 3.dp else 0.dp,
                                    color = if (active) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.pAvatar = skin.second }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = viewModel.pUsername,
                    onValueChange = { viewModel.pUsername = it },
                    label = { Text("Agent Handle Username") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberColor.NeonGreen,
                        unfocusedBorderColor = CyberColor.SurfaceLight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.pCountry,
                    onValueChange = { viewModel.pCountry = it },
                    label = { Text("Geographic Territory Code") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberColor.NeonGreen,
                        unfocusedBorderColor = CyberColor.SurfaceLight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.pBio,
                    onValueChange = { viewModel.pBio = it },
                    label = { Text("Profile System Broadcast Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberColor.NeonGreen,
                        unfocusedBorderColor = CyberColor.SurfaceLight
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveProfile() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PERSIST CONFIGURATION DATA", color = CyberColor.DarkCanvas, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personal combat Logs Match History
        Text(
            text = "COMBAT MATCH LOGS (HISTORY)",
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text(
                text = "No prior matches recorded yet. Complete some matches to sync log data.",
                color = CyberColor.DarkMuted,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        } else {
            for (match in history) {
                MatchRowItem(match)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun MatchRowItem(match: MatchHistoryEntity) {
    val placementColor = if (match.position == 1) CyberColor.NeonGreen else CyberColor.NeonPink

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
        border = BorderStroke(1.dp, CyberColor.SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PLACEMENT: #${match.position}",
                    color = placementColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ENGAGED OPS COUNT: ${match.opponentsCount} | SECS: ${match.durationSeconds}s",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${match.score} PTS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatTime(match.matchDate),
                    color = CyberColor.DarkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Avatar color mapper
private fun getSkinColor(avatar: String): Color {
    return when (avatar) {
        "cyber_green" -> CyberColor.NeonGreen
        "neon_pink" -> CyberColor.NeonPink
        "retro_cyan" -> CyberColor.NeonCyan
        "matrix_rain" -> Color(0xFF00FF66)
        "cyber_gold" -> CyberColor.NeonYellow
        "cyber_admin" -> Color(0xFFFF4500)
        else -> CyberColor.DarkMuted
    }
}

// --- 11. Supreme Admin dashboard Desk (AdminPanel) ---
@Composable
fun AdminDashboardScreen(viewModel: SnaikyViewModel) {
    val users by viewModel.allUsers.collectAsState()
    val reports by viewModel.adminReports.collectAsState()
    var adminTabSelected by remember { mutableStateOf("METRICS") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ADMIN CENTRAL CONTROL DESK",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CyberColor.NeonPink
        )
        Text(
            text = "Master administrative console and cheat mitigation desk",
            fontSize = 11.sp,
            color = CyberColor.DarkMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Admin submenu tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CyberColor.SurfaceDark)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("METRICS", "USERS", "REPORTS")
            for (tab in tabs) {
                val active = adminTabSelected == tab
                Button(
                    onClick = { adminTabSelected = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) CyberColor.SurfaceLight else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 10.sp,
                        color = if (active) CyberColor.NeonPink else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active admin page view
        when (adminTabSelected) {
            "METRICS" -> AdminMetricsTab(users, reports)
            "USERS" -> AdminUsersTab(viewModel)
            "REPORTS" -> AdminReportsTab(reports, viewModel)
        }
    }
}

@Composable
fun AdminMetricsTab(users: List<UserEntity>, reports: List<AbuseReportEntity>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminMetricCard(title = "TOTAL USERS", value = users.size.toString(), modifier = Modifier.weight(1f))
            AdminMetricCard(title = "OPEN ISSUES", value = reports.count { it.status == "Pending" }.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminMetricCard(title = "SERVER TICK", value = "125ms (HEALTHY)", modifier = Modifier.weight(1f))
            AdminMetricCard(title = "BANNED USER OPS", value = users.count { it.isBanned }.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CPU/Memory Mock Chart drawings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
            border = BorderStroke(1.dp, CyberColor.SurfaceLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SERVER LOADS METRIC TIMELINE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Line graph drawing with canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val points = listOf(15f, 40f, 25f, 65f, 45f, 85f, 30f, 50f, 95f)
                    val widthStep = size.width / (points.size - 1)
                    val maxHeight = size.height

                    for (i in 0 until points.size - 1) {
                        val startX = i * widthStep
                        val startY = maxHeight - (points[i] / 100f) * maxHeight
                        val endX = (i + 1) * widthStep
                        val endY = maxHeight - (points[i + 1] / 100f) * maxHeight

                        drawLine(
                            color = CyberColor.NeonPink,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("05:00Z", color = CyberColor.DarkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("05:05Z (CURRENT)", color = CyberColor.NeonPink, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
        border = BorderStroke(1.dp, CyberColor.SurfaceLight)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = CyberColor.DarkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AdminUsersTab(viewModel: SnaikyViewModel) {
    val users by viewModel.filteredUsers.collectAsState()

    Column {
        OutlinedTextField(
            value = viewModel.adminSearchQuery,
            onValueChange = { viewModel.adminSearchQuery = it },
            placeholder = { Text("Search users by email or name...") },
            modifier = Modifier.fillMaxWidth().testTag("admin_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberColor.NeonPink,
                unfocusedBorderColor = CyberColor.SurfaceLight
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn {
                items(users) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
                        border = BorderStroke(1.dp, CyberColor.SurfaceLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        user.username,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(user.email, color = CyberColor.DarkMuted, fontSize = 11.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (user.isBanned) "BANNED" else "ACTIVE",
                                        color = if (user.isBanned) CyberColor.NeonPink else CyberColor.NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, if (user.isBanned) CyberColor.NeonPink else CyberColor.NeonGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (user.isMuted) {
                                        Text(
                                            text = "MUTED",
                                            color = CyberColor.NeonYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, CyberColor.NeonYellow, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { viewModel.adminBanUser(user.email, !user.isBanned) }) {
                                    Text(if (user.isBanned) "UNBAN" else "BAN", color = CyberColor.NeonPink, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                TextButton(onClick = { viewModel.adminMuteUser(user.email, !user.isMuted) }) {
                                    Text(if (user.isMuted) "UNMUTE" else "MUTE", color = CyberColor.NeonYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                TextButton(onClick = { viewModel.adminResetPassword(user.email) }) {
                                    Text("RESET PASS", color = CyberColor.NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                TextButton(onClick = { viewModel.adminDeleteUser(user.email) }) {
                                    Text("DELETE", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsTab(reports: List<AbuseReportEntity>, viewModel: SnaikyViewModel) {
    if (reports.isEmpty()) {
        Text("No active reported abuse files generated in game.", color = CyberColor.DarkMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
    } else {
        LazyColumn {
            items(reports) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberColor.SurfaceDark),
                    border = BorderStroke(1.dp, if (report.status == "Pending") CyberColor.NeonPink.copy(alpha = 0.5f) else CyberColor.SurfaceLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "TICKET: ${report.reason}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Text(
                                report.status,
                                color = if (report.status == "Pending") CyberColor.NeonPink else CyberColor.NeonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Accused User: @${report.reportedName}", color = Color.White, fontSize = 12.sp)
                        Text("Reporter Submitter: @${report.reporterName}", color = CyberColor.DarkMuted, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (report.status == "Pending") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { viewModel.adminResolveReport(report.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.NeonCyan),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("RESOLVE TICKET", color = CyberColor.DarkCanvas, fontSize = 9.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.adminDeleteReport(report.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberColor.SurfaceLight),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("DISMISS", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
