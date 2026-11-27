package com.hassan.quizforge.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hassan.quizforge.model.GeneratedQuiz
import com.hassan.quizforge.ui.screens.ConfigureScreen
import com.hassan.quizforge.ui.screens.HomeScreen
import com.hassan.quizforge.ui.screens.QuizScreen
import com.hassan.quizforge.util.ExportFormatter
import com.hassan.quizforge.viewmodel.QuizViewModel
import com.hassan.quizforge.viewmodel.UiState

private const val PDF_MIME = "application/pdf"
private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

/** Which top-level screen is showing, used only to drive the AnimatedContent transition below. */
private enum class ScreenKind { HOME, CONFIGURE, QUIZ }

@Composable
fun QuizForgeApp(viewModel: QuizViewModel = viewModel()) {
    val context = LocalContext.current
    val state = viewModel.uiState
    var pendingExportText by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.processDocument(context, it) } }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingExportText
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }
        pendingExportText = null
    }

    val screenKind = when (state) {
        is UiState.Success -> ScreenKind.QUIZ
        is UiState.ReadyToConfigure -> ScreenKind.CONFIGURE
        else -> ScreenKind.HOME
    }

    // AnimatedContent only re-renders the ConfigureScreen/QuizScreen content when their data
    // actually changes, but during the exit half of a transition the outgoing screen is still
    // composed while `state` itself may have already moved on - so each screen keeps its own
    // "last known" snapshot to render from while it fades/slides out.
    var lastConfigure by remember { mutableStateOf<UiState.ReadyToConfigure?>(null) }
    var lastQuiz by remember { mutableStateOf<GeneratedQuiz?>(null) }
    if (state is UiState.ReadyToConfigure) lastConfigure = state
    if (state is UiState.Success) lastQuiz = state.quiz

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = screenKind,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                if (forward) {
                    (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
                } else {
                    (slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300)))
                }
            },
            label = "screen-transition"
        ) { kind ->
            when (kind) {
                ScreenKind.QUIZ -> lastQuiz?.let { quiz ->
                    QuizScreen(
                        quiz = quiz,
                        onBack = viewModel::reset,
                        onExport = {
                            pendingExportText = ExportFormatter.format(quiz)
                            val baseName = quiz.sourceFileName.substringBeforeLast('.').ifBlank { "document" }
                            createDocumentLauncher.launch("quiz_$baseName.txt")
                        }
                    )
                }

                ScreenKind.CONFIGURE -> lastConfigure?.let { configure ->
                    ConfigureScreen(
                        fileName = configure.fileName,
                        isPdf = configure.isPdf,
                        totalUnits = configure.totalUnits,
                        chapters = configure.chapters,
                        initialMcqCount = viewModel.mcqCount,
                        initialShortAnswerCount = viewModel.shortAnswerCount,
                        onBack = viewModel::reset,
                        onGenerate = { selection, mcq, short ->
                            viewModel.mcqCount = mcq
                            viewModel.shortAnswerCount = short
                            viewModel.generateQuiz(context, selection, mcq, short)
                        }
                    )
                }

                ScreenKind.HOME -> HomeScreen(
                    loadingMessage = (state as? UiState.Loading)?.message,
                    errorMessage = (state as? UiState.Error)?.message,
                    onPickFile = { openDocumentLauncher.launch(arrayOf(PDF_MIME, DOCX_MIME)) },
                    onDismissError = viewModel::reset
                )
            }
        }
    }
}
