package vcmsa.projects.district49_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.models.NotificationPreferences

class NotificationSettingsActivity : ComponentActivity() {

    private lateinit var notificationManager: District49NotificationManager
    private val auth = FirebaseAuth.getInstance()

    // Permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationManager = District49NotificationManager.getInstance()
        notificationManager.createNotificationChannel(this)

        // Request notification permission on Android 13+
        checkNotificationPermission()

        setContent {
            NotificationSettingsScreenWithNavBar(
                notificationManager = notificationManager,
                onBackPressed = { finish() },
                onMenuClick = {
                    // Navigate to MenuActivity
                    try {
                        val intent = Intent(this, MenuActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Menu not available", Toast.LENGTH_SHORT).show()
                    }
                },
                onNotificationsClick = {
                    // Already on notifications page - show message
                    Toast.makeText(this, "You're already on notifications", Toast.LENGTH_SHORT).show()
                },
                onProfileClick = {
                    // Navigate to ProfileActivity
                    try {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Profile not available", Toast.LENGTH_SHORT).show()
                    }
                },
                onHomeClick = {
                    // Navigate to Homepage with clear stack
                    try {
                        val intent = Intent(this, Homepage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Home not available", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsScreenWithNavBar(
    notificationManager: District49NotificationManager,
    onBackPressed: () -> Unit,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        NotificationSettingsScreen(
            notificationManager = notificationManager,
            onBackPressed = onBackPressed
        )

        // Compose NavBar at the bottom
        ComposeNavBar(
            onBackPressed = onBackPressed,
            onMenuClick = onMenuClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick,
            onHomeClick = onHomeClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ComposeNavBar(
    onBackPressed: () -> Unit,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        // Main navbar background - GRAY #DEDEDE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFDEDEDE)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button - using your navbar_arrow_back drawable
            NavBarButton(
                iconRes = R.drawable.navbar_arrow_back,
                onClick = onBackPressed,
                modifier = Modifier.weight(1f)
            )

            // Menu button - using your menu_menu2 drawable (three lines)
            NavBarButton(
                iconRes = R.drawable.menu_menu2,
                onClick = onMenuClick,
                modifier = Modifier.weight(1f)
            )

            // Spacer for home button
            Spacer(modifier = Modifier.weight(1f))

            // Notifications button - using your navbar_notifications drawable
            NavBarButton(
                iconRes = R.drawable.navbar_notifications,
                onClick = onNotificationsClick,
                modifier = Modifier.weight(1f)
            )

            // Profile button - using your navbar_profile drawable
            NavBarButton(
                iconRes = R.drawable.navbar_profile,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Home button (floating in center) - using your navbar_home drawable
        // GRAY background with ORANGE icon
        FloatingActionButton(
            onClick = onHomeClick,
            modifier = Modifier
                .size(74.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-15).dp) // Moved lower
                .shadow(8.dp, CircleShape),
            containerColor = Color(0xFFDEDEDE), // GRAY background
            contentColor = Color.Transparent // Remove default icon color
        ) {
            Image(
                painter = painterResource(id = R.drawable.navbar_home),
                contentDescription = "Home",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(Color(0xFFFF9800)) // ORANGE tint for home icon
            )
        }
    }
}

@Composable
fun NavBarButton(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(Color(0xFFFF9800)) // ORANGE tint for all icons
        )
    }
}

@Composable
fun NotificationSettingsScreen(
    notificationManager: District49NotificationManager,
    onBackPressed: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var preferences by remember { mutableStateOf(NotificationPreferences()) }

    val context = LocalContext.current
    val lifecycleScope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Load preferences
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            try {
                val result = notificationManager.getUserPreferences(currentUser.uid)
                if (result.isSuccess) {
                    preferences = result.getOrNull()!!
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading preferences", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.contact_us_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f))
        )

        // Main Content - Add bottom padding to avoid navbar overlap
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.final_logo),
                contentDescription = "District 49 Logo",
                modifier = Modifier
                    .width(120.dp)
                    .height(100.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(
                        Color(0xFFB6B7FA),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "NOTIFICATION SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFB6B7FA),
                    modifier = Modifier.padding(32.dp)
                )
            } else if (currentUser == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "Please log in to manage notification settings",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Settings Cards
                SettingCard(
                    title = "Event Notifications",
                    description = "Get notified about new upcoming events",
                    checked = preferences.eventsEnabled,
                    onCheckedChange = { enabled ->
                        preferences = preferences.copy(eventsEnabled = enabled)
                        lifecycleScope.launch {
                            notificationManager.updateUserPreferences(preferences)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingCard(
                    title = "News & Announcements",
                    description = "Stay updated with latest news from District 49",
                    checked = preferences.newsEnabled,
                    onCheckedChange = { enabled ->
                        preferences = preferences.copy(newsEnabled = enabled)
                        lifecycleScope.launch {
                            notificationManager.updateUserPreferences(preferences)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingCard(
                    title = "Newsletter Notifications",
                    description = "Get notified when a new newsletter is available",
                    checked = preferences.newsletterEnabled,
                    onCheckedChange = { enabled ->
                        preferences = preferences.copy(newsletterEnabled = enabled)
                        lifecycleScope.launch {
                            notificationManager.updateUserPreferences(preferences)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingCard(
                    title = "Notification Sound",
                    description = "Play sound with notifications",
                    checked = preferences.soundEnabled,
                    onCheckedChange = { enabled ->
                        preferences = preferences.copy(soundEnabled = enabled)
                        lifecycleScope.launch {
                            notificationManager.updateUserPreferences(preferences)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back Button
            Button(
                onClick = onBackPressed,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A4A8B)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "BACK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B6AD0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
fun NotificationSettingsPreview() {
    NotificationSettingsScreenWithNavBar(
        notificationManager = District49NotificationManager.getInstance(),
        onBackPressed = {},
        onMenuClick = {},
        onNotificationsClick = {},
        onProfileClick = {},
        onHomeClick = {}
    )
}