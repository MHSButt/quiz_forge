package com.hassan.quizforge.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    // Bumped by "Start over" so the short-answer cards also clear what was typed into them.
    var attempt by rememberSaveable(quiz) { mutableIntStateOf(0) }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }

    val correctById = quiz.mcqs.associate { it.id to it.correctIndex }
    val mcqCorrect = mcqSelections.count { (id, option) -> correctById[id] == option }
    val hasProgress = mcqSelections.isNotEmpty() || revealedIds.isNotEmpty()

    val tabs = listOf("MCQs (${quiz.mcqs.size})", "Short answer (${quiz.shortAnswers.size})")
    val currentTotal = if (selectedTab == 0) quiz.mcqs.size else quiz.shortAnswers.size
    val currentAnswered = if (selectedTab == 0) mcqSelections.size else revealedIds.size
    val progress = if (currentTotal == 0) 0f else currentAnswered.toFloat() / currentTotal
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "quiz-progress"
    )

    fun restart() {
        mcqSelections = emptyMap()
        revealedIds = emptySet()
        attempt++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.sourceFileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasProgress) {
                        IconButton(onClick = { confirmRestart = true }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Start over")
                        }
                    }
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (selectedTab == 0) "$currentAnswered of $currentTotal answered"
                            else "$currentAnswered of $currentTotal checked",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedTab == 0 && mcqSelections.isNotEmpty()) {
                            Text(
                                "Score $mcqCorrect/${mcqSelections.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
            when (selectedTab) {
                0 -> if (quiz.mcqs.isEmpty()) {
                    EmptyState("No multiple-choice questions were generated for this selection.")
                } else {
                    LazyColumn(
                        modifier = Modifier.navigationBarsPadding(),
                        contentPadding = listPadding,
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
                        if (mcqSelections.size == quiz.mcqs.size) {
                            item(key = "result") {
                                ResultCard(
                                    correct = mcqCorrect,
                                    total = quiz.mcqs.size,
                                    onRetry = { mcqSelections = emptyMap() },
                                    onNext = if (quiz.shortAnswers.isNotEmpty()) {
                                        { selectedTab = 1 }
                                    } else null
                                )
                            }
                        }
                    }
                }

                else -> if (quiz.shortAnswers.isEmpty()) {
                    EmptyState("No short-answer questions were generated for this selection.")
                } else {
                    LazyColumn(
                        modifier = Modifier.navigationBarsPadding(),
                        contentPadding = listPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(quiz.shortAnswers, key = { _, item -> item.id }) { index, question ->
                            ShortAnswerQuestionCard(
                                question = question,
                                index = index,
                                attempt = attempt,
                                revealed = question.id in revealedIds,
                                onRevealedChange = { shown ->
                                    revealedIds = if (shown) revealedIds + question.id else revealedIds - question.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            title = { Text("Start over?") },
            text = { Text("This clears all your answers in both tabs.") },
            confirmButton = {
                Button(onClick = { restart(); confirmRestart = false }) { Text("Start over") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ResultCard(correct: Int, total: Int, onRetry: () -> Unit, onNext: (() -> Unit)?) {
    val percent = if (total == 0) 0 else correct * 100 / total
    val message = when {
        percent >= 90 -> "Excellent - you know this material well."
        percent >= 70 -> "Good job! Review the ones you missed."
        percent >= 50 -> "Not bad - another pass will help."
        else -> "Keep practising - re-read this part and try again."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$correct / $total correct",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry MCQs")
                }
                if (onNext != null) {
                    Button(onClick = onNext, shape = MaterialTheme.shapes.medium) {
                        Text("Short answers")
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
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
