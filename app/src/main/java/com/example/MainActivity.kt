package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.qrcode.QrGeneratorLogic
import com.example.qrcode.LandingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room database and repository
        val database = com.example.database.AppDatabase.getDatabase(this)
        val repository = com.example.database.AppRepository(database)

        setContent {
            MyApplicationTheme {
                QrAppRoot(repository = repository)
            }
        }
    }
}

// Named Color Choices for Customization
data class ColorPreset(val name: String, val color: Color, val hexInt: Int)

val ForeColorPresets = listOf(
    ColorPreset("Midnight", Color(0xFF1E1E24), 0xFF1E1E24.toInt()),
    ColorPreset("Cyan Glow", Color(0xFF00E5FF), 0xFF00E5FF.toInt()),
    ColorPreset("Deep Indigo", Color(0xFF2D00F7), 0xFF2D00F7.toInt()),
    ColorPreset("Ruby Pink", Color(0xFFFF0844), 0xFFFF0844.toInt()),
    ColorPreset("Sunset Gold", Color(0xFFFF9100), 0xFFFF9100.toInt()),
    ColorPreset("Forest green", Color(0xFF00E676), 0xFF00E676.toInt())
)

val BackColorPresets = listOf(
    ColorPreset("Ice Custom White", Color(0xFFFAFAFC), 0xFFFAFAFC.toInt()),
    ColorPreset("Warm Cream", Color(0xFFFEFDF9), 0xFFFEFDF9.toInt()),
    ColorPreset("Deep Carbon", Color(0xFF121212), 0xFF121212.toInt()),
    ColorPreset("Translucent Glass", Color(0x33FFFFFF), 0x33FFFFFF.toInt())
)

@Composable
fun QrAppRoot(repository: com.example.database.AppRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(true) }
    var showLanding by remember { mutableStateOf(true) }

    // Smooth app opening transition values
    val overshootEasing = remember { CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.12f) }
    val decelEasing = remember { CubicBezierEasing(0.1f, 0.6f, 0.2f, 1.0f) }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (showLanding) 0f else 1f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100, easing = decelEasing),
        label = "EntranceAlpha"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (showLanding) 0.92f else 1f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100, easing = overshootEasing),
        label = "EntranceScale"
    )
    val entranceOffsetY by animateFloatAsState(
        targetValue = if (showLanding) 60f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 100, easing = decelEasing),
        label = "EntranceOffsetY"
    )

    // Navigation and Session States
    var activeScreen by remember { mutableStateOf("workspace") } // workspace, history, auth, admin
    var showDrawer by remember { mutableStateOf(false) }
    var loggedInUser by remember { mutableStateOf<com.example.database.UserEntity?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    
    // Ambient Animated Colors matching glass theme (Bespoke Forge Amber & Midnight Navy Palette)
    val animatedBg = if (isDarkTheme) Color(0xFF070B14) else Color(0xFFF0F4FA)
    val containerColor = if (isDarkTheme) Color(0xFF131D31) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Layout configuration
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Core States
    var currentTab by remember { mutableIntStateOf(0) } // 0: Link, 1: Plain Text, 2: Phone, 3: Wi-Fi, 4: Payment, 5: Image
    var lastOutputText by remember { mutableStateOf("https://play.google.com") }

    // Tab 0: Link States
    var urlInput by remember { mutableStateOf("https://play.google.com") }

    // Tab 1: Plain Text States
    var plainTextInput by remember { mutableStateOf("Hello via RG QR FORGE!") }

    // Tab 2: Phone Number States
    var phoneInput by remember { mutableStateOf("+1 555-0199") }

    // Tab 3: Wi-Fi States
    var wifiSsid by remember { mutableStateOf("HomeGlow_WiFi") }
    var wifiPassword by remember { mutableStateOf("crystalglass2026") }
    var wifiSecurity by remember { mutableStateOf("WPA") } // WPA, WEP, nopass

    // Tab 4: Payment States
    var paymentMode by remember { mutableStateOf("UPI") } // UPI vs PayPal
    var upiId by remember { mutableStateOf("example@oksbi") }
    var upiName by remember { mutableStateOf("Payee Name") }
    var upiAmount by remember { mutableStateOf("") }
    var upiNote by remember { mutableStateOf("Design Work") }
    
    var paypalUsername by remember { mutableStateOf("ritikraj") }
    var paypalAmount by remember { mutableStateOf("") }

    // Tab 5: Image States
    var imagePresetSelected by remember { mutableStateOf("heart") } // heart, star, smile, check, customized
    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    var compressedBase64 by remember { mutableStateOf("") }
    var imageSizeInBytes by remember { mutableStateOf(0) }
    var customImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Customization States
    var selectedForeColor by remember { mutableStateOf(ForeColorPresets[0]) }
    var selectedBackColor by remember { mutableStateOf(BackColorPresets[0]) }
    var cornerRoundness by remember { mutableFloatStateOf(0.15f) } // slider 0f to 0.5f

    // Side-effects updates the final text representation
    LaunchedEffect(
        currentTab, urlInput, plainTextInput, phoneInput, wifiSsid, wifiPassword, wifiSecurity,
        paymentMode, upiId, upiName, upiAmount, upiNote, paypalUsername, paypalAmount,
        imagePresetSelected, compressedBase64
    ) {
        lastOutputText = when (currentTab) {
            0 -> urlInput.trim().ifEmpty { "https://play.google.com" }
            1 -> plainTextInput
            2 -> {
                val cleanPhone = phoneInput.trim()
                if (cleanPhone.isNotEmpty()) "tel:$cleanPhone" else "tel:+15550199"
            }
            3 -> {
                QrGeneratorLogic.buildWifiString(wifiSsid, wifiPassword, wifiSecurity)
            }
            4 -> {
                if (paymentMode == "UPI") {
                    QrGeneratorLogic.buildUpiUri(upiId, upiName, upiAmount, upiNote)
                } else {
                    QrGeneratorLogic.buildPayPalUri(paypalUsername, paypalAmount)
                }
            }
            5 -> {
                if (imagePresetSelected == "customized" && compressedBase64.isNotEmpty()) {
                    compressedBase64
                } else {
                    val presetBitmap = QrGeneratorLogic.createPresetBitmap(imagePresetSelected)
                    val (base64, _) = QrGeneratorLogic.compressBitmapToBase64(presetBitmap, 12, 12)
                    base64
                }
            }
            else -> "https://play.google.com"
        }
    }

    // Prepare custom image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            customImageUri = uri
            imagePresetSelected = "customized"
            try {
                // Read image dimensions first to optimize heap allocation
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                var inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                // Calculate proper power-of-two sub-sampling sample scale
                val reqWidth = 512
                val reqHeight = 512
                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888

                inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Compress to fit inside QR capabilities (keep dimensions very small e.g. 16x16)
                    val (base64, size) = QrGeneratorLogic.compressBitmapToBase64(originalBitmap, 16, 16)
                    compressedBase64 = base64
                    imageSizeInBytes = size
                    customImageBitmap = originalBitmap
                } else {
                    Toast.makeText(context, "Failed to decode image.", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                android.util.Log.e("MainActivity", "Error handling selected user image file", t)
                Toast.makeText(context, "Image file too large or invalid. Loaded with safety limits.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Standard Scaffold with Dynamic Blurring Background Canvas
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            containerColor = animatedBg
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ambient Neon Orbs drawn in canvas background for high-end Glassmorphism aesthetics
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = entranceAlpha }
            ) {
                val w = size.width
                val h = size.height
                if (w > 0f && h > 0f && w.isFinite() && h.isFinite()) {
                    val purpleOrbOffset = Offset(w * 0.15f, h * 0.2f)
                    val blueOrbOffset = Offset(w * 0.85f, h * 0.7f)
                    val orbRadius = (w * 0.55f).coerceAtLeast(1f)
                    
                    if (isDarkTheme) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x22FF6D00), Color.Transparent), // Glowing Amber
                                center = purpleOrbOffset,
                                radius = orbRadius
                            ),
                            center = purpleOrbOffset,
                            radius = orbRadius
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1E2563EB), Color.Transparent), // Tech Blue
                                center = blueOrbOffset,
                                radius = orbRadius
                            ),
                            center = blueOrbOffset,
                            radius = orbRadius
                        )
                    } else {
                        // Soft elegant light warm and cool ambient pairs
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1CFF6D00), Color.Transparent),
                                center = purpleOrbOffset,
                                radius = orbRadius
                            ),
                            center = purpleOrbOffset,
                            radius = orbRadius
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1E2563EB), Color.Transparent),
                                center = blueOrbOffset,
                                radius = orbRadius
                            ),
                            center = blueOrbOffset,
                            radius = orbRadius
                        )
                    }
                }
            }

            // Central Scrollable layout inside the safe zone
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = entranceAlpha
                        scaleX = entranceScale
                        scaleY = entranceScale
                        translationY = entranceOffsetY
                    }
            ) {
                // Header Row matching HTML QuickQR design style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hamburger Menu Drawer Button
                        IconButton(
                            onClick = { showDrawer = true },
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(1.dp, CircleShape)
                                .background(
                                    if (isDarkTheme) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x33000000),
                                    CircleShape
                                )
                                .testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Menu",
                                tint = if (isDarkTheme) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // RG QRFORGE Custom Shield Logo Header Asset
                        Box(
                            modifier = Modifier
                                .size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_rg_logo),
                                contentDescription = "QuickQR Code Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = "RG QR FORGE",
                                color = textPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("app_title")
                            )
                            Text(
                                text = "Dynamic Frosted Vector Studio",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Mode Switcher Button (with glass border and translucent backdrop)
                    IconButton(
                        onClick = { isDarkTheme = !isDarkTheme },
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(1.dp, CircleShape)
                            .background(
                                if (isDarkTheme) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x33000000),
                                CircleShape
                            )
                            .testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Switch theme",
                            tint = if (isDarkTheme) Color(0xFFFFB300) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (activeScreen == "workspace") {
                    // Split Layout for Tablet / Landscape or column for phone portrait
                    if (isTablet) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Left Column: Interactive Inputs and Customization Options
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                GlassCard(isDarkMode = isDarkTheme) {
                                    TabSelector(
                                        currentTab = currentTab,
                                        isDarkMode = isDarkTheme,
                                        onTabSelected = { currentTab = it }
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    TabContentPane(
                                        currentTab = currentTab,
                                        urlInput = urlInput,
                                        onUrlChange = { urlInput = it },
                                        plainTextInput = plainTextInput,
                                        onPlainTextInputChange = { plainTextInput = it },
                                        phoneInput = phoneInput,
                                        onPhoneInputChange = { phoneInput = it },
                                        wifiSsid = wifiSsid,
                                        onWifiSsidChange = { wifiSsid = it },
                                        wifiPassword = wifiPassword,
                                        onWifiPasswordChange = { wifiPassword = it },
                                        wifiSecurity = wifiSecurity,
                                        onWifiSecurityChange = { wifiSecurity = it },
                                        paymentMode = paymentMode,
                                        onPaymentModeChange = { paymentMode = it },
                                        upiId = upiId,
                                        onUpiIdChange = { upiId = it },
                                        upiName = upiName,
                                        onUpiNameChange = { upiName = it },
                                        upiAmount = upiAmount,
                                        onUpiAmountChange = { upiAmount = it },
                                        upiNote = upiNote,
                                        onUpiNoteChange = { upiNote = it },
                                        paypalUsername = paypalUsername,
                                        onPaypalUsernameChange = { paypalUsername = it },
                                        paypalAmount = paypalAmount,
                                        onPaypalAmountChange = { paypalAmount = it },
                                        imagePresetSelected = imagePresetSelected,
                                        onPresetSelectedChange = { imagePresetSelected = it },
                                        customImageUri = customImageUri,
                                        onPickImageClick = { imagePickerLauncher.launch("image/*") },
                                        imageSizeInBytes = imageSizeInBytes,
                                        isDarkMode = isDarkTheme
                                    )
                                }

                                GlassCard(isDarkMode = isDarkTheme) {
                                    CustomizerPane(
                                        selectedForeColor = selectedForeColor,
                                        onForeColorChange = { selectedForeColor = it },
                                        selectedBackColor = selectedBackColor,
                                        onBackColorChange = { selectedBackColor = it },
                                        cornerRoundness = cornerRoundness,
                                        onRoundnessChange = { cornerRoundness = it },
                                        isDarkMode = isDarkTheme
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "powered\nmade by Ritik❤️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary.copy(alpha = 0.5f),
                                    lineHeight = 15.sp,
                                    letterSpacing = 1.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                )
                            }

                            // Right Column: Floating QR Live Preview & Actions
                            Column(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LivePreviewCard(
                                    outputText = lastOutputText,
                                    foreColor = selectedForeColor,
                                    backColor = selectedBackColor,
                                    roundness = cornerRoundness,
                                    isDarkMode = isDarkTheme,
                                    currentTab = currentTab,
                                    paymentMode = paymentMode,
                                    onSaveToHistory = { showSaveDialog = true }
                                )
                            }
                        }
                    } else {
                        // Mobile Portrait Scrolling Main Column
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Inputs Card
                            GlassCard(isDarkMode = isDarkTheme) {
                                TabSelector(
                                    currentTab = currentTab,
                                    isDarkMode = isDarkTheme,
                                    onTabSelected = { currentTab = it }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                TabContentPane(
                                    currentTab = currentTab,
                                    urlInput = urlInput,
                                    onUrlChange = { urlInput = it },
                                    plainTextInput = plainTextInput,
                                    onPlainTextInputChange = { plainTextInput = it },
                                    phoneInput = phoneInput,
                                    onPhoneInputChange = { phoneInput = it },
                                    wifiSsid = wifiSsid,
                                    onWifiSsidChange = { wifiSsid = it },
                                    wifiPassword = wifiPassword,
                                    onWifiPasswordChange = { wifiPassword = it },
                                    wifiSecurity = wifiSecurity,
                                    onWifiSecurityChange = { wifiSecurity = it },
                                    paymentMode = paymentMode,
                                    onPaymentModeChange = { paymentMode = it },
                                    upiId = upiId,
                                    onUpiIdChange = { upiId = it },
                                    upiName = upiName,
                                    onUpiNameChange = { upiName = it },
                                    upiAmount = upiAmount,
                                    onUpiAmountChange = { upiAmount = it },
                                    upiNote = upiNote,
                                    onUpiNoteChange = { upiNote = it },
                                    paypalUsername = paypalUsername,
                                    onPaypalUsernameChange = { paypalUsername = it },
                                    paypalAmount = paypalAmount,
                                    onPaypalAmountChange = { paypalAmount = it },
                                    imagePresetSelected = imagePresetSelected,
                                    onPresetSelectedChange = { imagePresetSelected = it },
                                    customImageUri = customImageUri,
                                    onPickImageClick = { imagePickerLauncher.launch("image/*") },
                                    imageSizeInBytes = imageSizeInBytes,
                                    isDarkMode = isDarkTheme
                                )
                            }

                            // Customizer Options Card
                            GlassCard(isDarkMode = isDarkTheme) {
                                CustomizerPane(
                                    selectedForeColor = selectedForeColor,
                                    onForeColorChange = { selectedForeColor = it },
                                    selectedBackColor = selectedBackColor,
                                    onBackColorChange = { selectedBackColor = it },
                                    cornerRoundness = cornerRoundness,
                                    onRoundnessChange = { cornerRoundness = it },
                                    isDarkMode = isDarkTheme
                                )
                            }

                            // Live Preview at the bottom
                            LivePreviewCard(
                                outputText = lastOutputText,
                                foreColor = selectedForeColor,
                                backColor = selectedBackColor,
                                roundness = cornerRoundness,
                                isDarkMode = isDarkTheme,
                                currentTab = currentTab,
                                paymentMode = paymentMode,
                                onSaveToHistory = { showSaveDialog = true }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "powered\nmade by Ritik❤️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary.copy(alpha = 0.5f),
                                lineHeight = 15.sp,
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                        }
                    }
                } else if (activeScreen == "history") {
                    com.example.qrcode.HistoryScreen(
                        repository = repository,
                        loggedInUser = loggedInUser,
                        isDarkMode = isDarkTheme
                    ) { tab, content, foreHex, backHex, roundness ->
                        currentTab = tab
                        when (tab) {
                            0 -> urlInput = content
                            1 -> plainTextInput = content
                            2 -> phoneInput = content
                            3 -> {
                                val parseParts = content.split(";")
                                for (part in parseParts) {
                                    if (part.startsWith("S:")) wifiSsid = part.substring(2)
                                    if (part.startsWith("P:")) wifiPassword = part.substring(2)
                                    if (part.startsWith("T:")) wifiSecurity = part.substring(2)
                                }
                            }
                            4 -> {
                                if (content.startsWith("upi:")) {
                                    paymentMode = "UPI"
                                    val uri = Uri.parse(content)
                                    upiId = uri.getQueryParameter("pa") ?: ""
                                    upiName = uri.getQueryParameter("pn") ?: ""
                                    upiAmount = uri.getQueryParameter("am") ?: ""
                                    upiNote = uri.getQueryParameter("tn") ?: ""
                                } else {
                                    paymentMode = "PayPal"
                                    paypalUsername = content.substringAfter("paypal.me/").substringBefore("/")
                                    paypalAmount = content.substringAfter("paypal.me/$paypalUsername/").ifEmpty { "" }
                                }
                            }
                            5 -> {
                                compressedBase64 = content
                                imagePresetSelected = "customized"
                            }
                        }
                        
                        // Set custom colors
                        val foundFore = ForeColorPresets.firstOrNull { it.hexInt == foreHex } ?: ColorPreset("Restored", Color(foreHex), foreHex)
                        val foundBack = BackColorPresets.firstOrNull { it.hexInt == backHex } ?: ColorPreset("Restored", Color(backHex), backHex)
                        selectedForeColor = foundFore
                        selectedBackColor = foundBack
                        cornerRoundness = roundness
                        activeScreen = "workspace"
                    }
                } else if (activeScreen == "auth") {
                    com.example.qrcode.AuthScreen(
                        repository = repository,
                        loggedInUser = loggedInUser,
                        isDarkMode = isDarkTheme,
                        onLoginSuccess = { loggedInUser = it },
                        onLogout = { loggedInUser = null }
                    )
                } else {
                    com.example.qrcode.AdminScreen(
                        repository = repository,
                        loggedInUser = loggedInUser,
                        isDarkMode = isDarkTheme
                    )
                }
            }

            // Save layout configuration alert dialog
            if (showSaveDialog) {
                val dialogBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    containerColor = dialogBg,
                    shape = RoundedCornerShape(18.dp),
                    title = {
                        Text(
                            text = "Save Design Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Associate your active custom QR design with your history session list.",
                                color = textSecondary,
                                fontSize = 13.sp
                            )
                            OutlinedTextField(
                                value = saveTitleInput,
                                onValueChange = { saveTitleInput = it },
                                placeholder = { Text("e.g. My Website QR") },
                                label = { Text("Design Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = Color(0xFF4F46E5)
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val titleVal = saveTitleInput.trim().ifEmpty { 
                                    val simpleType = when (currentTab) {
                                        0 -> "Link"
                                        1 -> "Plain Text"
                                        2 -> "Phone"
                                        3 -> "Wi-Fi"
                                        4 -> "Payment"
                                        else -> "Image"
                                    }
                                    "$simpleType QR Design"
                                }
                                
                                val qrEntity = com.example.database.SavedQrEntity(
                                    username = loggedInUser?.username ?: "anonymous",
                                    title = titleVal,
                                    type = when (currentTab) {
                                        0 -> "URL Link"
                                        1 -> "Plain Text"
                                        2 -> "Phone Number"
                                        3 -> "Wi-Fi"
                                        4 -> "UPI Payment"
                                        else -> "Image Customization"
                                    },
                                    content = lastOutputText,
                                    foreColorHex = selectedForeColor.hexInt,
                                    backColorHex = selectedBackColor.hexInt,
                                    roundness = cornerRoundness
                                )
                                
                                coroutineScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            repository.insertSavedQr(qrEntity)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Failed to save QR configuration", e)
                                    }
                                }
                                
                                showSaveDialog = false
                                saveTitleInput = ""
                                Toast.makeText(context, "Layout config saved to History bank!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Layout", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = textSecondary)
                        }
                    }
                )
            }

            // Sliding Side Navigation Menu Drawer (Frosted overlay)
            AnimatedVisibility(
                visible = showDrawer,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (isTablet) 0.35f else 0.78f)
                    .zIndex(10f)
            ) {
                Surface(
                    color = if (isDarkTheme) Color(0xFB0F1219) else Color(0xFAF8F9FF),
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            1.dp,
                            if (isDarkTheme) Color(0x22FFFFFF) else Color(0x110F172A),
                            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        ),
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    tonalElevation = 16.dp,
                    shadowElevation = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.DashboardCustomize, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Text("FORGE MENU", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp, color = textPrimary)
                            }
                            IconButton(onClick = { showDrawer = false }) {
                                Icon(Icons.Rounded.Close, null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        HorizontalDivider(color = if (isDarkTheme) Color(0x12FFFFFF) else Color(0x12000000))
                        
                        // Menu Choices List
                        listOf(
                            Triple("workspace", "QR Studio Designer", Icons.Rounded.QrCode2),
                            Triple("history", "My Saved History", Icons.Rounded.History),
                            Triple("auth", if (loggedInUser != null) "My Account Profile" else "Login & SignUp", Icons.Rounded.AccountCircle)
                        ).forEach { (scr, label, icon) ->
                            val isSelected = activeScreen == scr
                            val itemBg = if (isSelected) (if (isDarkTheme) Color(0x26FF6D00) else Color(0x1A2563EB)) else Color.Transparent
                            val itemTextCol = if (isSelected) (if (isDarkTheme) Color(0xFFFF9100) else Color(0xFF1D4ED8)) else textPrimary
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBg)
                                    .clickable {
                                        activeScreen = scr
                                        showDrawer = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = itemTextCol, modifier = Modifier.size(18.dp))
                                Text(label, color = itemTextCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Footer account indicator card inside menu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDarkTheme) Color(0xFF131D31) else Color(0x122563EB))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6D00)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = loggedInUser?.username?.take(2)?.uppercase() ?: "G",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = loggedInUser?.username ?: "Guest Session",
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (loggedInUser != null) loggedInUser!!.role.uppercase() else "OFFLINE MODE",
                                    color = textSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "powered by Ritik❤️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary.copy(alpha = 0.45f),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Drawer Dim Backdrop overlay layer
            if (showDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showDrawer = false }
                        .zIndex(5f)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showLanding,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(650)) + slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(650, easing = FastOutSlowInEasing))
    ) {
        LandingScreen(onFinished = { showLanding = false })
    }
}
}

// Glass Card Shell styled with frosted translucency
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    // 90% white glass in light mode for excellent layout contrast, slate translucency in dark mode
    val bgCol = if (isDarkMode) Color(0x331E293B) else Color(0xE6FFFFFF)
    // Subtle dark border outline in light mode for high-contrast structure, soft white sheen in dark mode
    val borderCol = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A0F172A)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDarkMode) 10.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        color = bgCol,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

// Tabbed system selector with glassmorphic container and active indicator
@Composable
fun TabSelector(
    currentTab: Int,
    isDarkMode: Boolean,
    onTabSelected: (Int) -> Unit
) {
    val activeColor = Color.White
    val inactiveColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf("Link", "Plain Text", "Phone", "Wi-Fi", "Payment", "Image")
        tabs.forEachIndexed { index, title ->
            val isActive = currentTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isActive) {
                            Color(0xFF4F46E5) // Premium branding focus
                        } else {
                            if (isDarkMode) Color(0x221E293B) else Color(0x1F64748B)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) Color(0x33FFFFFF) else {
                            if (isDarkMode) Color(0x1AFFFFFF) else Color(0x120F172A)
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("tab_button_$index"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isActive) activeColor else inactiveColor,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

// Tab content controls switcher
@Composable
fun TabContentPane(
    currentTab: Int,
    urlInput: String,
    onUrlChange: (String) -> Unit,
    plainTextInput: String,
    onPlainTextInputChange: (String) -> Unit,
    phoneInput: String,
    onPhoneInputChange: (String) -> Unit,
    wifiSsid: String,
    onWifiSsidChange: (String) -> Unit,
    wifiPassword: String,
    onWifiPasswordChange: (String) -> Unit,
    wifiSecurity: String,
    onWifiSecurityChange: (String) -> Unit,
    paymentMode: String,
    onPaymentModeChange: (String) -> Unit,
    upiId: String,
    onUpiIdChange: (String) -> Unit,
    upiName: String,
    onUpiNameChange: (String) -> Unit,
    upiAmount: String,
    onUpiAmountChange: (String) -> Unit,
    upiNote: String,
    onUpiNoteChange: (String) -> Unit,
    paypalUsername: String,
    onPaypalUsernameChange: (String) -> Unit,
    paypalAmount: String,
    onPaypalAmountChange: (String) -> Unit,
    imagePresetSelected: String,
    onPresetSelectedChange: (String) -> Unit,
    customImageUri: Uri?,
    onPickImageClick: () -> Unit,
    imageSizeInBytes: Int,
    isDarkMode: Boolean
) {
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val brandAccent = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5) // Indigo-600 focus representation
    val fieldBg = if (isDarkMode) Color(0x4D0F172A) else Color(0xCCFFFFFF) // White/80% in light mode

    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "tab_content_change"
    ) { targetTab ->
        when (targetTab) {
            0 -> {
                // Link Mode representation
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "App or Website URL".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field"),
                        placeholder = { Text("https://apps.apple.com/app/id12345") },
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = "Link", tint = brandAccent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    // Helper Preset Chips
                    Text(
                        text = "Quick Presets:".uppercase(),
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "Google Play" to "https://play.google.com/store",
                            "App Store" to "https://apps.apple.com",
                            "Portfolio" to "https://github.com",
                            "YouTube" to "https://youtube.com"
                        )
                        presets.forEach { (label, valStr) ->
                            FilterChip(
                                selected = urlInput == valStr,
                                onClick = { onUrlChange(valStr) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brandAccent,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (isDarkMode) Color(0x1F1E293B) else Color(0x1F64748B)
                                )
                            )
                        }
                    }

                    // Validation indicator
                    val isValidUrl = urlInput.startsWith("http://") || urlInput.startsWith("https://")
                    if (!isValidUrl && urlInput.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, "Warning", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Standard protocols (http:// or https://) ensure universal scanner support.",
                                color = Color(0xFFE11D48),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (urlInput.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, "Valid", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Standard link protocol verified.",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            1 -> {
                // Plain Text Mode representation
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Raw Text Message".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = plainTextInput,
                        onValueChange = onPlainTextInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .testTag("text_input_field"),
                        placeholder = { Text("Enter plain text or formatted notes to encode...") },
                        leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = "Text", tint = brandAccent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    
                    Text(
                        text = "Everything matches verbatim. Scanning this will show the message directly on any camera or scanner app without requiring internet.",
                        color = textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            2 -> {
                // Phone Number Mode representation
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Direct Call Phone Number".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = onPhoneInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input_field"),
                        placeholder = { Text("+1 555-0199") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = "Phone", tint = brandAccent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    Text(
                        text = "Uses standard international telephone protocol ('tel:'). Universal scanner support will prompt standard dialers to call immediately.",
                        color = textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            3 -> {
                // Wi-Fi Password Mode representation
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Wi-Fi Hotspot details".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    // SSID / Network Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Network Name (SSID)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = onWifiSsidChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wifi_ssid_field"),
                            placeholder = { Text("eg. HomeGlow_WiFi") },
                            leadingIcon = { Icon(Icons.Rounded.Wifi, contentDescription = "WiFi Name", tint = brandAccent) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedBorderColor = brandAccent,
                                unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }

                    // Security Type Option (WPA / WEP / Open)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Security Standard", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0x1F0F172A) else Color(0x1F64748B))
                                .border(1.dp, if (isDarkMode) Color(0x1AFFFFFF) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            listOf("WPA", "WEP", "nopass").forEach { mode ->
                                val isSelected = wifiSecurity == mode
                                val label = when (mode) {
                                    "WPA" -> "WPA/WPA2"
                                    "WEP" -> "WEP"
                                    else -> "Open"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) (if (isDarkMode) Color(0xFF1E293B) else Color.White) else Color.Transparent)
                                        .clickable { onWifiSecurityChange(mode) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) brandAccent else textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Password (hide if Open security mode)
                    if (wifiSecurity != "nopass") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Pre-shared Key (Password)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            var isPasswordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = onWifiPasswordChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wifi_password_field"),
                                placeholder = { Text("Password credentials") },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                            tint = textSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
            4 -> {
                // Payment Mode representation
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Merchant Payment Gateway".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Payment Provider Switcher (UPI / PayPal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0x1F0F172A) else Color(0x1F64748B))
                            .border(1.dp, if (isDarkMode) Color(0x1AFFFFFF) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf("UPI", "PayPal").forEach { mode ->
                            val isSelected = paymentMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) (if (isDarkMode) Color(0xFF1E293B) else Color.White) else Color.Transparent)
                                    .clickable { onPaymentModeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) brandAccent else textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (paymentMode == "UPI") {
                        // UPI Fields
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "UPI Virtual Payment Address (VPA)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = onUpiIdChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upi_id_field"),
                                placeholder = { Text("username@bank") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Error warning for absent @ symbol
                            if (!upiId.contains("@") && upiId.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Error, "Error", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                    Text("UPI address must contain '@' symbol", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(text = "Payee Name", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = upiName,
                                        onValueChange = onUpiNameChange,
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedBorderColor = brandAccent,
                                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(0.8f)) {
                                    Text(text = "Amount (Optional)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = upiAmount,
                                        onValueChange = onUpiAmountChange,
                                        singleLine = true,
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedBorderColor = brandAccent,
                                            unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Transaction Remarks / Note (Optional)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = upiNote,
                                onValueChange = onUpiNoteChange,
                                singleLine = true,
                                placeholder = { Text("Note description") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    } else {
                        // PayPal Fields
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "PayPal Username or Email Address", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = paypalUsername,
                                onValueChange = onPaypalUsernameChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paypal_user_field"),
                                placeholder = { Text("username or email") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Text(text = "Amount (Optional)", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = paypalAmount,
                                onValueChange = onPaypalAmountChange,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("e.g. 15.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }
            5 -> {
                // Image to QR mode with Compression
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Image Bit-string Vectorization".uppercase(),
                        color = brandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Tiny preset visualizers
                    Text(
                        text = "Interactive Local Preset Grids:".uppercase(),
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "heart" to "❤️ Heart",
                            "star" to "⭐ Star",
                            "smile" to "😊 Smile",
                            "check" to "✅ Verified"
                        )
                        presets.forEach { (presetKey, label) ->
                            val active = imagePresetSelected == presetKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) brandAccent else fieldBg)
                                    .clickable { onPresetSelectedChange(presetKey) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) Color.White else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Gallery / Custom File picker section
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Or Load Custom Image from Gallery:".uppercase(),
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(fieldBg)
                            .border(
                                width = 1.5.dp,
                                color = if (imagePresetSelected == "customized") brandAccent else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onPickImageClick() }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = "Uploader",
                                tint = brandAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Tap to Pick Image File",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (customImageUri != null && imagePresetSelected == "customized") {
                                    "File selected: \n${customImageUri.lastPathSegment}"
                                } else {
                                    "PNG or JPEG files supported"
                                },
                                color = textSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Warning / Size alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkMode) Color(0x334F46E5) else Color(0x1F4F46E5))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Info, "Limits info", tint = brandAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "QR Code Limit Optimization",
                                    color = brandAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Standard QR Codes store up to 2,953 characters. Your image is auto-scaled down and optimized to a compact Base64 format to guarantee instant recognition on all standard scanners.",
                                color = textPrimary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Styling Control Options List
@Composable
fun CustomizerPane(
    selectedForeColor: ColorPreset,
    onForeColorChange: (ColorPreset) -> Unit,
    selectedBackColor: ColorPreset,
    onBackColorChange: (ColorPreset) -> Unit,
    cornerRoundness: Float,
    onRoundnessChange: (Float) -> Unit,
    isDarkMode: Boolean
) {
    val textPrimary = if (isDarkMode) Color(0xFFF5F7FA) else Color(0xFF1A1F26)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Fine-Tune QR Code Aesthetics",
            color = textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Corner styling Roundness slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Corner & Module Roundness (EyeRadius)",
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(cornerRoundness * 200).toInt()}%",
                    color = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFF00C853),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = cornerRoundness,
                onValueChange = onRoundnessChange,
                valueRange = 0f..0.5f,
                steps = 10,
                modifier = Modifier.testTag("roundness_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFF00C853),
                    activeTrackColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFF00C853)
                )
            )
        }

        // Foreground preset dots
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Vector Foreground Tint",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ForeColorPresets.forEach { preset ->
                    val isChosen = selectedForeColor.name == preset.name
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .border(
                                width = if (isChosen) 3.dp else 0.dp,
                                color = if (isDarkMode) Color.White else Color.Black,
                                shape = CircleShape
                            )
                            .clickable { onForeColorChange(preset) }
                            .testTag("fore_color_${preset.name.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChosen) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = if (preset.color == Color(0xFFFAFAFC) || preset.color == Color(0xFF00E5FF) || preset.color == Color(0xFF00E676)) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Background preset dots
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Canvas Background color",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackColorPresets.forEach { preset ->
                    val isChosen = selectedBackColor.name == preset.name
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .border(
                                width = if (isChosen) 2.5.dp else 1.dp,
                                color = if (isChosen) {
                                    if (isDarkMode) Color(0xFF00FFCC) else Color(0xFF4F46E5)
                                } else {
                                    if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                                },
                                shape = CircleShape
                            )
                            .clickable { onBackColorChange(preset) }
                            .testTag("back_color_${preset.name.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChosen) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = if (preset.name == "Deep Carbon") Color.White else Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Live vector preview representation card with downloads and copy clip integrations
@Composable
fun LivePreviewCard(
    outputText: String,
    foreColor: ColorPreset,
    backColor: ColorPreset,
    roundness: Float,
    isDarkMode: Boolean,
    currentTab: Int,
    paymentMode: String,
    onSaveToHistory: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textPrimary = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Compute binary matrix representing current QR asynchronously to prevent UI block or ANR
    var qrMatrix by remember { mutableStateOf<Array<BooleanArray>?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(outputText) {
        isGenerating = true
        qrMatrix = withContext(Dispatchers.Default) {
            QrGeneratorLogic.generateMatrix(outputText)
        }
        isGenerating = false
    }

    // Set vector painter for local branding inside the quiet center zone ring
    val painter = rememberVectorPainter(
        image = when (currentTab) {
            0 -> Icons.Rounded.Link
            1 -> Icons.Rounded.Description
            2 -> Icons.Rounded.Phone
            3 -> Icons.Rounded.Wifi
            4 -> if (paymentMode == "UPI") Icons.Rounded.AccountBalanceWallet else Icons.Rounded.Payment
            else -> Icons.Rounded.Image
        }
    )

    val currentMatrix = qrMatrix

    GlassCard(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Live dynamic Preview",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Dynamic vector rendering Canvas
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .shadow(
                        elevation = if (isDarkMode) 4.dp else 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        spotColor = if (isDarkMode) Color.Transparent else Color(0x2B000000),
                        ambientColor = if (isDarkMode) Color.Transparent else Color(0x12000000)
                    )
                    .background(backColor.color, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x400F172A),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentMatrix != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val N = currentMatrix.size
                        val cellW = size.width / N
                        val cellH = size.height / N
                        val moduleRadius = roundness * cellW

                        // Quiet center zone masking (5x5 cells in center) for brand shield
                        val midStart = N / 2 - 2
                        val midEnd = N / 2 + 1

                        for (y in 0 until N) {
                            for (x in 0 until N) {
                                if (x in midStart..midEnd && y in midStart..midEnd) {
                                    continue
                                }
                                if (currentMatrix[y][x]) {
                                    val left = x * cellW
                                    val top = y * cellH

                                    if (roundness > 0f) {
                                        drawRoundRect(
                                            color = foreColor.color,
                                            topLeft = Offset(left, top),
                                            size = Size(cellW, cellH),
                                            cornerRadius = CornerRadius(moduleRadius, moduleRadius)
                                        )
                                    } else {
                                        drawRect(
                                            color = foreColor.color,
                                            topLeft = Offset(left, top),
                                            size = Size(cellW, cellH)
                                        )
                                    }
                                }
                            }
                        }

                        // Now draw the center badge container (with quiet background & outline rim)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val badgeRadius = size.width * 0.13f

                        // Draw background boundary block
                        drawCircle(
                            color = backColor.color,
                            radius = badgeRadius,
                            center = Offset(cx, cy)
                        )
                        // Draw badge outline frame matching the QR foreground
                        drawCircle(
                            color = foreColor.color,
                            radius = badgeRadius,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // Draw centered vector brand icon inside the quiet zone ring
                        val iconSize = badgeRadius * 1.15f
                        translate(left = cx - iconSize / 2f, top = cy - iconSize / 2f) {
                            with(painter) {
                                draw(
                                    size = Size(iconSize, iconSize),
                                    colorFilter = ColorFilter.tint(foreColor.color)
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Awaiting QR compilation...",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Output feedback metrics
            Text(
                text = "Destination Payload Size: ${outputText.length} Chars",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Dynamic Action Button rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy payload link text button (frosted translucent backdrop outline style)
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QR Payload Link", outputText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied payload text content to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_text_button")
                        .height(48.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDarkMode) Color(0x1F64748B) else Color(0x0F64748B),
                        contentColor = textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ContentCopy, "Copy Text", modifier = Modifier.size(16.dp))
                        Text("Copy String", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Download/Save PNG to public gallery button (Premium branding color fill)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                QrGeneratorLogic.saveQrToGallery(
                                    context = context,
                                    matrix = currentMatrix,
                                    foreColor = foreColor.hexInt,
                                    backColor = backColor.hexInt,
                                    roundness = roundness
                                )
                            }
                            if (uri != null) {
                                Toast.makeText(context, "Vector PNG exported to Picture Gallery!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Saved successfully to Pictures/QRCodeGenerator!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_button")
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Download, "Download QR", modifier = Modifier.size(16.dp))
                        Text("Download PNG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (onSaveToHistory != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSaveToHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_history_button")
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color(0xFF10B981) else Color(0xFF0F9D58),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, "Save to History", modifier = Modifier.size(16.dp))
                        Text("Save Layout to History", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
