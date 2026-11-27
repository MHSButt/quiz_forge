package com.hassan.quizforge.nlp

data class PatternShortAnswer(val question: String, val answer: String)

object QuestionFormatter {

    private const val BLANK = "_____"

    /** Replace exactly the first occurrence of [phrase] in [sentence] with a blank. */
    fun clozeStem(sentence: String, phrase: String): String {
        val index = sentence.indexOf(phrase)
        if (index < 0) return sentence.replace(phrase, BLANK, ignoreCase = true)
        return sentence.substring(0, index) + BLANK + sentence.substring(index + phrase.length)
    }

    fun clozeShortAnswerQuestion(sentence: String, answer: String): String {
        val stem = clozeStem(sentence, answer)
        return "Fill in the blank: \"$stem\""
    }

    // Subject [is/are/was/were/means/refers to] Predicate.
    private val DEFINITION_PATTERN = Regex(
        "^(.{3,80}?)\\s+(is|are|was|were|means|refers to)\\s+(.{3,160})$",
        RegexOption.IGNORE_CASE
    )
    // Two or more capitalized words in a row, e.g. "Albert Einstein" or "Marie Skłodowska Curie".
    private val PERSON_NAME_PATTERN = Regex("^[A-Z][a-zA-Z.'-]+(?:\\s+[A-Z][a-zA-Z.'-]+)+\$")
    private val TRAILING_RELATIVE_PRONOUN = Regex(
        "\\b(which|who|whom|whose|that)$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Tries to turn a declarative "X is/was Y" style sentence into a natural "What/Who is X?"
     * question with Y as the answer. Returns null if the sentence doesn't match that shape, or
     * the resulting subject/predicate look unusable.
     */
    fun tryPatternShortAnswer(sentence: String): PatternShortAnswer? {
        val trimmed = sentence.trim().trimEnd('.', ' ')
        val match = DEFINITION_PATTERN.find(trimmed) ?: return null

        var subject = match.groupValues[1].trim()
        val verb = match.groupValues[2].trim().lowercase()
        val predicate = match.groupValues[3].trim()

        if (subject.split(Regex("\\s+")).size > 8) return null
        if (predicate.split(Regex("\\s+")).size > 25) return null
        if (predicate.isBlank() || subject.isBlank()) return null
        // Avoid turning sentences that start with a pronoun ("It is...", "This is...") into
        // confusing questions - there'd be no real subject to ask about.
        val firstWord = subject.substringBefore(' ').lowercase()
        if (firstWord in setOf("it", "this", "that", "these", "those", "there", "he", "she", "they")) {
            return null
        }
        // The regex matches the *first* is/are/was/were/means/refers-to in the sentence, which
        // for a sentence with a subordinate or appositive clause ("Respiration, which releases
        // energy, is...") clips the "subject" mid-clause, producing a nonsensical question like
        // "What is Respiration, which?". A comma/semicolon in the captured subject, or the
        // subject trailing off on a relative pronoun, are both reliable signs of that - bail out
        // rather than ask a broken question (the cloze-style fallback picks this sentence up
        // with a plain fill-in-the-blank instead).
        if (subject.contains(',') || subject.contains(';')) return null
        if (TRAILING_RELATIVE_PRONOUN.containsMatchIn(subject)) return null

        val who = PERSON_NAME_PATTERN.matches(subject)

        // Lower-case the subject's first letter unless it looks like an acronym/proper noun
        // (i.e. its second character is also a capital, or it's a single capitalized word that
        // could be a name) - a plain sentence-case "The X" should read as "the X" mid-question.
        val lowerFirst = subject.length > 1 && subject[1].isLowerCase() && !who
        subject = if (lowerFirst) subject.replaceFirstChar { it.lowercase() } else subject

        // "is/are/was/were" splice directly into a natural question ("What is X?"). "means" and
        // "refers to" do NOT - splicing them the same way used to produce backwards, broken
        // English like "What means X?" or "What refers to X?" (literally asking what X is the
        // subject *of*, the opposite of the intended question). Those two need a real
        // do/does-support question instead.
        val question = when (verb) {
            "means" -> "${if (who) "Who" else "What"} does $subject mean?"
            "refers to" -> "${if (who) "Who" else "What"} does $subject refer to?"
            else -> "${if (who) "Who" else "What"} $verb $subject?"
        }
        return PatternShortAnswer(question = question, answer = predicate.trimEnd('.'))
    }
}
