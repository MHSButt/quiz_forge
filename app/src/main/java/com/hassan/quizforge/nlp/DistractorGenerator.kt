package com.hassan.quizforge.nlp

import kotlin.random.Random

/** Picks up to 3 wrong-answer options for an MCQ, preferring the same "shape" as the answer. */
object DistractorGenerator {

    fun generate(
        answer: AnswerPick,
        pools: VocabPools,
        alreadyUsed: Set<String>,
        random: Random = Random.Default
    ): List<String> {
        val exclude = alreadyUsed + answer.phrase.lowercase()

        val samePool = when (answer.type) {
            AnswerType.PROPER_NOUN -> pools.properNounPhrases
            AnswerType.NUMBER -> pools.numbers
            AnswerType.COMMON_WORD -> pools.commonWords
        }.filter { it.lowercase() !in exclude }

        val picked = LinkedHashSet<String>()
        picked.addAll(samePool.shuffled(random).take(3))

        if (picked.size < 3) {
            // Not enough of the same "shape" - fall back to pooling everything else together.
            val fallbackPool = (pools.properNounPhrases + pools.numbers + pools.commonWords)
                .filter { it.lowercase() !in exclude && it.lowercase() !in picked.map(String::lowercase) }
                .distinctBy { it.lowercase() }
            picked.addAll(fallbackPool.shuffled(random).take(3 - picked.size))
        }

        if (picked.size < 3 && answer.type == AnswerType.NUMBER) {
            // Last resort for numeric answers: synthesize plausible-looking wrong numbers.
            val numericValue = answer.phrase.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            if (numericValue != null) {
                val variants = listOf(numericValue * 2, numericValue / 2, numericValue + 10)
                    .map { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
                    .filter { it.lowercase() !in exclude }
                picked.addAll(variants.take(3 - picked.size))
            }
        }

        return picked.take(3).toList()
    }
}
