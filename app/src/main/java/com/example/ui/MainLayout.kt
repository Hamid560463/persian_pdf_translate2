package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.TranslationHistory
import com.example.ui.theme.*

@Composable
fun MainLayout(
    translationViewModel: TranslationViewModel,
    historyViewModel: HistoryViewModel,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var showReaderView by remember { mutableStateOf(false) }
    var showSecretsDialog by remember { mutableStateOf(false) }

    val pdfUriState by translationViewModel.pdfUri.collectAsState()

    if (showSecretsDialog) {
        SecretsPanelDialog(
            onDismiss = { showSecretsDialog = false },
            viewModel = translationViewModel
        )
    }

    // Immersive Main Screen
    Surface(
        modifier = Modifier.fillMaxSize().testTag("main_surface"),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showReaderView && pdfUriState != null) {
            // Fullscreen specialized interactive PDF PDFReader
            PdfReaderScreen(
                viewModel = translationViewModel,
                historyViewModel = historyViewModel,
                onClose = { showReaderView = false },
                onOpenSecrets = { showSecretsDialog = true }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_navigation")
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Translator") },
                            label = { Text("Translator") },
                            modifier = Modifier.testTag("tab_translator")
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            icon = { Icon(Icons.Default.Menu, contentDescription = "PDF Files") },
                            label = { Text("PDF Translate") },
                            modifier = Modifier.testTag("tab_pdf")
                        )
                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            icon = { Icon(Icons.Default.List, contentDescription = "History") },
                            label = { Text("History Archive") },
                            modifier = Modifier.testTag("tab_history")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (activeTab) {
                        0 -> TextTranslatorScreen(
                            viewModel = translationViewModel,
                            historyViewModel = historyViewModel,
                            currentThemeMode = currentThemeMode,
                            onThemeChange = onThemeChange,
                            onOpenSecrets = { showSecretsDialog = true }
                        )
                        1 -> PdfDashboardScreen(
                            viewModel = translationViewModel,
                            onOpenReader = { showReaderView = true },
                            currentThemeMode = currentThemeMode,
                            onThemeChange = onThemeChange,
                            onOpenSecrets = { showSecretsDialog = true }
                        )
                        2 -> HistoryScreen(historyViewModel, translationViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TextTranslatorScreen(
    viewModel: TranslationViewModel,
    historyViewModel: HistoryViewModel,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit,
    onOpenSecrets: () -> Unit
) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            statusMessage = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Section with elegant theme gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Persian PDF Translate",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Powered by Google Gemini AI Engine",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key (Secrets Panel) setup row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyConfigured = viewModel.getCustomApiKey().isNotEmpty()
            
            // Status Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (keyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (keyConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (keyConfigured) "کلید اختصاصی فعال است" else "استفاده از کلید اشتراکی پیش‌فرض",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (keyConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            // Trigger Button
            Button(
                onClick = onOpenSecrets,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Secrets Panel (کلید API)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // English Text Input Surface
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "English Text Source",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.inputText.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.inputText.value = it },
                    placeholder = { Text("Type or paste any English text to translate...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("english_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Bar
        Button(
            onClick = { viewModel.translateText(historyViewModel) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("translate_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, contentDescription = "Translate")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Translate to Farsi (Persian)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // State Feedback Visuals
        when (val state = uiState) {
            is UiState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Translating using Gemini AI...",
                        fontWeight = FontWeight.Medium,
                        color = CharcoalColor()
                    )
                }
            }
            is UiState.Error -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFBEBEB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.message.contains("API key", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenSecrets,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنظیم کلید (Secrets Panel)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        // Persian Translated Card
        if (translatedText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translated_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ترجمه فارسی (Persian Translation)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )

                        Row {
                            IconButton(onClick = {
                                viewModel.speak(translatedText) { statusMessage = it }
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Speak Out Loud")
                            }
                            IconButton(onClick = {
                                viewModel.stopSpeaking()
                                statusMessage = "Stopped speech"
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Stop Speech")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = translatedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SandColor(), shape = RoundedCornerShape(8.dp))
                                .padding(16.dp)
                                .testTag("persian_text_view"),
                            style = TextStyle(
                                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl,
                                lineHeight = 28.sp,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                             ),
                            color = CharcoalColor()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary actions: Copy, Share, Export
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Persian Translation", translatedText)
                            clipboard.setPrimaryClip(clip)
                            statusMessage = "Translation copied to clipboard!"
                        }) {
                            Text("Copy", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, translatedText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share translation via"))
                        }) {
                            Text("Share", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = {
                            val pdfUri = viewModel.exportToPdf()
                            if (pdfUri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open translated PDF"))
                            } else {
                                statusMessage = "Could not generate PDF"
                            }
                        }) {
                            Text("Export PDF", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PersianTranslationGuide()
        Spacer(modifier = Modifier.height(12.dp))
        PersianAboutAppCard(currentThemeMode = currentThemeMode, onThemeChange = onThemeChange)
    }
}

@Composable
fun PdfDashboardScreen(
    viewModel: TranslationViewModel,
    onOpenReader: () -> Unit,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit,
    onOpenSecrets: () -> Unit
) {
    val context = LocalContext.current
    val pdfUriState by viewModel.pdfUri.collectAsState()
    val pdfNameState by viewModel.pdfName.collectAsState()
    val pageCountState by viewModel.pageCount.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadPdf(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic PDF translation introduction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Persian PDF Scanner",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upload any English PDF file to examine layout, extract text visually, and translate to literary Persian side by side.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key (Secrets Panel) setup row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyConfigured = viewModel.getCustomApiKey().isNotEmpty()
            
            // Status Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (keyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (keyConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (keyConfigured) "کلید اختصاصی فعال است" else "استفاده از کلید اشتراکی پیش‌فرض",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (keyConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            // Trigger Button
            Button(
                onClick = onOpenSecrets,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Secrets Panel (کلید API)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (pdfUriState == null) {
            // Empty Upload State
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) }
                    .testTag("upload_zone_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Upload Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        "No PDF File Loaded",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Tap here to browse and select any PDF document from your device storage, download folder, or Google Drive.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "امکان انتخاب مستقیم فایل از حافظه داخلی، پوشه دانلودها و گوگل درایو",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // PDF File Info Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Document Selected",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "PDF document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = pdfNameState,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$pageCountState Pages Available",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Button(
                        onClick = onOpenReader,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_reader_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Interactive Reader", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Select Different PDF File", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PersianTranslationGuide()
        Spacer(modifier = Modifier.height(12.dp))
        PersianAboutAppCard(currentThemeMode = currentThemeMode, onThemeChange = onThemeChange)
    }
}

@Composable
fun PdfReaderScreen(
    viewModel: TranslationViewModel,
    historyViewModel: HistoryViewModel,
    onClose: () -> Unit,
    onOpenSecrets: () -> Unit
) {
    val context = LocalContext.current
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPageBitmap by viewModel.currentPageBitmap.collectAsState()
    val pageTranslations by viewModel.pageTranslations.collectAsState()
    val pdfTranslationState by viewModel.pdfTranslationState.collectAsState()
    val pdfName by viewModel.pdfName.collectAsState()

    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            statusMessage = ""
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        IconButton(onClick = onOpenSecrets) {
                            Icon(Icons.Default.Lock, contentDescription = "Secrets Panel", tint = Color.White)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pdfName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                        Text(
                            text = "Page ${currentPageIndex + 1} of $pageCount",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = {
                        val txtUri = viewModel.exportToTxt()
                        if (txtUri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, txtUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share current PDF translations"))
                        } else {
                            statusMessage = "No pages translated yet!"
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share All", tint = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.prevPage() },
                        enabled = currentPageIndex > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val pdfUri = viewModel.exportToPdf()
                                if (pdfUri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export Translation PDF"))
                                } else {
                                    statusMessage = "No content translated to export. Click Translate Page first."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Export PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.nextPage() },
                        enabled = currentPageIndex < pageCount - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    ) { paddingValues ->
        // Split-screen implementation (Adaptive vertical layout for mobile screens)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SandColor())
        ) {
            // Original PDF image (Upper/Top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0E1114))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentPageBitmap != null) {
                    Image(
                        bitmap = currentPageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF original",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(color = Color.LightGray, thickness = 2.dp)

            // Farsi translation space (Lower/Bottom half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                val currentTranslation = pageTranslations[currentPageIndex]

                if (currentTranslation != null) {
                    // Translated Screen (Show Arabic Script)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Persian Translation (Page ${currentPageIndex + 1})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Row {
                                IconButton(onClick = {
                                    viewModel.speak(currentTranslation) { statusMessage = it }
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Read aloud")
                                }
                                IconButton(onClick = {
                                    viewModel.stopSpeaking()
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Stop speech")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = currentTranslation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SandColor(), shape = RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                style = TextStyle(
                                    textDirection = androidx.compose.ui.text.style.TextDirection.Rtl,
                                    lineHeight = 28.sp,
                                    fontSize = 16.sp
                                ),
                                color = CharcoalColor()
                            )
                        }
                    }
                } else {
                    // Empty or loading state for translation
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (pdfTranslationState) {
                            is UiState.Loading -> {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Gemini AI is examining English layout...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Translating visuals directly into Persian...",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            is UiState.Error -> {
                                Text(
                                    text = (pdfTranslationState as UiState.Error).message,
                                    color = Color.Red,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.translateCurrentPdfPage(historyViewModel) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Retry Translation")
                                    }
                                    val errMessage = (pdfTranslationState as UiState.Error).message
                                    if (errMessage.contains("API key", ignoreCase = true)) {
                                        Button(
                                            onClick = onOpenSecrets,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تنظیم کلید API", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Visual Translate Icon",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Page translation is not initiated",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "We use direct AI visual OCR scan to preserve lines.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.translateCurrentPdfPage(historyViewModel) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Translate Page With Gemini AI")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    translationViewModel: TranslationViewModel
) {
    val historyList by historyViewModel.allHistory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var viewingItem by remember { mutableStateOf<TranslationHistory?>(null) }

    val filteredList = historyList.filter {
        it.sourceName.contains(searchQuery, ignoreCase = true) ||
                it.originalText.contains(searchQuery, ignoreCase = true) ||
                it.translatedText.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Translation History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (filteredList.isNotEmpty()) {
                            IconButton(onClick = { historyViewModel.clearAllHistory() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear All Archives", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search through history...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (filteredList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "History empty icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No archives available",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Your translations will appear here.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { history ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingItem = history },
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = history.sourceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = history.translatedText,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { historyViewModel.deleteHistory(history) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail dialog
    viewingItem?.let { item ->
        Dialog(onDismissRequest = { viewingItem = null }) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = item.sourceName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Original / Source:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.originalText,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Translated Persian:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = item.translatedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SandColor(), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            style = TextStyle(
                                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl,
                                lineHeight = 26.sp,
                                fontSize = 16.sp
                            ),
                            color = CharcoalColor()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            translationViewModel.inputText.value = item.originalText
                            translationViewModel.translatedText.value = item.translatedText
                            viewingItem = null
                        }) {
                            Text("Use in Translator", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { viewingItem = null }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Colors helper to support solid contrasts across Light & Dark Theme modes
@Composable
fun SandColor() = if (MaterialTheme.colorScheme.background.red < 0.3f) Color(0xFF15191E) else Color(0xFFF1F3F9)

@Composable
fun CharcoalColor() = if (MaterialTheme.colorScheme.background.red < 0.3f) Color(0xFFE2E2E6) else Color(0xFF191C20)

@Composable
fun PersianTranslationGuide() {
    var isExpanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("persian_guide_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "راهنما",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "راهنمای کامل استفاده از مترجم هوشمند",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = if (isExpanded) "بستن راهنما ▲" else "مشاهده راهنما ▼",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )

                        GuideStepItem(
                            icon = Icons.Default.Edit,
                            title = "۱. ترجمه مستقیم متون و مقالات انگلیسی",
                            description = "متون مورد نظر خود را کپی کرده و در بخش 'مترجم متن' قرار دهید. با فشردن دکمه ترجمه، متون توسط موتور هوش مصنوعی جمینی به فارسی روان ترجمه شده و همچنین قابلیت ارسال، کپی سریع یا خروجی PDF جدید را داراست."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        GuideStepItem(
                            icon = Icons.Default.Menu,
                            title = "۲. کتابخوان و مفسر پیشرفته فایل‌های PDF",
                            description = "فایل‌های کتاب یا اسناد PDF انگلیسی خود را بدون محدودیت از پوشه دانلودها یا حافظه دستگاه باز کنید. کتابخوان دو پنله به صورت هوشمند متن انگلیسی را استخراج کرده و ترجمه فارسی آن را در کنار هر صفحه به شکلی کاملاً جذاب و همزمان نمایش می‌دهد."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        GuideStepItem(
                            icon = Icons.Default.List,
                            title = "۳. تاریخچه اتوماتیک آفلاین و مدیریت آرشیو",
                            description = "تمام ترجمه‌های گرانبهای شما به محض اتمام به طور خودکار در آرشیو محلی تلفن همراه شما ثبت می‌شوند تا در مراجعات بعدی کاملاً رایگان، بدون اینترنت و به صورت سریع از بخش آرشیو در دسترس باشند."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        GuideStepItem(
                            icon = Icons.Default.Warning,
                            title = "۴. نیاز به فیلترشکن (VPN) برای دسترسی به گوگل",
                            description = "از آنجایی که سرورهای هوش مصنوعی شرکت گوگل (Gemini API) ایران را محدود کرده‌اند، برای ترجمه کردن متون و فایل‌های جدید حتماً باید فیلترشکن (VPN) خود را روشن کنید تا نرم‌افزار بتواند به درستی با سرور ارتباط برقرار کند."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        GuideStepItem(
                            icon = Icons.Default.Lock,
                            title = "۵. عملکرد نرم‌افزار در زمان قطع اینترنت (آفلاین)",
                            description = "در حالت آفلاین و قطع اینترنت، امکان ثبت ترجمهٔ گویای جدید وجود ندارد؛ اما نگران نباشید! شما می‌توانید به تمام فایل‌ها، اسناد و متون ترجمه‌شدهٔ قبلی خود به صورت ۱۰۰ درصد آفلاین، رایگان و بدون نیاز به هیچ اتصالی در زبانهٔ آرشیو دسترسی کامل داشته باشید."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideStepItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PersianAboutAppCard(
    currentThemeMode: String,
    onThemeChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("persian_about_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "درباره ما",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "در مورد نرم افزار",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Text(
                        text = if (isExpanded) "بستن جزئیات ▲" else "مشاهده جزئیات ▼",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Choice of App Theme (Dark/Light/System)
                        Text(
                            text = "پوسته نرم‌افزار:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val themeOptions = listOf(
                                Triple("light", "روشن ☀️", "light_btn"),
                                Triple("dark", "تاریک 🌙", "dark_btn"),
                                Triple("system", "سیستم ⚙️", "system_btn")
                            )
                            
                            themeOptions.forEach { (modeKey, modeTitle, testTagSuffix) ->
                                val isSelected = currentThemeMode == modeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else Color.Transparent
                                        )
                                        .clickable { onThemeChange(modeKey) }
                                        .padding(vertical = 8.dp)
                                        .testTag("theme_opt_$testTagSuffix"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeTitle,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )

                        // Designer and Developer Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "طراح و مجری:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "حمید نعمت‌الهی (Hamid Nematollahi)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Feedback Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نظرات و پیشنهادات:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "اگر نظر و پیشنهادی دارید به ایمیل زیر بفرستید:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Clickable email row
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:")
                                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("hnamatollahi@gmail.com"))
                                                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for Persian PDF Translator")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "ارسال ایمیل با"))
                                            } catch (e: Exception) {
                                                // Fallback if no email client
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "hnamatollahi@gmail.com",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("feedback_email")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "ارسال",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretsPanelDialog(
    onDismiss: () -> Unit,
    viewModel: TranslationViewModel
) {
    var apiKeyInput by remember { mutableStateOf(viewModel.getCustomApiKey()) }
    val context = LocalContext.current
    val currentSaved = viewModel.getCustomApiKey()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("secrets_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Secrets Panel (تنظیم کلید خصوصی)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "با قرار دادن کلید اختصاصی Gemini API خود، می‌توانید بدون هیچ محدودیتی از تمامی قابلیت‌های ترجمهٔ متون و فایل‌های PDF استفاده کنید.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "وضعیت فعلی:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentSaved.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (currentSaved.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentSaved.isNotEmpty()) "کلید اختصاصی شما فعال است" else "استفاده از کلید اشتراکی پیش‌فرض",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentSaved.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "کلید API خود را وارد کنید (Gemini API Key):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCustomApiKey(apiKeyInput)
                            Toast.makeText(context, "کلید با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ذخیره کلید", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (currentSaved.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveCustomApiKey("")
                                apiKeyInput = ""
                                Toast.makeText(context, "کلید اختصاصی حذف شد.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("حذف کلید")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guide on how to get API key in English and Persian
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "How to get a free Gemini API Key?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Go to Google AI Studio (aistudio.google.com) and click 'Get API Key'. It is completely free!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
