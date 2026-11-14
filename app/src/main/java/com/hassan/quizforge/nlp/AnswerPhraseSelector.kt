package com.hassan.quizforge.nlp

enum class AnswerType { PROPER_NOUN, NUMBER, COMMON_WORD }

data class AnswerPick(val phrase: String, val type: AnswerType)

/**
 * Picks the single best "blank-able" word or phrase out of one sentence: prefer a proper-noun
 * phrase, then a number, then the rarest-but-present common word. Returns null if nothing in
 * the sentence looks worth blanking out.
 */
object AnswerPhraseSelector {

    private val PROPER_NOUN_RUN =
        Regex("\\b[A-Z][a-zA-Z.'-]*(?:\\s+[A-Z][a-zA-Z.'-]*)*\\b")
    private val NUMBER = Regex("\\b\\d[\\d,.]*%?\\b")

    fun select(sentence: String, freq: Map<String, Int>, alreadyUsed: Set<String>): AnswerPick? {
        // 1) Longest proper-noun run that isn't just the first word of the sentence and isn't
        //    already used as an answer elsewhere.
        val properNounCandidates = PROPER_NOUN_RUN.findAll(sentence)
            .map { it.value.trim() to it.range.first }
            .filter { (phrase, start) ->
                val wordCount = phrase.split(Regex("\\s+")).size
                val isSentenceStart = start == 0
                phrase.length >= 3 &&
                    !(isSentenceStart && wordCount == 1) &&
                    phrase.lowercase() !in alreadyUsed
            }
            .map { it.first }
            .toList()

        if (properNounCandidates.isNotEmpty()) {
            val best = properNounCandidates.maxByOrNull { it.length }!!
            return AnswerPick(best, AnswerType.PROPER_NOUN)
        }

        // 2) A number / percentage / year not already used.
        val numberCandidates = NUMBER.findAll(sentence)
            .map { it.value }
            .filter { it.lowercase() !in alreadyUsed }
            .toList()
        if (numberCandidates.isNotEmpty()) {
            return AnswerPick(numberCandidates.first(), AnswerType.NUMBER)
        }

        // 3) The rarest-but-present non-stopword word, length >= 4, not the first word.
        val words = sentence.trim().split(Regex("\\s+"))
        val commonCandidates = words.drop(1)
            .map { it.trim(',', '.', ';', ':', '"', '\'', ')', '(') }
            .filter { raw ->
                val lower = raw.lowercase()
                raw.length >= 4 && lower.all { c -> c.isLetter() || c == '-' || c == '\'' } &&
                    !StopWords.isStopWord(lower) && lower !in alreadyUsed
            }

        if (commonCandidates.isEmpty()) return null
        val best = commonCandidates.minByOrNull { freq[it.lowercase()] ?: Int.MAX_VALUE } ?: return null
        return AnswerPick(best, AnswerType.COMMON_WORD)
    }
}
