package com.hassan.quizforge.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassan.quizforge.extraction.ChapterDetector
import com.hassan.quizforge.extraction.DocxParagraph
import com.hassan.quizforge.extraction.DocxTextExtractor
import com.hassan.quizforge.extraction.EmptyDocumentException
import com.hassan.quizforge.extraction.FileType
import com.hassan.quizforge.extraction.FileTypeDetector
import com.hassan.quizforge.extraction.QuizForgeException
import com.hassan.quizforge.extraction.OcrTextExtractor
import com.hassan.quizforge.extraction.PdfContent
import com.hassan.quizforge.extraction.PdfTextExtractor
import com.hassan.quizforge.extraction.ScanDetector
import com.hassan.quizforge.extraction.UnsupportedFileTypeException
import com.hassan.quizforge.model.DocumentChapter
import com.hassan.quizforge.model.GeneratedQuiz
import com.hassan.quizforge.nlp.QuestionGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Upper bound on how many MCQs or short-answer questions can be requested at once. */
const val MAX_QUESTIONS_PER_TYPE = 300

/** Which part of the document to generate the quiz from. */
sealed interface SelectionMode {
    data object WholeDocument : SelectionMode
    /** [selectedIndices] are indices into the detected chapters list, not page numbers. */
    data class Chapters(val selectedIndices: Set<Int>) : SelectionMode
    /** 1-based, inclusive page numbers. PDF only - .docx has no fixed page concept. */
    data class CustomPageRange(val fromPage: Int, val toPage: Int) : SelectionMode
}

sealed interface UiState {
    data object Idle : UiState
    /**
     * [canSkip]: the current step is optional and the UI may offer a "Skip" button.
     * [progress]: 0..1 when the step's length is known (OCR), null for an indeterminate spinner.
     */
    data class Loading(
        val message: String = "Reading document…",
        val canSkip: Boolean = false,
        val progress: Float? = null
    ) : UiState
    data class ReadyToConfigure(
        val fileName: String,
        val isPdf: Boolean,
        val totalUnits: Int,
        val chapters: List<DocumentChapter>
    ) : UiState
    data class Success(val quiz: GeneratedQuiz) : UiState
    data class Error(val message: String) : UiState
}

class QuizViewModel : ViewModel() {

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    // Seed values the Configure screen's question-count fields start from.
    var mcqCount by mutableStateOf(8)
    var shortAnswerCount by mutableStateOf(6)

    // Phase-1 results. Held as plain (non-Compose-state) fields, not inside UiState, since a
    // long document's full page/paragraph text can be sizeable and the UI never needs to
    // observe it directly - only the Configure screen's summary (in ReadyToConfigure) does.
    private var pendingUri: Uri? = null
    private var pendingFileName: String = ""
    private var pendingPdfPages: List<String>? = null
    private var pendingDocxParagraphs: List<DocxParagraph>? = null

    /** Set from the UI thread when the user skips the scanned-page chapter search. */
    @Volatile
    private var skipChapterScanRequested = false

    /** Stops the optional OCR chapter search; whatever was found so far is still used. */
    fun skipChapterScan() {
        skipChapterScanRequested = true
    }

    /**
     * Phase 1: read the document and work out its structure - pages (PDF) or paragraphs
     * (.docx), plus any auto-detected chapters. Deliberately does NOT run OCR here even if the
     * PDF turns out to be scanned - that's postponed until [generateQuiz], and even then only
     * for the pages actually selected, so picking one chapter out of a 300-page scanned book
     * doesn't OCR the other 299 pages for nothing.
     */
    fun processDocument(context: Context, uri: Uri) {
        uiState = UiState.Loading("Reading document…")
        viewModelScope.launch {
            val appContext = context.applicationContext

            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val displayName = FileTypeDetector.queryDisplayName(appContext, uri)
                    val type = FileTypeDetector.detect(appContext, uri, displayName)

                    when (type) {
                        FileType.PDF -> {
                            val content = PdfTextExtractor.extract(appContext, uri)
                            val pages = content.pages
                            val headerText = ocrScannedPageHeaders(appContext, uri, content)
                            val chapters = ChapterDetector.detectPdfChapters(content, headerText)

                            pendingUri = uri
                            pendingFileName = displayName
                            pendingPdfPages = pages
                            pendingDocxParagraphs = null

                            UiState.ReadyToConfigure(
                                fileName = displayName,
                                isPdf = true,
                                totalUnits = pages.size,
                                chapters = chapters
                            )
                        }

                        FileType.DOCX -> {
                            val paragraphs = DocxTextExtractor.extractParagraphs(appContext, uri)
                            val chapters = ChapterDetector.detectDocxChapters(paragraphs)

                            pendingUri = uri
                            pendingFileName = displayName
                            pendingDocxParagraphs = paragraphs
                            pendingPdfPages = null

                            UiState.ReadyToConfigure(
                                fileName = displayName,
                                isPdf = false,
                                totalUnits = paragraphs.size,
                                chapters = chapters
                            )
                        }

                        FileType.UNSUPPORTED ->
                            throw UnsupportedFileTypeException(FileTypeDetector.extensionOf(displayName))
                    }
                }
            }

            uiState = result.fold(
                onSuccess = { it },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Something went wrong while reading the document.")
                }
            )
        }
    }

    /** Phase 2: build the quiz from whichever part of the document the user selected. */
    fun generateQuiz(context: Context, selection: SelectionMode, requestedMcq: Int, requestedShort: Int) {
        val appContext = context.applicationContext
        val fileName = pendingFileName
        val chapters = (uiState as? UiState.ReadyToConfigure)?.chapters.orEmpty()
        val mcq = requestedMcq.coerceIn(0, MAX_QUESTIONS_PER_TYPE)
        val short = requestedShort.coerceIn(0, MAX_QUESTIONS_PER_TYPE)

        uiState = UiState.Loading("Generating questions…")
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    var usedOcr = false
                    val rawText = when {
                        pendingPdfPages != null ->
                            buildPdfSelectionText(appContext, selection, chapters) { usedOcr = true }
                        pendingDocxParagraphs != null ->
                            buildDocxSelectionText(selection, chapters)
                        else -> throw IllegalStateException("No document is loaded")
                    }

                    if (rawText.isBlank()) throw EmptyDocumentException(ocrAttempted = usedOcr)

                    val quiz = QuestionGenerator().generate(rawText, mcq, short)
                    quiz.copy(sourceFileName = fileName)
                }
            }

            uiState = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Something went wrong while generating the quiz.")
                }
            )
        }
    }

    /**
     * Scanned pages have no text layer, so a "Chapter 3" heading on them is invisible to the
     * text-based detector. When the PDF has no bookmarks to rely on, OCR just the top of each
     * scanned page (fast, low resolution) so their headings can be found too. The user can skip
     * this on a long book and fall back to a page range.
     */
    private fun ocrScannedPageHeaders(context: Context, uri: Uri, content: PdfContent): Map<Int, String> {
        if (ChapterDetector.hasUsableOutline(content)) return emptyMap()
        val scanned = content.pages.indices.filter { ScanDetector.pageLooksScanned(content.pages[it]) }
        if (scanned.isEmpty()) return emptyMap()

        skipChapterScanRequested = false
        uiState = UiState.Loading("Looking for chapters in scanned pages…", canSkip = true)
        return try {
            OcrTextExtractor.extractPageHeaders(
                context, uri, scanned,
                onProgress = { done, total ->
                    uiState = UiState.Loading(
                        "Looking for chapters in scanned pages - page $done of $total…",
                        canSkip = true,
                        progress = done.toFloat() / total
                    )
                },
                shouldStop = { skipChapterScanRequested }
            )
        } catch (e: QuizForgeException) {
            // Chapter detection is a convenience - don't fail the import over it. Full OCR
            // at generation time will report the problem properly if it's still there.
            emptyMap()
        } finally {
            uiState = UiState.Loading()
        }
    }

    private fun buildPdfSelectionText(
        context: Context,
        selection: SelectionMode,
        chapters: List<DocumentChapter>,
        onOcrUsed: () -> Unit
    ): String {
        val pages = pendingPdfPages ?: return ""
        val uri = pendingUri ?: return pages.joinToString("\n\n")

        val selectedIndices = resolveIndices(selection, chapters, pages.size)
        if (selectedIndices.isEmpty()) return ""

        val scannedAmongSelected = selectedIndices.filter { ScanDetector.pageLooksScanned(pages[it]) }
        if (scannedAmongSelected.isEmpty()) {
            return selectedIndices.joinToString("\n\n") { pages[it] }
        }

        onOcrUsed()
        uiState = UiState.Loading(
            if (scannedAmongSelected.size == selectedIndices.size) {
                "This looks like a scanned PDF - running on-device text recognition…"
            } else {
                "Running on-device text recognition on ${scannedAmongSelected.size} " +
                    "page(s) that don't have embedded text…"
            }
        )

        val ocrResults = OcrTextExtractor.extractPages(context, uri, scannedAmongSelected) { done, total ->
            uiState = UiState.Loading(
                "Running on-device text recognition - page $done of $total…",
                progress = done.toFloat() / total
            )
        }

        return selectedIndices.joinToString("\n\n") { i -> ocrResults[i] ?: pages[i] }
    }

    private fun buildDocxSelectionText(selection: SelectionMode, chapters: List<DocumentChapter>): String {
        val paragraphs = pendingDocxParagraphs ?: return ""
        val selectedIndices = resolveIndices(selection, chapters, paragraphs.size)
        return selectedIndices.joinToString("\n") { paragraphs[it].text }
    }

    /** Turns a [SelectionMode] into a sorted, de-duplicated list of 0-based unit indices. */
    private fun resolveIndices(
        selection: SelectionMode,
        chapters: List<DocumentChapter>,
        totalUnits: Int
    ): List<Int> {
        if (totalUnits <= 0) return emptyList()
        val validRange = 0 until totalUnits

        return when (selection) {
            is SelectionMode.WholeDocument -> validRange.toList()

            is SelectionMode.Chapters -> {
                selection.selectedIndices
                    .mapNotNull { chapters.getOrNull(it) }
                    .flatMap { it.startIndex..it.endIndex }
                    .filter { it in validRange }
                    .distinct()
                    .sorted()
            }

            is SelectionMode.CustomPageRange -> {
                val from = (selection.fromPage - 1).coerceIn(0, totalUnits - 1)
                val to = (selection.toPage - 1).coerceIn(from, totalUnits - 1)
                (from..to).toList()
            }
        }
    }

    fun reset() {
        uiState = UiState.Idle
        pendingUri = null
        pendingFileName = ""
        pendingPdfPages = null
        pendingDocxParagraphs = null
    }
}
