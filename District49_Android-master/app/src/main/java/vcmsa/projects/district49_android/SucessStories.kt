package vcmsa.projects.district49_android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.models.SuccessStory
import vcmsa.projects.district49_android.utils.AuthManager
import kotlin.math.absoluteValue

class SuccessStories : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SuccessStoriesScreen(
                onBackPressed = { finish() },
                context = this
            )
        }
    }
}

@Composable
fun SuccessStoriesScreen(onBackPressed: () -> Unit, context: SuccessStories) {
    val repository = remember { SuccessStoryRepository() }
    val authManager = remember { AuthManager.getInstance() }

    var stories by remember { mutableStateOf<List<SuccessStory>>(emptyList()) }
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isSheetExpanded by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val collapsedHeight = 200.dp
    val expandedHeight = screenHeight - 100.dp

    val sheetHeight by animateDpAsState(
        targetValue = if (isSheetExpanded) expandedHeight else collapsedHeight,
        animationSpec = tween(300),
        label = "sheet_height"
    )

    val collapsedTopPosition = screenHeight - collapsedHeight - 20.dp
    val expandedTopPosition = 180.dp

    val cardTopPosition by animateDpAsState(
        targetValue = if (isSheetExpanded) expandedTopPosition else collapsedTopPosition,
        animationSpec = tween(300),
        label = "card_position"
    )

    val verticalDragOffset = remember { mutableFloatStateOf(0f) }
    val horizontalDragOffset = remember { mutableFloatStateOf(0f) }

    // Check admin status
    LaunchedEffect(Unit) {
        val isAdminResult = authManager.isCurrentUserAdmin()
        isAdmin = isAdminResult.getOrDefault(false)
    }

    // Load stories
    LaunchedEffect(Unit) {
        context.lifecycleScope.launch {
            val result = repository.getAllStories()
            stories = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    val currentStory = if (stories.isNotEmpty() && currentStoryIndex < stories.size) {
        stories[currentStoryIndex]
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background logo
        Image(
            painter = painterResource(id = R.drawable.successstoriesbackgroundlogo),
            contentDescription = null,
            modifier = Modifier
                .width(413.dp)
                .height(437.dp)
                .align(Alignment.TopStart),
            contentScale = ContentScale.Fit
        )

        // Logo at top
        Image(
            painter = painterResource(id = R.drawable.final_logo),
            contentDescription = "District 49 Logo",
            modifier = Modifier
                .width(120.dp)
                .height(100.dp)
                .align(Alignment.TopCenter)
                .offset(y = 50.dp)
        )

        // Success Stories logo
        Image(
            painter = painterResource(id = R.drawable.successstorieslogo),
            contentDescription = "Success Stories Logo",
            modifier = Modifier
                .offset(x = 70.dp, y = 160.dp)
                .width(270.dp)
                .height(95.dp)
        )

        // Navigation Arrows (only show when not expanded and multiple stories exist)
        if (!isSheetExpanded && stories.size > 1 && currentStory != null) {
            // Left Arrow
            if (currentStoryIndex > 0) {
                IconButton(
                    onClick = { currentStoryIndex-- },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                        .offset(y = (-40).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.navbar_arrow_back),
                        contentDescription = "Previous Story",
                        tint = Color(0xFF28348D),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Right Arrow
            if (currentStoryIndex < stories.size - 1) {
                IconButton(
                    onClick = { currentStoryIndex++ },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp)
                        .offset(y = (-40).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.navbar_arrow_back),
                        contentDescription = "Next Story",
                        tint = Color(0xFF28348D),
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(rotationZ = 180f)
                    )
                }
            }
        }

        // Story Card
        if (currentStory != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = cardTopPosition)
                    .height(sheetHeight)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (!isSheetExpanded && horizontalDragOffset.floatValue.absoluteValue > 100) {
                                    if (horizontalDragOffset.floatValue < 0 && currentStoryIndex < stories.size - 1) {
                                        currentStoryIndex++
                                    } else if (horizontalDragOffset.floatValue > 0 && currentStoryIndex > 0) {
                                        currentStoryIndex--
                                    }
                                }
                                horizontalDragOffset.floatValue = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (!isSheetExpanded) {
                                    horizontalDragOffset.floatValue += dragAmount
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (verticalDragOffset.floatValue < -100 && !isSheetExpanded) {
                                    isSheetExpanded = true
                                } else if (verticalDragOffset.floatValue > 100 && isSheetExpanded) {
                                    isSheetExpanded = false
                                }
                                verticalDragOffset.floatValue = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDragOffset.floatValue += dragAmount
                            }
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF28348D)
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(2.dp, Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }

                    // Profile Image (Anonymous or uploaded)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                    ) {
                        if (currentStory.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = currentStory.imageUrl,
                                contentDescription = "Story author",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Anonymous icon
                            Image(
                                painter = painterResource(id = R.drawable.generic_avatar),
                                contentDescription = "Anonymous",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Name
                    Text(
                        text = "${currentStory.name}'s Story",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // Story indicator
                    Text(
                        text = "${currentStoryIndex + 1} of ${stories.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    if (isSheetExpanded) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Story text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = currentStory.storyParts.joinToString("\n\n"),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Text(
                            text = "↓ Pull down to close",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    } else {
                        // Preview
                        Text(
                            text = currentStory.storyParts.firstOrNull() ?: "",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "↑ Pull up to read full story",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "← Swipe to change stories →",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (!isLoading) {
            // No stories message
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No success stories yet.\n${if (isAdmin) "Tap + to add one!" else "Check back soon!"}",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF28348D)
            )
        }

        // Admin buttons
        if (isAdmin && !isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete button (only if stories exist)
                if (stories.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showDeleteConfirm = true },
                        containerColor = Color.Red.copy(alpha = 0.9f),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.meettheteam_trash),
                            contentDescription = "Delete Story",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Add button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF28348D),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Back button
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp)
                .background(
                    Color.White.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.navbar_arrow_back),
                contentDescription = "Back",
                tint = Color(0xFF28348D)
            )
        }
    }

    // Add Story Dialog
    if (showAddDialog) {
        AddStoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, storyText, imageUri ->
                context.lifecycleScope.launch {
                    isLoading = true
                    val storyParts = storyText.split("\n\n").filter { it.isNotBlank() }
                    val result = repository.addStory(name, storyParts, imageUri, context)
                    if (result.isSuccess) {
                        // Reload stories
                        val updatedStories = repository.getAllStories()
                        stories = updatedStories.getOrDefault(emptyList())
                        currentStoryIndex = 0
                        showAddDialog = false
                    }
                    isLoading = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && currentStory != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "Delete Story?",
                    color = Color(0xFF1976D2)
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ${currentStory.name}'s story? This cannot be undone.",
                    color = Color(0xFF424242)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.lifecycleScope.launch {
                            isLoading = true
                            val result = repository.deleteStory(currentStory.id, currentStory.imageUrl)
                            if (result.isSuccess) {
                                val updatedStories = repository.getAllStories()
                                stories = updatedStories.getOrDefault(emptyList())
                                currentStoryIndex = 0
                            }
                            showDeleteConfirm = false
                            isLoading = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1976D2))
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun AddStoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var storyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Success Story",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    IconButton(onClick = onDismiss) {
                        Text(
                            text = "✕",
                            fontSize = 24.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name field
                    Text(
                        text = "Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Enter name or 'Anonymous'", color = Color(0xFF9E9E9E)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            cursorColor = Color(0xFF1976D2)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image picker button
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedImageUri != null) Color(0xFF4CAF50) else Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (selectedImageUri != null) "✓ Image Selected" else "📷 Add Photo (Optional)",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Story text field
                    Text(
                        text = "Story Text",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        placeholder = { Text("Write the success story here...", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        maxLines = Int.MAX_VALUE,
                        minLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121),
                            cursorColor = Color(0xFF1976D2)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instructions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 ",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Tip: Separate paragraphs with two blank lines (press Enter twice)",
                            fontSize = 12.sp,
                            color = Color(0xFF1976D2),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action buttons at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF757575)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && storyText.isNotBlank()) {
                                onConfirm(name, storyText, selectedImageUri)
                            }
                        },
                        enabled = name.isNotBlank() && storyText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            disabledContainerColor = Color(0xFFBDBDBD)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Story", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}