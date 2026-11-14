package com.hassan.quizforge.nlp

/** Light cleanup of raw extracted text before sentence splitting. */
object TextCleaner {

    private val HYPHEN_LINEBREAK = Regex("(\\w)-\\n(\\w)") // "exam-\nple" -> "example"
    private val MULTI_WHITESPACE = Regex("[ \\t\\x0B\\f\\r]+")
    private val MULTI_NEWLINE = Regex("\\n{2,}")
    private val BULLET_PREFIX = Regex("^\\s*([•\\-*▪◦]|\\d+[.)])\\s+")

    fun clean(raw: String): String {
        var text = raw.replace("\u0000", " ")
        text = HYPHEN_LINEBREAK.replace(text) { it.groupValues[1] + it.groupValues[2] }
        text = text.lines().joinToString("\n") { line ->
            BULLET_PREFIX.replace(line, "").trim()
        }
        text = text.replace(MULTI_WHITESPACE, " ")
        text = text.replace(MULTI_NEWLINE, "\n")
        return text.trim()
    }
}
