package com.hassan.quizforge.nlp

/**
 * A deliberately simple, dependency-free sentence splitter. It won't be perfect on every
 * document (abbreviations, weird punctuation), but it's good enough to feed the question
 * generator - occasional mis-splits just turn into slightly odd candidate sentences that the
 * candidate scorer is likely to skip anyway.
 */
object SentenceSplitter {

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc", "fig", "figs", "no",
        "vol", "pp", "approx", "apt", "dept", "est", "min", "max", "mt", "ave", "blvd", "inc",
        "ltd", "co", "corp", "e.g", "i.e"
    )

    // Split after ., !, or ? when followed by whitespace and then a capital letter, a quote, or
    // an opening parenthesis - a reasonable proxy for "a new sentence starts here".
    private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+(?=[A-Z0-9\"'(\\u201C])")

    fun split(cleanedText: String): List<String> {
        val paragraphs = cleanedText.split("\n")
        val sentences = mutableListOf<String>()

        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isEmpty()) continue

            val rough = trimmedParagraph.split(SENTENCE_BOUNDARY)
            for (piece in rough) {
                val candidate = piece.trim()
                if (candidate.isEmpty()) continue

                val lastSentence = sentences.lastOrNull()
                val lastWord = lastSentence
                    ?.trim()
                    ?.substringAfterLast(' ')
                    ?.trimEnd('.')
                    ?.lowercase()

                if (lastSentence != null && lastWord in ABBREVIATIONS) {
                    // Merge back - the previous "sentence" actually ended on an abbreviation.
                    sentences[sentences.lastIndex] = "$lastSentence $candidate"
                } else {
                    sentences.add(candidate)
                }
            }
        }

        return sentences
    }
}
