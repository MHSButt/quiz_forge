package com.hassan.quizforge.nlp

import kotlin.math.sqrt

data class RankedSentence(val index: Int, val sentence: String, val score: Double)

/**
 * Scores every sentence in the document by how "quiz-worthy" it looks, then returns them sorted
 * best-first. A sentence scores well if it is a reasonable length, declarative (not already a
 * question), and contains words that are either frequent across the document or look like
 * proper nouns / numbers (both of which tend to make good quiz answers).
 */
object CandidateSentenceSelector {

    private val NUMBER = Regex("\\b\\d[\\d,.]*%?\\b")
    private val CAPITALIZED_WORD = Regex("\\b[A-Z][a-z]+\\b")

    fun rank(sentences: List<String>, freq: Map<String, Int>): List<RankedSentence> {
        val ranked = sentences.mapIndexedNotNull { index, sentence ->
            val trimmed = sentence.trim()
            val wordCount = trimmed.split(Regex("\\s+")).size
            if (wordCount < 6 || wordCount > 40) return@mapIndexedNotNull null
            if (trimmed.endsWith("?")) return@mapIndexedNotNull null
            if (!trimmed.first().isLetter() && !trimmed.first().isDigit()) return@mapIndexedNotNull null

            val words = WordFrequencyAnalyzer.tokenize(trimmed)
            val freqScore = words.sumOf { (freq[it.lowercase()] ?: 0).toDouble() }
            val normalizedFreqScore = freqScore / sqrt(wordCount.toDouble())

            val properNounBonus = CAPITALIZED_WORD.findAll(trimmed).count().coerceAtMost(5) * 1.5
            val numberBonus = if (NUMBER.containsMatchIn(trimmed)) 2.0 else 0.0

            val score = normalizedFreqScore + properNounBonus + numberBonus
            RankedSentence(index, trimmed, score)
        }
        return ranked.sortedByDescending { it.score }
    }
}
