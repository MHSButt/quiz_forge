package com.hassan.quizforge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.McqQuestion
import com.hassan.quizforge.ui.theme.CorrectGreen
import com.hassan.quizforge.ui.theme.CorrectGreenContainer
import com.hassan.quizforge.ui.theme.CorrectGreenContainerDark
import com.hassan.quizforge.ui.theme.CorrectGreenDark
import com.hassan.quizforge.ui.theme.OnCorrectGreenDark

private val OptionLetters = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun McqQuestionCard(
    question: McqQuestion,
    index: Int,
    // The chosen option index (-1 = not answered yet). Owned by QuizScreen rather than this card,
    // so the answer survives switching tabs/rotation and always agrees with the progress bar.
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val answered = selected >= 0
    val isDark = isSystemInDarkTheme()
    val correctColor = if (isDark) CorrectGreenDark else CorrectGreen
    val correctContainer = if (isDark) CorrectGreenContainerDark else CorrectGreenContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    question.stem,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            question.options.forEachIndexed { optionIndex, option ->
                val isSelected = selected == optionIndex
                val isCorrectOption = optionIndex == question.correctIndex
                val showAsCorrect = answered && isCorrectOption
                val showAsWrong = answered && isSelected && !isCorrectOption

                val containerColor by animateColorAsState(
                    targetValue = when {
                        showAsCorrect -> correctContainer
                        showAsWrong -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(250),
                    label = "option-bg"
                )
                val badgeColor = when {
                    showAsCorrect -> correctColor
                    showAsWrong -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                val textColor = when {
                    showAsCorrect -> MaterialTheme.colorScheme.onSurface
                    showAsWrong -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(containerColor)
                        .selectable(
                            selected = isSelected,
                            enabled = !answered,
                            onClick = {
                                if (!answered) onSelect(optionIndex)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (showAsCorrect || showAsWrong) badgeColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            showAsCorrect -> Icon(
                                Icons.Filled.Check,
                                contentDescription = "Correct answer",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            showAsWrong -> Icon(
                                Icons.Filled.Close,
                                contentDescription = "Your answer (incorrect)",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Text(
                                OptionLetters.getOrElse(optionIndex) { "?" },
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        option,
                        color = textColor,
                        fontWeight = if (showAsCorrect) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (answered) {
                val correct = selected == question.correctIndex
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (correct) correctContainer else MaterialTheme.colorScheme.errorContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (correct) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (correct) correctColor else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (correct) "Correct!" else "Correct answer: ${question.correctAnswer}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (correct) {
                            if (isDark) OnCorrectGreenDark else MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}
