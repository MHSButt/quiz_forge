package com.hassan.quizforge.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.DocumentChapter
import com.hassan.quizforge.viewmodel.MAX_QUESTIONS_PER_TYPE
import com.hassan.quizforge.viewmodel.SelectionMode

private enum class ModeChoice { WHOLE, CHAPTERS, RANGE }

private val IntSetSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() }
)

/** Show a search box above the chapter list once it's long enough to need one. */
private const val SEARCH_THRESHOLD = 8

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
    val unitWord = if (isPdf) "page" else "paragraph"
    var mode by rememberSaveable {
        mutableStateOf(if (chapters.isNotEmpty()) ModeChoice.CHAPTERS else ModeChoice.WHOLE)
    }
    var selectedChapters by rememberSaveable(stateSaver = IntSetSaver) { mutableStateOf(chapters.indices.toSet()) }
    var query by rememberSaveable { mutableStateOf("") }
    var fromPageText by rememberSaveable { mutableStateOf("1") }
    var toPageText by rememberSaveable { mutableStateOf(totalUnits.toString()) }
    var mcqText by rememberSaveable { mutableStateOf(initialMcqCount.toString()) }
    var shortText by rememberSaveable { mutableStateOf(initialShortAnswerCount.toString()) }

    val mcqValue = (mcqText.toIntOrNull() ?: 0).coerceIn(0, MAX_QUESTIONS_PER_TYPE)
    val shortValue = (shortText.toIntOrNull() ?: 0).coerceIn(0, MAX_QUESTIONS_PER_TYPE)
    val totalQuestions = mcqValue + shortValue

    val fromPage = fromPageText.toIntOrNull()
    val toPage = toPageText.toIntOrNull()
    val rangeError: String? = when {
        mode != ModeChoice.RANGE -> null
        fromPage == null || toPage == null -> "Enter both page numbers."
        fromPage < 1 || toPage > totalUnits -> "Pages must be between 1 and $totalUnits."
        fromPage > toPage -> "\"From\" can't be after \"To\"."
        else -> null
    }

    val canGenerate = totalQuestions > 0 &&
        (mode != ModeChoice.CHAPTERS || selectedChapters.isNotEmpty()) &&
        rangeError == null

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
        },
        // Pinned to the bottom (above the navigation bar and keyboard) so it can never be
        // pushed off-screen by a long chapter list on a small phone.
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Button(
                    onClick = {
                        val selection = when (mode) {
                            ModeChoice.WHOLE -> SelectionMode.WholeDocument
                            ModeChoice.CHAPTERS -> SelectionMode.Chapters(selectedChapters)
                            ModeChoice.RANGE -> SelectionMode.CustomPageRange(
                                fromPage = fromPage ?: 1,
                                toPage = toPage ?: totalUnits
                            )
                        }
                        onGenerate(selection, mcqValue, shortValue)
                    },
                    enabled = canGenerate,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            totalQuestions == 0 -> "Choose how many questions"
                            mode == ModeChoice.CHAPTERS && selectedChapters.isEmpty() -> "Select at least one section"
                            rangeError != null -> "Fix the page range"
                            else -> "Generate $totalQuestions question${if (totalQuestions == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocumentHeader(
                fileName = fileName,
                isPdf = isPdf,
                summary = buildString {
                    append("$totalUnits $unitWord${if (totalUnits == 1) "" else "s"}")
                    if (chapters.isNotEmpty()) {
                        append(" · ${chapters.size} section${if (chapters.size == 1) "" else "s"} found")
                    }
                }
            )

            SectionLabel("What to include")

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chapters.isNotEmpty()) {
                    ModeOption(
                        title = "Chapters / lectures / topics",
                        subtitle = "${chapters.size} found - choose the ones you want",
                        selected = mode == ModeChoice.CHAPTERS,
                        onClick = { mode = ModeChoice.CHAPTERS }
                    )
                }
                ModeOption(
                    title = "Whole document",
                    subtitle = "Use all $totalUnits $unitWord${if (totalUnits == 1) "" else "s"}",
                    selected = mode == ModeChoice.WHOLE,
                    onClick = { mode = ModeChoice.WHOLE }
                )
                if (isPdf) {
                    ModeOption(
                        title = "Page range",
                        subtitle = "Pick a from-to range of pages",
                        selected = mode == ModeChoice.RANGE,
                        onClick = { mode = ModeChoice.RANGE }
                    )
                }
                if (chapters.isEmpty()) {
                    InfoNote(
                        "No chapters, lectures or topics were detected in this document" +
                            if (isPdf) " - use a page range to focus on part of it." else "."
                    )
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "configure-mode"
            ) { currentMode ->
                when (currentMode) {
                    ModeChoice.CHAPTERS -> ChapterPicker(
                        chapters = chapters,
                        isPdf = isPdf,
                        selected = selectedChapters,
                        onSelectedChange = { selectedChapters = it },
                        query = query,
                        onQueryChange = { query = it }
                    )

                    ModeChoice.RANGE -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = fromPageText,
                                onValueChange = { fromPageText = it.filter(Char::isDigit).take(5) },
                                label = { Text("From page") },
                                singleLine = true,
                                isError = rangeError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = toPageText,
                                onValueChange = { toPageText = it.filter(Char::isDigit).take(5) },
                                label = { Text("To page") },
                                singleLine = true,
                                isError = rangeError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            rangeError ?: "This document has $totalUnits pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (rangeError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    ModeChoice.WHOLE -> Spacer(Modifier.height(0.dp))
                }
            }

            SectionLabel("How many questions")

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
        }
    }
}

@Composable
private fun DocumentHeader(fileName: String, isPdf: Boolean, summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon: ImageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Description
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
    )
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
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
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onClick = null: the whole card is the click target, the radio is just the indicator.
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChapterPicker(
    chapters: List<DocumentChapter>,
    isPdf: Boolean,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit
) {
    val visible = if (query.isBlank()) {
        chapters.indices.toList()
    } else {
        chapters.indices.filter { chapters[it].title.contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (chapters.size > SEARCH_THRESHOLD) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search, e.g. \"Lecture 5\" or a topic name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${selected.size} of ${chapters.size} selected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            // With a search active these act on the matching rows only.
            TextButton(onClick = { onSelectedChange(selected + visible) }) {
                Text(if (query.isBlank()) "Select all" else "Select matches")
            }
            TextButton(onClick = { onSelectedChange(selected - visible.toSet()) }) {
                Text(if (query.isBlank()) "Clear" else "Clear matches")
            }
        }

        if (visible.isEmpty()) {
            Text(
                "Nothing matches \"$query\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visible, key = { it }) { index ->
                    val chapter = chapters[index]
                    val checked = index in selected
                    val toggle = { onSelectedChange(if (checked) selected - index else selected + index) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    chapter.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (checked) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (isPdf) {
                                    Text(
                                        if (chapter.startIndex == chapter.endIndex) {
                                            "Page ${chapter.startIndex + 1}"
                                        } else {
                                            "Pages ${chapter.startIndex + 1}-${chapter.endIndex + 1}"
                                        },
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
                singleLine = true,
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
