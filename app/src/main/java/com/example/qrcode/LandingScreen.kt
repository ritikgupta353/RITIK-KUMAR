package com.example.qrcode

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LandingScreen(onFinished: () -> Unit) {
    // Animated progress state from 0 to 1
    var progress by remember { mutableStateOf(0f) }
    var currentSubTextIndex by remember { mutableStateOf(0) }
    
    val subTexts = listOf(
        "Initializing Forge Engine...",
        "Generating Core Matrix...",
        "Calibrating Laser Optics...",
        "Optimizing Vector Pipelines...",
        "Polishing High-DPI Canvas...",
        "Ready to Craft!"
    )

    // Side-effects to animate progress and sub-texts
    LaunchedEffect(Unit) {
        val animationSteps = 100
        val totalDurationMs = 3500L
        val stepDelay = totalDurationMs / animationSteps

        for (i in 1..animationSteps) {
            delay(stepDelay)
            progress = i / 100f
            
            // Advance status subtexts proportional to progress
            val textIndex = ((progress * (subTexts.size - 1))).toInt().coerceIn(0, subTexts.size - 1)
            currentSubTextIndex = textIndex
        }
        
        // Auto-transition when done
        delay(300)
        onFinished()
    }

    // High fidelity infinite transition for ambient glows & sweep lasers
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlow")
    
    // Rotating glowing ring animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Breathing logo scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Scanning laser sweep offset (0f to 1f)
    val laserSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserSweep"
    )

    // Grid particles glow opacity
    val glowOpacity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowOpacity"
    )

    // Master dark theme palette for premium look & contrast
    val obsidianBg = Color(0xFF080C14) // Rich obsidian black-navy
    val neonIndigo = Color(0xFF2563EB) // Royal blue tech accent
    val neonCyan = Color(0xFFFF6D00) // Brand Orange forge glow
    val neonEmerald = Color(0xFFFFB300) // Deep Gold secondary
    val activeBrand = Color(0xFF1D4ED8) // Deep Blue brand

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(obsidianBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinished() } // Tap anywhere to skip
            .testTag("landing_screen_container")
    ) {
        // 1. Futuristic background ambient glows & quantum node grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            if (w > 0f && h > 0f && w.isFinite() && h.isFinite()) {
                val centerOffset = Offset(w / 2f, h / 2f)
                val auraRadius = (w * 0.8f).coerceAtLeast(1f)
                
                // Large ambient indigo background aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x334F46E5), Color.Transparent),
                        center = centerOffset,
                        radius = auraRadius
                    ),
                    center = centerOffset,
                    radius = auraRadius
                )

                // Dynamic background quantum grid lines (for visual depth)
                val spacing = 60.dp.toPx().coerceAtLeast(20f)
                val gridPaintAlpha = 0.08f
                
                var x = 0f
                var xCount = 0
                while (x < w && xCount < 100) {
                    drawLine(
                        color = neonIndigo.copy(alpha = gridPaintAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += spacing
                    xCount++
                }
                
                var y = 0f
                var yCount = 0
                while (y < h && yCount < 100) {
                    drawLine(
                        color = neonIndigo.copy(alpha = gridPaintAlpha),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += spacing
                    yCount++
                }
            }
        }

        // 2. Central Assembly Component
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Interactive Glowing Scanner Box containing the Animated Matrix Logo
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Background spinning cyber ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.width > 0f && size.height > 0f) {
                        // Outer rotating cyber dashed ring
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(neonIndigo, neonCyan, neonIndigo),
                                center = Offset(size.width / 2f, size.height / 2f)
                            ),
                            startAngle = rotationAngle,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Inner steady glowing ring with coerced safe radius
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = (size.width / 2f - 10.dp.toPx()).coerceAtLeast(0f)
                        )
                    }
                }

                // Centered Matrix Logo & custom locator square assets
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x1F818CF8)),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw localized locator squares & central grid elements
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        if (w > 0f && h > 0f && w.isFinite() && h.isFinite()) {
                            val padding = 16.dp.toPx()
                            val boxSize = 28.dp.toPx()
                            val innerBoxSize = 12.dp.toPx()
                            
                            // Color scheme matching logo assembly
                            val cornerColor = neonCyan
                            
                            // Top Left Locator Box
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(padding, padding),
                                size = Size(boxSize, boxSize),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(padding + 8.dp.toPx(), padding + 8.dp.toPx()),
                                size = Size(innerBoxSize, innerBoxSize),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            // Top Right Locator Box
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(w - padding - boxSize, padding),
                                size = Size(boxSize, boxSize),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(w - padding - boxSize + 8.dp.toPx(), padding + 8.dp.toPx()),
                                size = Size(innerBoxSize, innerBoxSize),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            // Bottom Left Locator Box
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(padding, h - padding - boxSize),
                                size = Size(boxSize, boxSize),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = cornerColor,
                                topLeft = Offset(padding + 8.dp.toPx(), h - padding - boxSize + 8.dp.toPx()),
                                size = Size(innerBoxSize, innerBoxSize),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            // Draw randomized futuristic data matrix nodes in the remaining area
                            val dotRadius = 3.dp.toPx()
                            val dotsCoords = listOf(
                                Offset(w / 2f, h / 2f),
                                Offset(w / 2f - 14.dp.toPx(), h / 2f),
                                Offset(w / 2f + 14.dp.toPx(), h / 2f),
                                Offset(w / 2f, h / 2f - 14.dp.toPx()),
                                Offset(w / 2f, h / 2f + 14.dp.toPx()),
                                Offset(w - padding - 10.dp.toPx(), h - padding - 10.dp.toPx()),
                                Offset(w - padding - 24.dp.toPx(), h - padding - 10.dp.toPx()),
                                Offset(w - padding - 10.dp.toPx(), h - padding - 24.dp.toPx())
                            )
                            
                            dotsCoords.forEach { coord ->
                                drawCircle(
                                    color = neonIndigo.copy(alpha = glowOpacity),
                                    radius = dotRadius,
                                    center = coord
                                )
                            }
                        }
                    }
                    
                    // Center high-end vector branding logo with flash of energy
                    Image(
                        painter = painterResource(id = com.example.R.drawable.ic_rg_logo),
                        contentDescription = "Branding QR logo",
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                    )
                }

                // Futuristic laser scanning sweep beam overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    if (w > 0f && h > 0f && w.isFinite() && h.isFinite()) {
                        // Limit laser drawing region to logo box bounds
                        val laserY = h * 0.2f + (h * 0.6f * laserSweep)
                        val laserWidth = w * 0.7f
                        val startX = (w - laserWidth) / 2f
                        
                        // Glowing laser guide line
                        drawLine(
                            color = neonCyan,
                            start = Offset(startX, laserY),
                            end = Offset(startX + laserWidth, laserY),
                            strokeWidth = 2.dp.toPx()
                        )
                        
                        // Laser volumetric light spread (gradient block)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(neonCyan.copy(alpha = 0.15f), Color.Transparent),
                                startY = laserY,
                                endY = laserY + 12.dp.toPx()
                            ),
                            topLeft = Offset(startX, laserY),
                            size = Size(laserWidth, 12.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. Dynamic Title Typography with clean spacing
            Text(
                text = "RG QR FORGE",
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFB300), Color(0xFFFF6D00), Color(0xFF00E5FF))
                    ),
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = 2.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "THE PREMIUM DYNAMIC STUDIO",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = Color(0xFF00E5FF), // Cyber Cyan highlight to contrast the golden title
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
        }

        // 4. Loading Progress Bar & Dynamic Status Logs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 40.dp, end = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant smooth custom progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(neonIndigo, neonCyan)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic step actions mimicking loading states
            Text(
                text = subTexts[currentSubTextIndex],
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = neonCyan,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "${(progress * 100).toInt()}% READY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.padding(top = 4.dp),
                letterSpacing = 1.sp
            )
        }

        // 5. SKIP / GET STARTED CTA WITH DEVELOPER SIGNATURE
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tap anywhere to skip setup",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "made with ❤️ by Ritik",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6D00).copy(alpha = 0.9f),
                lineHeight = 15.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
