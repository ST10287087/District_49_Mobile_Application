package vcmsa.projects.district49_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.utils.AuthManager

class AdminDashboard : ComponentActivity() {

    private lateinit var adminRepository: AdminRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adminRepository = AdminRepository()

        setContent {
            AdminDashboardScreen(
                adminRepository = adminRepository,
                onBackPressed = { finish() },
                context = this
            )
        }
    }
}

@Composable
fun AdminDashboardScreen(
    adminRepository: AdminRepository,
    onBackPressed: () -> Unit,
    context: AdminDashboard
) {
    var currentGoal by remember { mutableIntStateOf(120000) }
    var currentRaised by remember { mutableIntStateOf(50000) }
    var isLoading by remember { mutableStateOf(false) }

    // PDF picker for newsletters - accepts multiple file types
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.lifecycleScope.launch {
                isLoading = true
                try {
                    val result = adminRepository.uploadNewsletter(it, context)
                    if (result.isSuccess) {
                        val newsletter = result.getOrNull()!!
                        Toast.makeText(
                            context,
                            "Newsletter '${newsletter.title}' sent to subscribers! Notifications sent.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Toast.makeText(context, "Failed to send newsletter: $error", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Load current donation data
    LaunchedEffect(Unit) {
        val donationData = adminRepository.getCurrentDonationGoal()
        donationData.getOrNull()?.let { data ->
            currentGoal = data.goalAmount
            currentRaised = data.raisedAmount
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.home_phonebackground07),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main Content - Always scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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

            // Admin Dashboard Title with new color
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
                    text = "ADMIN DASHBOARD",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Donations Goal Section
            DonationGoalCard(
                currentGoal = currentGoal,
                currentRaised = currentRaised,
                onUpdateGoal = { newGoal ->
                    context.lifecycleScope.launch {
                        isLoading = true
                        val result = adminRepository.updateDonationGoal(newGoal, currentRaised)
                        if (result.isSuccess) {
                            currentGoal = newGoal
                            Toast.makeText(context, "Donation goal updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update goal", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                },
                onUpdateRaised = { newRaised ->
                    context.lifecycleScope.launch {
                        isLoading = true
                        val result = adminRepository.updateDonationGoal(currentGoal, newRaised)
                        if (result.isSuccess) {
                            currentRaised = newRaised
                            Toast.makeText(context, "Raised amount updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update amount", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Newsletter Section
            NewsletterCard(
                onSendNewsletter = {
                    // Accept multiple file types including PDFs, images, documents
                    pdfPicker.launch("*/*")
                },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // News & Announcements Section
            NewsAnnouncementsCard(
                onAddNews = { title, content ->
                    context.lifecycleScope.launch {
                        isLoading = true
                        // 🔔 UPDATED: Pass context to enable notifications
                        val result = adminRepository.addNewsAnnouncement(title, content, context)
                        if (result.isSuccess) {
                            Toast.makeText(context, "News added successfully! Notifications sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to add news", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Back Button - Smaller and darker
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
                    text = "BACK TO HOME",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFB6B7FA),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DonationGoalCard(
    currentGoal: Int,
    currentRaised: Int,
    onUpdateGoal: (Int) -> Unit,
    onUpdateRaised: (Int) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showRaisedDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B6AD0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DONATION GOALS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Goal",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "R${currentGoal.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1 ")}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Amount Raised",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "R${currentRaised.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1 ")}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            val progress = (currentRaised.toFloat() / currentGoal.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons with shadows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Goal", color = Color(0xFF2B6AD0), fontSize = 14.sp)
                }

                Button(
                    onClick = { showRaisedDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Raised", color = Color(0xFF2B6AD0), fontSize = 14.sp)
                }
            }
        }
    }

    // Update Goal Dialog
    if (showGoalDialog) {
        UpdateAmountDialog(
            title = "Update Donation Goal",
            currentAmount = currentGoal,
            onConfirm = { newAmount ->
                onUpdateGoal(newAmount)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }

    // Update Raised Dialog
    if (showRaisedDialog) {
        UpdateAmountDialog(
            title = "Update Raised Amount",
            currentAmount = currentRaised,
            onConfirm = { newAmount ->
                onUpdateRaised(newAmount)
                showRaisedDialog = false
            },
            onDismiss = { showRaisedDialog = false }
        )
    }
}

@Composable
fun NewsletterCard(
    onSendNewsletter: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF99B9DF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEWSLETTER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Send newsletter to all subscribers",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Accepts: PDF, Images, Documents",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSendNewsletter,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF99B9DF)
                        )
                        Text(
                            text = "SENDING...",
                            color = Color(0xFF99B9DF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "SELECT & SEND NEWSLETTER",
                        color = Color(0xFF99B9DF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NewsAnnouncementsCard(onAddNews: (String, String) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFB1D7C0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEWS & ANNOUNCEMENTS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Add news that will appear on the home page",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "ADD NEWS",
                    color = Color(0xFFB1D7C0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAddDialog) {
        AddNewsDialog(
            onConfirm = { title, content ->
                onAddNews(title, content)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun UpdateAmountDialog(
    title: String,
    currentAmount: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Current: R${currentAmount.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1 ")}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("New Amount (R)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountText.toIntOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddNewsDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add News & Announcement") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, content)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}