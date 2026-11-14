package com.hassan.quizforge.nlp

/** Simple term-frequency based "importance" scoring, computed once per document. */
object WordFrequencyAnalyzer {

    private val WORD_REGEX = Regex("[\\p{L}][\\p{L}'-]*")

    fun tokenize(sentence: String): List<String> = WORD_REGEX.findAll(sentence).map { it.value }.toList()

    /** lowercase word -> frequency across the whole document, stop words and short words excluded. */
    fun analyze(sentences: List<String>): Map<String, Int> {
        val freq = HashMap<String, Int>()
        for (sentence in sentences) {
            for (word in tokenize(sentence)) {
                val lower = word.lowercase()
                if (lower.length < 3 || StopWords.isStopWord(lower)) continue
                freq[lower] = (freq[lower] ?: 0) + 1
            }
        }
        return freq
    }

    /** Top [limit] most frequent non-stopword single words, original case of their first occurrence. */
    fun topCommonWords(sentences: List<String>, freq: Map<String, Int>, limit: Int): List<String> {
        val firstSeenCase = LinkedHashMap<String, String>()
        for (sentence in sentences) {
            for (word in tokenize(sentence)) {
                val lower = word.lowercase()
                if (lower.length < 4 || StopWords.isStopWord(lower)) continue
                firstSeenCase.putIfAbsent(lower, word)
            }
        }
        return firstSeenCase.entries
            .sortedByDescending { freq[it.key] ?: 0 }
            .take(limit)
            .map { it.value }
    }
}
