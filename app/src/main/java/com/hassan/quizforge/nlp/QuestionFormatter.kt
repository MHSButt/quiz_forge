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
    private val PERSON_NAME_PATTERN = Regex("^[A-Z][a-zA-Z.'-]+\\s+[A-Z][a-zA-Z.'-]+\$")

    /**
     * Tries to turn a declarative "X is/was Y" style sentence into a natural "What/Who is X?"
     * question with Y as the answer. Returns null if the sentence doesn't match that shape, or
     * the resulting subject/predicate look unusable.
     */
    fun tryPatternShortAnswer(sentence: String): PatternShortAnswer? {
        val trimmed = sentence.trim().trimEnd('.', ' ')
        val match = DEFINITION_PATTERN.find(trimmed) ?: return null

        var subject = match.groupValues[1].trim()
        val verb = match.groupValues[2].trim()
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

        val who = PERSON_NAME_PATTERN.matches(subject)

        // Lower-case the subject's first letter unless it looks like an acronym/proper noun
        // (i.e. its second character is also a capital, or it's a single capitalized word that
        // could be a name) - a plain sentence-case "The X" should read as "the X" mid-question.
        val lowerFirst = subject.length > 1 && subject[1].isLowerCase() && !who
        subject = if (lowerFirst) subject.replaceFirstChar { it.lowercase() } else subject

        val questionWord = if (who) "Who" else "What"
        val question = "$questionWord $verb $subject?"
        return PatternShortAnswer(question = question, answer = predicate.trimEnd('.'))
    }
}
