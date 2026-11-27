package com.hassan.quizforge.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.DocumentChapter
import com.hassan.quizforge.viewmodel.MAX_QUESTIONS_PER_TYPE
import com.hassan.quizforge.viewmodel.SelectionMode

private enum class ModeChoice { WHOLE, CHAPTERS, RANGE }

private val IntSetSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScreen(
    fileName: String,
    isPdf: Boolean,
    totalUnits: Int,
    chapters: List<DocumentChapter>,
    initialMcqCount: Int,
    initialShortAnswerCount: Int,
    onBack: () -> Unit,
    onGenerate: (SelectionMode, mcqCount: Int, shortAnswerCount: Int) -> Unit
) {
    val unitWord = if (isPdf) "page" else "section"
    var mode by rememberSaveable {
        mutableStateOf(if (chapters.isNotEmpty()) ModeChoice.CHAPTERS else ModeChoice.WHOLE)
    }
    var selectedChapters by rememberSaveable(stateSaver = IntSetSaver) { mutableStateOf(chapters.indices.toSet()) }
    var fromPageText by rememberSaveable { mutableStateOf("1") }
    var toPageText by rememberSaveable { mutableStateOf(totalUnits.toString()) }
    var mcqText by rememberSaveable { mutableStateOf(initialMcqCount.toString()) }
    var shortText by rememberSaveable { mutableStateOf(initialShortAnswerCount.toString()) }

    val mcqValue = mcqText.toIntOrNull() ?: 0
    val shortValue = shortText.toIntOrNull() ?: 0
    val canGenerate = (mcqValue > 0 || shortValue > 0) &&
        (mode != ModeChoice.CHAPTERS || selectedChapters.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up your quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(fileName, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$totalUnits ${unitWord}${if (totalUnits == 1) "" else "s"}" +
                        if (chapters.isNotEmpty()) " · ${chapters.size} section(s) detected" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "What to include",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                ModeSegment(
                    label = "Whole document",
                    selected = mode == ModeChoice.WHOLE,
                    onClick = { mode = ModeChoice.WHOLE }
                )

                if (chapters.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ModeSegment(
                        label = "Select chapters / lectures / topics",
                        selected = mode == ModeChoice.CHAPTERS,
                        onClick = { mode = ModeChoice.CHAPTERS }
                    )
                } else {
                    Text(
                        "No chapters, lectures or topics were automatically detected in this document" +
                            if (isPdf) " - use a custom page range below instead." else ".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isPdf) {
                    Spacer(Modifier.height(8.dp))
                    ModeSegment(
                        label = "Custom page range",
                        selected = mode == ModeChoice.RANGE,
                        onClick = { mode = ModeChoice.RANGE }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "configure-mode"
            ) { currentMode ->
                when (currentMode) {
                    ModeChoice.CHAPTERS -> Column {
                        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TextButton(onClick = { selectedChapters = chapters.indices.toSet() }) {
                                Text("Select all")
                            }
                            TextButton(onClick = { selectedChapters = emptySet() }) {
                                Text("Select none")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .height(280.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            items(chapters.size) { index ->
                                val chapter = chapters[index]
                                val checked = index in selectedChapters
                                val toggle = {
                                    selectedChapters =
                                        if (checked) selectedChapters - index else selectedChapters + index
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .selectable(selected = checked, onClick = toggle),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (checked) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { toggle() },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Column {
                                            Text(
                                                chapter.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (checked) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (isPdf) {
                                                val range = if (chapter.startIndex == chapter.endIndex) {
                                                    "Page ${chapter.startIndex + 1}"
                                                } else {
                                                    "Pages ${chapter.startIndex + 1}-${chapter.endIndex + 1}"
                                                }
                                                Text(
                                                    range,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ModeChoice.RANGE -> Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = fromPageText,
                                onValueChange = { fromPageText = it.filter(Char::isDigit) },
                                label = { Text("From page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = toPageText,
                                onValueChange = { toPageText = it.filter(Char::isDigit) },
                                label = { Text("To page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This document has $totalUnits pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    ModeChoice.WHOLE -> Spacer(Modifier.height(8.dp))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How many questions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        QuestionCountStepper(
                            label = "Multiple-choice",
                            valueText = mcqText,
                            onValueChange = { mcqText = it },
                            modifier = Modifier.weight(1f)
                        )
                        QuestionCountStepper(
                            label = "Short-answer",
                            valueText = shortText,
                            onValueChange = { shortText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Up to $MAX_QUESTIONS_PER_TYPE of each. A short or narrow selection may " +
                            "yield fewer than you ask for - QuizForge won't repeat itself just to " +
                            "hit a number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = {
                    val selection = when (mode) {
                        ModeChoice.WHOLE -> SelectionMode.WholeDocument
                        ModeChoice.CHAPTERS -> SelectionMode.Chapters(selectedChapters)
                        ModeChoice.RANGE -> SelectionMode.CustomPageRange(
                            fromPage = fromPageText.toIntOrNull() ?: 1,
                            toPage = toPageText.toIntOrNull() ?: totalUnits
                        )
                    }
                    onGenerate(
                        selection,
                        mcqValue.coerceIn(0, MAX_QUESTIONS_PER_TYPE),
                        shortValue.coerceIn(0, MAX_QUESTIONS_PER_TYPE)
                    )
                },
                enabled = canGenerate,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp)
            ) {
                Text("Generate quiz", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ModeSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/** A number field flanked by -/+ buttons, clamped to [0, MAX_QUESTIONS_PER_TYPE]. */
@Composable
private fun QuestionCountStepper(
    label: String,
    valueText: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val value = valueText.toIntOrNull() ?: 0

    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { onValueChange((value - 1).coerceIn(0, MAX_QUESTIONS_PER_TYPE).toString()) },
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = { onValueChange(it.filter(Char::isDigit).take(3)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
            )

            FilledIconButton(
                onClick = { onValueChange((value + 1).coerceIn(0, MAX_QUESTIONS_PER_TYPE).toString()) },
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}
