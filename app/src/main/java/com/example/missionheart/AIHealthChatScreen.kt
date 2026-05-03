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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*

/**
 * Sprint 1: Robust UI for Health AI Assistant with History & Vision
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHealthChatScreen(
    navController: NavController, 
    sessionIdArg: String? = null,
    viewModel: AIChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }

    // Sync ViewModel with navigation arguments
    LaunchedEffect(sessionIdArg) {
        if (sessionIdArg != null && sessionIdArg != viewModel.currentSessionId) {
            viewModel.startNewSession(sessionIdArg)
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    // Image Handlers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.selectedImage = bitmap
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.selectedImage = it }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                Toast.makeText(context, "Camera failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = SurfaceWhite) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Previous Chats", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = BrandBlue)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(viewModel.chatSessions) { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                viewModel.startNewSession(session.id)
                                showHistory = false
                            },
                            colors = CardDefaults.cardColors(containerColor = if (viewModel.currentSessionId == session.id) BrandBlue.copy(0.1f) else Color.Transparent),
                            border = if (viewModel.currentSessionId == session.id) BorderStroke(1.dp, BrandBlue) else null
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
                        Icon(Icons.Default.SmartToy, null, tint = BrandBlue)
                        Spacer(Modifier.width(10.dp))
                        Text("Health AI Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.History, null, tint = BrandBlue) }
                    IconButton(onClick = { viewModel.startNewSession(null) }) { Icon(Icons.Default.Add, null, tint = BrandBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.messages) { message -> HealthChatBubble(message) }
                if (viewModel.isTyping) {
                    item { Text("AI is thinking...", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(start = 8.dp)) }
                }
            }

            // Preview selected image
            viewModel.selectedImage?.let {
                Box(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).size(80.dp).clip(RoundedCornerShape(8.dp))) {
                    Image(it.asImageBitmap(), null, modifier = Modifier.fillMaxSize())
                    IconButton(onClick = { viewModel.selectedImage = null }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Surface(tonalElevation = 2.dp, color = SurfaceWhite) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch()
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Icon(Icons.Default.PhotoCamera, null, tint = BrandBlue) }
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) { Icon(Icons.Default.AttachFile, null, tint = BrandBlue) }
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask or describe photo...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = InputFieldBg, focusedTextColor = TextPrimary),
                        maxLines = 3
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || viewModel.selectedImage != null) {
                                viewModel.sendMessage(inputText, viewModel.selectedImage)
                                inputText = ""
                            }
                        },
                        enabled = (inputText.isNotBlank() || viewModel.selectedImage != null) && !viewModel.isTyping,
                        modifier = Modifier.clip(CircleShape).background(if ((inputText.isNotBlank() || viewModel.selectedImage != null) && !viewModel.isTyping) BrandBlue else InputFieldBg)
                    ) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White) }
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
    val shape = if (message.isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = bgColor, shape = shape, tonalElevation = if (message.isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp).clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Health AI", message.text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.image?.let { Image(it.asImageBitmap(), null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))); Spacer(Modifier.height(8.dp)) }
                Text(text = parseMarkdown(message.text), color = textColor, fontSize = 15.sp, lineHeight = 20.sp)
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        text.split("\n").forEachIndexed { index, line ->
            var l = line
            if (l.trimStart().startsWith("* ")) { append("• "); l = l.trimStart().removePrefix("* ") }
            l.split("**").forEachIndexed { i, p ->
                if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p)
            }
            if (index < text.split("\n").size - 1) append("\n")
        }
    }
}
