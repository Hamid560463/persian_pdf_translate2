package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val translation: String) : UiState
    data class Error(val message: String) : UiState
}

class TranslationViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val context: Context get() = getApplication()

    // Plain text translation state
    val inputText = MutableStateFlow("")
    val translatedText = MutableStateFlow("")
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    // PDF state
    val pdfUri = MutableStateFlow<Uri?>(null)
    val pdfName = MutableStateFlow("")
    val pageCount = MutableStateFlow(0)
    val currentPageIndex = MutableStateFlow(0)
    val currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    
    // Cache for PDF translations: PageIndex -> String
    val pageTranslations = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val _pdfTranslationState = MutableStateFlow<UiState>(UiState.Idle)
    val pdfTranslationState: StateFlow<UiState> = _pdfTranslationState

    // Native PDF writing / reading support
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    // TTS Support
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        // Initialize TTS
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("fa", "IR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    fun speak(text: String, onStatus: (String) -> Unit) {
        if (!isTtsReady) {
            onStatus("TTS engine not ready or Persian language packet not installed.")
            return
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TranslationSpeechId")
        onStatus("Speaking...")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    // Direct Text Translation
    fun translateText(historyViewModel: HistoryViewModel) {
        val query = inputText.value.trim()
        if (query.isEmpty()) {
            _uiState.value = UiState.Error("Please enter some text to translate.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _uiState.value = UiState.Error("Gemini API key is not configured in Secrets panel.")
                    return@launch
                }

                val systemPrompt = "You are an expert bilingual Persian (Farsi) translator. Translate the given English text into beautiful, natural, precise, and grammatically correct literary Persian. Output ONLY the Persian translated text, maintaining professional prose. Do not include any explanations, English subtitles, prefaces, or notes."
                
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = query)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (result != null) {
                    translatedText.value = result.trim()
                    _uiState.value = UiState.Success(result)
                    historyViewModel.addHistory("Text Input", query, result)
                } else {
                    _uiState.value = UiState.Error("Could not generate a translation. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    // PDF Management
    fun loadPdf(uri: Uri) {
        pdfUri.value = uri
        stopPdfRenderer()
        pageTranslations.value = emptyMap()
        currentPageIndex.value = 0

        try {
            // Copy the file contents to a safe, accessible local cache file.
            // This ensures we can open PDFs from internal storage, downloads, 
            // the device's main phone memory, external SD card, or Google Drive virtual streams flawlessly.
            val cacheFile = File(context.cacheDir, "current_reading.pdf")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            if (pfd != null) {
                fileDescriptor = pfd
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount.value = renderer.pageCount
                pdfName.value = getFileNameFromUri(uri)
                renderCurrentPage()
            } else {
                _pdfTranslationState.value = UiState.Error("Could not retrieve file details.")
            }
        } catch (e: Exception) {
            _pdfTranslationState.value = UiState.Error("Error opening PDF: ${e.message}")
        }
    }

    fun nextPage() {
        pdfRenderer?.let {
            if (currentPageIndex.value < it.pageCount - 1) {
                currentPageIndex.value++
                renderCurrentPage()
            }
        }
    }

    fun prevPage() {
        if (currentPageIndex.value > 0) {
            currentPageIndex.value--
            renderCurrentPage()
        }
    }

    private fun renderCurrentPage() {
        val index = currentPageIndex.value
        val renderer = pdfRenderer ?: return
        if (index < 0 || index >= renderer.pageCount) return

        viewModelScope.launch(Dispatchers.IO) {
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(index)
                // Render at a high resolution (e.g. 1500px wide depending on aspect ratio)
                val scale = 2.0f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                currentPageBitmap.value = bitmap
            } catch (e: Exception) {
                _pdfTranslationState.value = UiState.Error("Failed to render page: ${e.message}")
            } finally {
                page?.close()
            }
        }
    }

    // Translates the currently displayed page by sending its image to Gemini Flash!
    fun translateCurrentPdfPage(historyViewModel: HistoryViewModel) {
        val bitmap = currentPageBitmap.value
        if (bitmap == null) {
            _pdfTranslationState.value = UiState.Error("No page rendered to translate.")
            return
        }

        _pdfTranslationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _pdfTranslationState.value = UiState.Error("Gemini API key is not configured in Secrets panel.")
                    return@launch
                }

                // Convert Bitmap to Base64
                val base64Image = withContext(Dispatchers.IO) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                }

                val promptText = "Review this document page photograph. Please extract all English paragraphs, structures, or textual information visible on this page, and translate them directly into gorgeous, fluent, precise, and grammatically correct literary Persian (Farsi). Output ONLY the translated Persian script, beautifully aligned. Do not add explanations, conversational headers, metadata, or English originals."

                val generateContentRequest = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = promptText),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, generateContentRequest)
                val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (result != null) {
                    val cleanResult = result.trim()
                    val currentMap = pageTranslations.value.toMutableMap()
                    currentMap[currentPageIndex.value] = cleanResult
                    pageTranslations.value = currentMap
                    
                    _pdfTranslationState.value = UiState.Success(cleanResult)
                    
                    historyViewModel.addHistory(
                        "${pdfName.value} (Page ${currentPageIndex.value + 1})",
                        "[Visual PDF Content]",
                        cleanResult
                    )
                } else {
                    _pdfTranslationState.value = UiState.Error("Could not generate translation for this page.")
                }
            } catch (e: Exception) {
                _pdfTranslationState.value = UiState.Error(e.message ?: "An unknown network error occurred.")
            }
        }
    }

    // Local file export features
    fun exportToTxt(): Uri? {
        val translations = pageTranslations.value
        if (translations.isEmpty() && translatedText.value.isEmpty()) return null
        
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val fileName = "Translation_${System.currentTimeMillis()}.txt"
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { fos ->
                if (pdfUri.value != null) {
                    fos.write("Source PDF: ${pdfName.value}\n\n".toByteArray())
                    translations.keys.sorted().forEach { pageIdx ->
                        fos.write("--- Page ${pageIdx + 1} ---\n".toByteArray())
                        fos.write("${translations[pageIdx]}\n\n".toByteArray())
                    }
                } else {
                    fos.write("Translated text:\n\n".toByteArray())
                    fos.write(translatedText.value.toByteArray())
                }
            }
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportToPdf(): Uri? {
        val translations = pageTranslations.value
        val singleText = translatedText.value
        if (translations.isEmpty() && singleText.isEmpty()) return null

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "Translation_${System.currentTimeMillis()}.pdf"
            val file = File(exportDir, fileName)

            val pdfDocument = PdfDocument()
            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }

            val textToDraw = if (pdfUri.value != null) {
                buildString {
                    translations.keys.sorted().forEach { pageIdx ->
                        append("--- Page ${pageIdx + 1} ---\n\n")
                        append(translations[pageIdx])
                        append("\n\n")
                    }
                }
            } else {
                singleText
            }

            // Split into pages / layout
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val margin = 40f
            val layoutWidth = pageWidth - (margin * 2).toInt()

            val lines = textToDraw.split("\n")
            var currentY = margin
            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            for (line in lines) {
                if (line.trim().isEmpty()) {
                    currentY += 15f
                    continue
                }

                // Build StaticLayout for RTL text wrapping
                val builder = StaticLayout.Builder.obtain(line, 0, line.length, textPaint, layoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_OPPOSITE) // Align right for Persian
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)

                val staticLayout = builder.build()

                // Check if layout fits on current page
                if (currentY + staticLayout.height > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin
                }

                canvas.save()
                canvas.translate(margin, currentY)
                staticLayout.draw(canvas)
                canvas.restore()

                currentY += staticLayout.height + 10f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "document.pdf"
        }
        return name
    }

    private fun stopPdfRenderer() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (e: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPdfRenderer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
