package com.hassan.quizforge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hassan.quizforge.model.ShortAnswerQuestion

@Composable
fun ShortAnswerQuestionCard(question: ShortAnswerQuestion, index: Int) {
    var myAnswer by rememberSaveable(question.id) { mutableStateOf("") }
    var revealed by rememberSaveable(question.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${index + 1}. ${question.question}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = myAnswer,
                onValueChange = { myAnswer = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your answer") },
                singleLine = false
            )

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = { revealed = !revealed }) {
                Text(if (revealed) "Hide answer" else "Show answer")
            }

            if (revealed) {
                Text(
                    question.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
