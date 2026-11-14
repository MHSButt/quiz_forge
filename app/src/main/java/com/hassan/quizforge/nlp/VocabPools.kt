package com.hassan.quizforge.nlp

/** Document-wide pools of candidate answer/distractor material, grouped by "shape". */
data class VocabPools(
    val properNounPhrases: List<String>,
    val numbers: List<String>,
    val commonWords: List<String>
)

object VocabPoolBuilder {

    // One or more consecutive Capitalized words, e.g. "World War" or "Albert Einstein".
    private val PROPER_NOUN_RUN =
        Regex("\\b[A-Z][a-zA-Z.'-]*(?:\\s+[A-Z][a-zA-Z.'-]*)*\\b")
    private val NUMBER = Regex("\\b\\d[\\d,.]*%?\\b")

    fun build(sentences: List<String>, freq: Map<String, Int>): VocabPools {
        val properNouns = LinkedHashSet<String>()
        val numbers = LinkedHashSet<String>()

        for (sentence in sentences) {
            val words = sentence.trim().split(Regex("\\s+"))
            // Find proper-noun runs, but skip a run if it is only the very first word of the
            // sentence (that's just normal sentence-initial capitalization, not necessarily a
            // proper noun) *and* is a single word.
            for (match in PROPER_NOUN_RUN.findAll(sentence)) {
                val phrase = match.value.trim()
                val isSentenceStart = match.range.first == 0
                val wordCount = phrase.split(Regex("\\s+")).size
                if (phrase.length < 3) continue
                if (isSentenceStart && wordCount == 1) continue
                if (wordCount >= 2 || phrase.length > 4) {
                    properNouns.add(phrase)
                }
            }
            for (match in NUMBER.findAll(sentence)) {
                numbers.add(match.value)
            }
        }

        val commonWords = WordFrequencyAnalyzer.topCommonWords(sentences, freq, limit = 60)

        return VocabPools(
            properNounPhrases = properNouns.toList(),
            numbers = numbers.toList(),
            commonWords = commonWords
        )
    }
}
