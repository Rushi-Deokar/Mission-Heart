package com.example.missionheart

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException as KotlinSerializationException

data class HealthChatMessage(
    val text: String,
    val isUser: Boolean,
    val image: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHealthChatScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<HealthChatMessage>() }
    var isTyping by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-3-flash-preview", // Restored Gemini 3 Preview
            apiKey = BuildConfig.GEMINI_API_KEY,
            requestOptions = RequestOptions(apiVersion = "v1beta") // Preview models require v1beta
        )
    }

    // Image Pickers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            selectedImage = bitmap
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { selectedImage = it }
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(HealthChatMessage("Namaste $userName! I'm your MissionHeart Vision AI. You can chat with me or upload photos of medicines/prescriptions for analysis.", false))
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                windowInsets = WindowInsets(0, 0, 0, 0) // Fix top empty space
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    HealthChatBubble(message)
                }
                if (isTyping) {
                    item {
                        Text("AI is analyzing...", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Image Preview (if selected)
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
                    IconButton(onClick = { cameraLauncher.launch() }) {
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
                                messages.add(HealthChatMessage(userMsgText, true, userMsgImg))
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
                                        val aiResponse = response.text ?: "I'm sorry, I couldn't analyze that."
                                        messages.add(HealthChatMessage(aiResponse, false))
                                    } catch (e: KotlinSerializationException) {
                                        Log.e("HealthAI", "Serialization Error", e)
                                        messages.add(HealthChatMessage("AI service returned an unexpected format. This usually happens during API updates. Please try again.", false))
                                    } catch (e: Exception) {
                                        Log.e("HealthAI", "Error", e)
                                        val errorMsg = when {
                                            e.message?.contains("404") == true -> "Error 404: Model not found. We are switching to a stable version."
                                            e.message?.contains("429") == true -> "Too many requests. Please wait a moment."
                                            else -> e.localizedMessage ?: "Unknown connection error"
                                        }
                                        messages.add(HealthChatMessage("Analysis Error: $errorMsg", false))
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
            
            // Handle Bullet points (* )
            if (currentLine.trimStart().startsWith("* ")) {
                val indent = currentLine.takeWhile { it.isWhitespace() }
                append(indent)
                append("• ")
                currentLine = currentLine.trimStart().removePrefix("* ")
            }

            // Handle Bold (**text**)
            val parts = currentLine.split("**")
            parts.forEachIndexed { partIndex, part ->
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
            }
            
            if (index < lines.size - 1) append("\n")
        }
    }
}
