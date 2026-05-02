package com.example.missionheart

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.serialization.SerializationException as KotlinSerializationException

data class HealthChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val image: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHealthChatScreen(navController: NavController, sessionIdArg: String? = null) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"
    val userId = currentUser?.uid ?: "anonymous"

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var currentSessionId by remember { mutableStateOf(sessionIdArg ?: UUID.randomUUID().toString()) }
    var showHistory by remember { mutableStateOf(false) }
    var chatSessions = remember { mutableStateListOf<ChatSession>() }

    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<HealthChatMessage>() }
    var isTyping by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY,
            requestOptions = RequestOptions(apiVersion = "v1beta")
        )
    }

    // Load Sessions List
    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("chat_sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    chatSessions.clear()
                    for (doc in it.documents) {
                        chatSessions.add(ChatSession(
                            id = doc.id,
                            title = doc.getString("title") ?: "Untitled Chat",
                            timestamp = doc.getLong("timestamp") ?: 0
                        ))
                    }
                }
            }
    }

    // Load Messages for Current Session
    LaunchedEffect(currentSessionId) {
        db.collection("users").document(userId).collection("chat_sessions")
            .document(currentSessionId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                messages.clear()
                if (snapshot.isEmpty) {
                    messages.add(HealthChatMessage(text = "Namaste $userName! I'm your MissionHeart Vision AI. How can I help you today?", isUser = false))
                } else {
                    for (doc in snapshot.documents) {
                        messages.add(HealthChatMessage(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            isUser = doc.getBoolean("isUser") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0
                        ))
                    }
                }
            }
    }

    // Function to save message to Firestore
    fun saveMessage(message: HealthChatMessage) {
        val sessionRef = db.collection("users").document(userId).collection("chat_sessions").document(currentSessionId)
        
        // Update session metadata (title if it's the first message)
        if (messages.size <= 2) { // Greeting + User's first message
            sessionRef.set(hashMapOf(
                "title" to if (message.isUser) message.text.take(30) + "..." else "New Health Consultation",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        val messageData = hashMapOf(
            "text" to message.text,
            "isUser" to message.isUser,
            "timestamp" to message.timestamp
        )
        
        sessionRef.collection("messages").add(messageData)
    }

    // Image Pickers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                selectedImage = bitmap
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { selectedImage = it }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch() else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = SurfaceWhite
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Your Previous Chats", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = BrandBlue)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(chatSessions) { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                currentSessionId = session.id
                                showHistory = false
                            },
                            colors = CardDefaults.cardColors(containerColor = if (currentSessionId == session.id) BrandBlue.copy(0.1f) else Color.Transparent),
                            border = if (currentSessionId == session.id) BorderStroke(1.dp, BrandBlue) else null
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = BrandBlue)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Health AI Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = BrandBlue)
                    }
                    IconButton(onClick = {
                        currentSessionId = UUID.randomUUID().toString()
                        messages.clear()
                        Toast.makeText(context, "New Chat Started", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = BrandBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    HealthChatBubble(message)
                }
                if (isTyping) {
                    item {
                        Text("AI is thinking...", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Image Preview
            selectedImage?.let {
                Box(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).size(80.dp).clip(RoundedCornerShape(8.dp))) {
                    Image(it.asImageBitmap(), null, modifier = Modifier.fillMaxSize())
                    IconButton(onClick = { selectedImage = null }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Input Area
            Surface(tonalElevation = 2.dp, color = SurfaceWhite) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.PhotoCamera, null, tint = BrandBlue)
                    }
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AttachFile, null, tint = BrandBlue)
                    }
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask or describe photo...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = InputFieldBg,
                            focusedTextColor = TextPrimary
                        ),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImage != null) {
                                val userMsgText = inputText
                                val userMsgImg = selectedImage
                                val userMsg = HealthChatMessage(text = userMsgText, isUser = true, image = userMsgImg)
                                messages.add(userMsg)
                                saveMessage(userMsg)
                                
                                inputText = ""
                                selectedImage = null
                                isTyping = true
                                
                                scope.launch {
                                    try {
                                        val inputContent = content {
                                            userMsgImg?.let { image(it) }
                                            text("User: $userName\nContext: Medical Assistant\nQuestion: $userMsgText")
                                        }
                                        val response = generativeModel.generateContent(inputContent)
                                        val aiResponseText = response.text ?: "I couldn't analyze that."
                                        val aiMsg = HealthChatMessage(text = aiResponseText, isUser = false)
                                        messages.add(aiMsg)
                                        saveMessage(aiMsg)
                                    } catch (e: Exception) {
                                        Log.e("HealthAI", "Error", e)
                                        messages.add(HealthChatMessage(text = "Error: ${e.localizedMessage}", isUser = false))
                                    } finally {
                                        isTyping = false
                                    }
                                }
                            }
                        },
                        enabled = (inputText.isNotBlank() || selectedImage != null) && !isTyping,
                        modifier = Modifier.clip(CircleShape).background(if ((inputText.isNotBlank() || selectedImage != null) && !isTyping) BrandBlue else InputFieldBg)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthChatBubble(message: HealthChatMessage) {
    val context = LocalContext.current
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) BrandBlue else SurfaceWhite
    val textColor = if (message.isUser) Color.White else TextPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = shape,
            tonalElevation = if (message.isUser) 0.dp else 2.dp,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Health AI Response", message.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                message.image?.let {
                    Image(it.asImageBitmap(), null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = parseMarkdown(message.text),
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            if (currentLine.trimStart().startsWith("* ")) {
                val indent = currentLine.takeWhile { it.isWhitespace() }
                append(indent)
                append("• ")
                currentLine = currentLine.trimStart().removePrefix("* ")
            }
            val parts = currentLine.split("**")
            parts.forEachIndexed { partIndex, part ->
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
                } else { append(part) }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}
