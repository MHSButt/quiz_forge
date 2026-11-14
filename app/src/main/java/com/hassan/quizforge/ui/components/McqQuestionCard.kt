package com.hassan.quizforge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.McqQuestion

private val CorrectGreen = Color(0xFF1B8A4A)
private val WrongRed = Color(0xFFBA1A1A)

@Composable
fun McqQuestionCard(question: McqQuestion, index: Int) {
    var selected by rememberSaveable(question.id) { mutableStateOf(-1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${index + 1}. ${question.stem}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            question.options.forEachIndexed { optionIndex, option ->
                val isSelected = selected == optionIndex
                val isCorrectOption = optionIndex == question.correctIndex
                val answered = selected >= 0

                val textColor = when {
                    !answered -> MaterialTheme.colorScheme.onSurface
                    isCorrectOption -> CorrectGreen
                    isSelected -> WrongRed
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            enabled = !answered,
                            onClick = { selected = optionIndex }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { if (!answered) selected = optionIndex },
                        enabled = !answered,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = if (answered && !isCorrectOption) WrongRed else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(option, color = textColor)
                }
            }

            if (selected >= 0) {
                val feedback = if (selected == question.correctIndex) {
                    "Correct!"
                } else {
                    "Correct answer: ${question.correctAnswer}"
                }
                Text(
                    feedback,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected == question.correctIndex) CorrectGreen else WrongRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
