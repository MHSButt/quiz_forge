package com.hassan.quizforge.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.GeneratedQuiz
import com.hassan.quizforge.ui.components.McqQuestionCard
import com.hassan.quizforge.ui.components.ShortAnswerQuestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(quiz: GeneratedQuiz, onBack: () -> Unit, onExport: () -> Unit) {
    // Progress is derived from sets of question ids rather than incremented counters, so it can't
    // drift from what the cards show: answering, switching tabs, coming back, or rotating the
    // phone never double-counts. All of it is rememberSaveable so it survives rotation too.
    var selectedTab by rememberSaveable(quiz) { mutableIntStateOf(0) }
    // MCQ id -> chosen option index. Held here (not inside each card) so a card scrolled away or
    // on the other tab keeps its answer when it comes back.
    var mcqSelections by rememberSaveable(quiz) { mutableStateOf(mapOf<Int, Int>()) }
    var revealedIds by rememberSaveable(quiz) { mutableStateOf(setOf<Int>()) }

    val tabs = listOf("MCQs (${quiz.mcqs.size})", "Short answer (${quiz.shortAnswers.size})")
    val currentTotal = if (selectedTab == 0) quiz.mcqs.size else quiz.shortAnswers.size
    val currentAnswered = if (selectedTab == 0) mcqSelections.size else revealedIds.size
    val progress = if (currentTotal == 0) 0f else currentAnswered.toFloat() / currentTotal
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "quiz-progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.sourceFileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Filled.Download, contentDescription = "Export as text file")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (currentTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "  $currentAnswered/$currentTotal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> if (quiz.mcqs.isEmpty()) {
                    EmptyState("No multiple-choice questions were generated for this document.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(quiz.mcqs, key = { _, item -> item.id }) { index, question ->
                            McqQuestionCard(
                                question = question,
                                index = index,
                                selected = mcqSelections[question.id] ?: -1,
                                onSelect = { option ->
                                    if (question.id !in mcqSelections) {
                                        mcqSelections = mcqSelections + (question.id to option)
                                    }
                                }
                            )
                        }
                    }
                }

                else -> if (quiz.shortAnswers.isEmpty()) {
                    EmptyState("No short-answer questions were generated for this document.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(quiz.shortAnswers, key = { _, item -> item.id }) { index, question ->
                            ShortAnswerQuestionCard(
                                question = question,
                                index = index,
                                onRevealed = { revealedIds = revealedIds + question.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
