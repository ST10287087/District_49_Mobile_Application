package vcmsa.projects.district49_android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val jsonFormat = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

val YellowColor = 0xFFFEC637
val RedColor = 0xFFE30133
val BlueColor = 0xFF486CFF
val PinkColor = 0xFFF4C5D5
val LightBlueColor = 0xFF99B9DF
val OrangeColor = 0xFFF85C36
val DarkPinkColor = 0xFFF3A5FB

class EventsPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = R.drawable.eventswallpaper,
                        contentDescription = "Events background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Image(
                        painter = painterResource(R.drawable.final_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(126.dp, 186.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )


                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 32.dp) // Increased top padding to move it down
                            .size(48.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.navbar_arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            colorFilter = ColorFilter.tint(Color.Black) // Changed to Black
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 32.dp, top = 218.dp)
                    ) {
                        Text(
                            text = "EVENTS",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "SCHEDULE",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }

                    DashboardScreen(context = this@EventsPage)

                    // Blue star moved even lower to avoid overlapping with "EVENTS SCHEDULE"
                    AndroidView(
                        factory = { context ->
                            android.view.LayoutInflater.from(context).inflate(R.layout.stars_overlay, null)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = 150.dp) // Moved further down by 150dp
                            .zIndex(2f)
                    )
                }
            }
        }
    }
}

private suspend fun saveTilesToFirestore(tiles: List<TileModel>, firestore: FirebaseFirestore) {
    try {
        val tilesJson = jsonFormat.encodeToString(tiles)
        val data = hashMapOf(
            "tilesJson" to tilesJson,
            "lastUpdated" to System.currentTimeMillis(),
            "totalTiles" to tiles.size
        )
        firestore.collection("eventsConfig").document("tiles").set(data).await()
    } catch (e: Exception) {
    }
}

private suspend fun checkIfAdmin(currentUser: com.google.firebase.auth.FirebaseUser?, firestore: FirebaseFirestore): Boolean {
    currentUser ?: return false
    return try {
        val uid = currentUser.uid
        val doc = firestore.collection("users").document(uid).get().await()
        val role = doc.getString("userRole") ?: ""
        val adminBool = doc.getBoolean("admin") ?: false
        role.equals("admin", ignoreCase = true) || adminBool
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(context: EventsPage) {
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var tiles by remember { mutableStateOf(listOf<TileModel>()) }
    var selectedTile by remember { mutableStateOf<TileModel?>(null) }
    var showEventDialog by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: ""
    val firestore = remember { FirebaseFirestore.getInstance() }

    LaunchedEffect(currentUser) {
        isAdmin = checkIfAdmin(currentUser, firestore)
    }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("eventsConfig").document("tiles").get().await()
            if (snapshot.exists()) {
                val tilesJson = snapshot.getString("tilesJson")
                tilesJson?.let { json ->
                    try {
                        tiles = jsonFormat.decodeFromString<List<TileModel>>(json)
                    } catch (e: Exception) {
                        tiles = defaultTiles()
                    }
                } ?: run {
                    tiles = defaultTiles()
                }
            } else {
                tiles = defaultTiles()
                scope.launch {
                    saveTilesToFirestore(tiles, firestore)
                }
            }
        } catch (e: Exception) {
            tiles = defaultTiles()
        }
    }

    LaunchedEffect(Unit) {
        try {
            firestore.collection("eventsConfig").document("tiles").addSnapshotListener { snapshot, error ->
                error?.let { return@addSnapshotListener }
                snapshot?.let { doc ->
                    if (doc.exists()) {
                        val tilesJson = doc.getString("tilesJson")
                        tilesJson?.let { json ->
                            try {
                                val loadedTiles = jsonFormat.decodeFromString<List<TileModel>>(json)
                                if (loadedTiles != tiles) {
                                    tiles = loadedTiles
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    fun saveTiles(newTiles: List<TileModel>) {
        tiles = newTiles
        scope.launch {
            saveTilesToFirestore(newTiles, firestore)
        }
    }

    Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
        if (isEditing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = 80.dp.toPx()
                val stepY = 80.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.Black.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, size.height))
                    x += stepX
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.Black.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y))
                    y += stepY
                }
            }
        }

        if (tiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events available", fontSize = 24.sp, color = Color.Gray)
            }
        } else {
            tiles.forEach { tile ->
                ResizableDraggableTile(
                    tileModel = tile,
                    isEditing = isEditing,
                    context = context,
                    onTileChange = { updatedTile ->
                        val updatedTiles = tiles.toMutableList()
                        val index = updatedTiles.indexOfFirst { it.id == updatedTile.id }
                        if (index != -1) {
                            updatedTiles[index] = updatedTile
                            saveTiles(updatedTiles)
                        }
                    },
                    onDelete = { deleteTile ->
                        val updatedTiles = tiles.filter { it.id != deleteTile.id }
                        saveTiles(updatedTiles)
                    },
                    onClick = { tile ->
                        if (!isEditing) {
                            selectedTile = tile
                            showEventDialog = true
                        }
                    }
                )
            }
        }

        if (showEventDialog && selectedTile != null) {
            EventDetailsDialog(
                tile = selectedTile!!,
                onDismiss = { showEventDialog = false }
            )
        }

        if (isAdmin) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.size(56.dp).background(
                        color = if (isEditing) Color(RedColor) else Color(YellowColor),
                        shape = RoundedCornerShape(16.dp)
                    )
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Exit Edit Mode" else "Enter Edit Mode",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isEditing) {
                    IconButton(
                        onClick = {
                            val newTile = TileModel(
                                id = "tile_${System.currentTimeMillis()}",
                                title = "New Event",
                                subtitle = "Brief description",
                                description = "Detailed event description goes here...",
                                date = System.currentTimeMillis(),
                                offsetX = 100f,
                                offsetY = 100f,
                                width = 220f,
                                height = 180f,
                                colorHex = BlueColor,
                                createdBy = userEmail
                            )
                            saveTiles(tiles + newTile)
                        },
                        modifier = Modifier.size(56.dp).background(
                            color = Color(LightBlueColor),
                            shape = RoundedCornerShape(16.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Tile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsDialog(tile: TileModel, onDismiss: () -> Unit) {
    var currentDate by remember { mutableStateOf(tile.date) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(tile.colorHex),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tile.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Event Date:", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentDate)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DatePicker(state = datePickerState, showModeToggle = false, title = null, modifier = Modifier.height(400.dp))
                }

                LaunchedEffect(datePickerState.selectedDateMillis) {
                    datePickerState.selectedDateMillis?.let { currentDate = it }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Location:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 4.dp))
                Text(tile.subtitle, fontSize = 15.sp, color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 12.dp))
                Text("Description:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 4.dp))
                Text(tile.description, fontSize = 15.sp, color = Color.Black.copy(alpha = 0.8f), lineHeight = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                if (tile.createdBy.isNotEmpty()) {
                    Text("Created by: ${tile.createdBy}", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got it!", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizableDraggableTile(
    tileModel: TileModel,
    isEditing: Boolean,
    context: EventsPage,
    onTileChange: (TileModel) -> Unit,
    onDelete: (TileModel) -> Unit,
    onClick: (TileModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTile by remember { mutableStateOf(tileModel) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(tileModel) {
        if (!isDragging) {
            currentTile = tileModel
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(currentTile.offsetX.roundToInt(), currentTile.offsetY.roundToInt()) }
            .size(currentTile.width.dp, currentTile.height.dp)
            .background(Color(currentTile.colorHex), shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(if (isEditing) 2.dp else 1.dp, Color.Black), shape = RoundedCornerShape(16.dp))
            .pointerInput(isEditing) {
                if (isEditing) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentTile = currentTile.copy(
                                offsetX = currentTile.offsetX + dragAmount.x,
                                offsetY = currentTile.offsetY + dragAmount.y
                            )
                        },
                        onDragEnd = { isDragging = false; onTileChange(currentTile) },
                        onDragCancel = { isDragging = false }
                    )
                }
            }
            .clickable(enabled = !isEditing, onClick = { onClick(currentTile) })
    ) {
        if (isEditing) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp).size(24.dp)
                    .background(Color(RedColor), shape = RoundedCornerShape(4.dp))
                    .clickable { onDelete(currentTile) }.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopStart).offset(x = 8.dp, y = 8.dp)
                .size(width = 28.dp, height = 12.dp)
                .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(50)).padding(1.dp)
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).offset(x = 4.dp).size(6.dp)
                    .background(Color.Black, shape = RoundedCornerShape(50))
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 32.dp, end = 8.dp, bottom = 8.dp)
                .clickable(enabled = isEditing) { if (isEditing) { showEditDialog = true } },
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(currentTile.title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(currentTile.subtitle, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = dateFormatter.format(Date(currentTile.date)), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        if (isEditing) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(16.dp)
                    .background(Color(BlueColor), shape = RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentTile = currentTile.copy(
                                    width = (currentTile.width + dragAmount.x).coerceAtLeast(120f),
                                    height = (currentTile.height + dragAmount.y).coerceAtLeast(100f)
                                )
                            },
                            onDragEnd = { isDragging = false; onTileChange(currentTile) }
                        )
                    }
            )
        }

        if (showEditDialog && isEditing) {
            TileEditDialog(
                tileModel = currentTile,
                context = context,
                onSave = { updatedTile ->
                    currentTile = updatedTile
                    onTileChange(updatedTile)
                    showEditDialog = false
                },
                onDelete = { onDelete(currentTile); showEditDialog = false },
                onDismiss = { showEditDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileEditDialog(
    tileModel: TileModel,
    context: EventsPage,
    onSave: (TileModel) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(tileModel.title) }
    var subtitle by remember { mutableStateOf(tileModel.subtitle) }
    var description by remember { mutableStateOf(tileModel.description) }
    var date by remember { mutableStateOf(tileModel.date) }
    var colorHex by remember { mutableStateOf(tileModel.colorHex) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event Details", color = Color.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Location/Short Info", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detailed Description", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Event Date:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dateFormatter.format(Date(date)), modifier = Modifier.weight(1f), color = Color.Black)
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(BlueColor))
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select Date", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Choose color:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(YellowColor, RedColor, BlueColor, PinkColor, LightBlueColor, OrangeColor, DarkPinkColor).forEach { color ->
                        Box(
                            modifier = Modifier.size(24.dp).background(Color(color), shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(if (colorHex == color) 2.dp else 1.dp, Color.Black), shape = RoundedCornerShape(4.dp))
                                .clickable { colorHex = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isSaving = true
                    val updatedTile = tileModel.copy(
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        date = date,
                        colorHex = colorHex
                    )

                    context.lifecycleScope.launch {
                        try {
                            onSave(updatedTile)

                            withContext(Dispatchers.IO) {
                                try {
                                    val notificationManager = District49NotificationManager.getInstance()
                                    val formattedDate = dateFormatter.format(Date(date))

                                    val result = notificationManager.notifyAllUsersWithPreference(
                                        context,
                                        NotificationType.EVENT,
                                        "📅 New Event: $title",
                                        "Join us at $subtitle on $formattedDate. ${description.take(80)}${if (description.length > 80) "..." else ""}"
                                    )

                                    if (result.isSuccess) {
                                        val count = result.getOrNull() ?: 0
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "✅ Event saved! Notification sent to $count users.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Event saved, but notification failed to send",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Event saved successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error saving event: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(BlueColor))
            ) {
                if (isSaving) {
                    Text("Saving...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(RedColor))
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showDatePicker) {
        JetpackDatePickerDialog(
            initialDate = date,
            onDateSelected = { selectedDate ->
                date = selectedDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JetpackDatePickerDialog(
    initialDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate,
        initialDisplayMode = DisplayMode.Picker
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it)
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(BlueColor))
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(RedColor))
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = {
                Text("Select Event Date", modifier = Modifier.padding(bottom = 8.dp))
            }
        )
    }
}

fun defaultTiles() = listOf(
    TileModel(
        "tile1", "SUNDAY MARKET", "123 Anywhere Rd, Durban, KZN",
        "Weekly market with fresh produce, crafts, and local delicacies. Open from 8AM to 2PM every Sunday.",
        System.currentTimeMillis() + 86400000,
        100f, 100f, 220f, 180f, YellowColor, ""
    ),
    TileModel(
        "tile2", "FOOD FEST", "456 Beach Rd, Cape Town",
        "Annual food festival featuring top chefs, cooking demonstrations, and food from around the world.",
        System.currentTimeMillis() + 172800000,
        350f, 100f, 220f, 180f, RedColor, ""
    ),
    TileModel(
        "tile3", "MUSIC NIGHT", "789 Hill St, Joburg",
        "Live music performance with local bands and artists. Drinks and snacks available. 18+ event.",
        System.currentTimeMillis() + 259200000,
        100f, 300f, 220f, 180f, BlueColor, ""
    )
)

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        // DashboardScreen preview requires context, so omitted
    }
}