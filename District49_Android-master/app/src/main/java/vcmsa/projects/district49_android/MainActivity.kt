package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // --- App Check set up BEFORE any Firebase usage ---
        FirebaseApp.initializeApp(applicationContext)
        val appCheck = FirebaseAppCheck.getInstance()

        // Use Play Integrity for production
        appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        appCheck.setTokenAutoRefreshEnabled(true)
        // --- end App Check setup ---

        //  Initialize notification channel
        District49NotificationManager.getInstance().createNotificationChannel(this)

        //  Initialize FCM token
        lifecycleScope.launch {
            District49NotificationManager.getInstance().initializeFCMToken()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WelcomeScreen(onComplete = {
                // Navigate to login_or_register activity
                startActivity(Intent(this, LoginOrRegisterActivity::class.java))
                finish()
            })
        }
    }
}

@Composable
fun WelcomeScreen(onComplete: () -> Unit) {
    // Define Inter font family
    val interFontFamily = FontFamily(
        Font(R.font.inter_extrabold, FontWeight.ExtraBold)
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.opening_logo_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.height(80.dp))

            // Welcome Text
            Text(
                text = "WELCOME",
                fontSize = 32.sp,
                fontFamily = interFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Slide to Start Component
            SlideToStart(
                onSlideComplete = onComplete,
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(onComplete = { /* Preview - no action */ })
}

@Composable
fun SlideToStart(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderWidth = 280.dp
    val buttonSize = 60.dp
    val maxOffset = sliderWidth - buttonSize

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val threshold = maxOffsetPx * 0.8f

    // Animation for button return
    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = tween(300),
        label = "offset"
    )

    // Pulsing animation for the button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
    ) {
        // Slider Track
        Box(
            modifier = Modifier
                .width(sliderWidth)
                .height(buttonSize)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            // Progress indicator (fills as you drag)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (animatedOffset / maxOffsetPx).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE91E63).copy(alpha = 0.6f),
                                Color(0xFFF06292).copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }

        // Draggable Button
        Box(
            modifier = Modifier
                .offset(x = with(density) { animatedOffset.toDp() })
                .size(buttonSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFFC2185B)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            if (offsetX >= threshold) {
                                onSlideComplete()
                            } else {
                                offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        // Allow both horizontal and vertical drag, but prioritize vertical for completion
                        val newOffsetX = offsetX + dragAmount.x
                        val newOffsetY = dragAmount.y

                        // If dragging up significantly, trigger completion
                        if (newOffsetY < -50f && offsetX > maxOffsetPx * 0.3f) {
                            onSlideComplete()
                        } else {
                            offsetX = max(0f, min(newOffsetX, maxOffsetPx))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "START",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(pulseAlpha)
            )
        }

        // Instruction Text
        if (offsetX < maxOffsetPx * 0.1f) {
            Text(
                text = "Slide to continue",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 40.dp)
            )
        }
    }
}