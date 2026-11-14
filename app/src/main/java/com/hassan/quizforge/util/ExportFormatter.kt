package com.hassan.quizforge.util

import com.hassan.quizforge.model.GeneratedQuiz
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats a [GeneratedQuiz] as a plain-text study sheet, for the "export" action. */
object ExportFormatter {

    private val LETTERS = listOf("A", "B", "C", "D")

    fun format(quiz: GeneratedQuiz): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.appendLine("QuizForge - generated from: ${quiz.sourceFileName}")
        sb.appendLine("Generated on: $dateStr")
        sb.appendLine()

        if (quiz.mcqs.isNotEmpty()) {
            sb.appendLine("===== Multiple Choice Questions =====")
            quiz.mcqs.forEachIndexed { i, q ->
                sb.appendLine("${i + 1}. ${q.stem}")
                q.options.forEachIndexed { j, option ->
                    sb.appendLine("   ${LETTERS.getOrElse(j) { (j + 1).toString() }}. $option")
                }
                sb.appendLine()
            }

            sb.appendLine("----- Answer key (Multiple Choice) -----")
            quiz.mcqs.forEachIndexed { i, q ->
                sb.appendLine("${i + 1}. ${LETTERS.getOrElse(q.correctIndex) { "?" }} - ${q.correctAnswer}")
            }
            sb.appendLine()
        }

        if (quiz.shortAnswers.isNotEmpty()) {
            sb.appendLine("===== Short Answer Questions =====")
            quiz.shortAnswers.forEachIndexed { i, q ->
                sb.appendLine("${i + 1}. ${q.question}")
            }
            sb.appendLine()

            sb.appendLine("----- Answer key (Short Answer) -----")
            quiz.shortAnswers.forEachIndexed { i, q ->
                sb.appendLine("${i + 1}. ${q.answer}")
            }
        }

        return sb.toString()
    }
}
