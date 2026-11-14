package com.hassan.quizforge.extraction

import com.hassan.quizforge.model.DocumentChapter

/**
 * Best-effort chapter/section detection, purely from text patterns (and, for .docx, paragraph
 * styles) - there's no real document outline to read, so this is a heuristic, not a guarantee.
 * It reliably catches the common convention of an explicit "Chapter 3", "Section 2", "Part I",
 * etc. heading on its own line. Anything else (an unlabeled heading, a creative title with no
 * "Chapter"/"Section" word, in a PDF) won't be picked up - that's exactly why there's always a
 * manual page-range fallback in the UI as well.
 */
object ChapterDetector {

    private val HEADING_REGEX = Regex(
        """^(chapter|section|part|unit|lesson|module)\s+([0-9]+|[ivxlcdm]+)\b.*""",
        RegexOption.IGNORE_CASE
    )

    /** True if this single line/paragraph looks like a "Chapter N ..." style heading. */
    fun matchesHeadingPattern(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.length > 80) return false
        return HEADING_REGEX.matches(trimmed)
    }

    /**
     * Scans each PDF page's (already-extracted, pre-OCR) text for the first line that looks
     * like a chapter heading. A chapter is deemed to run from the page its heading was found on
     * up to (but not including) the page the next heading was found on. Returns an empty list
     * if nothing was found - most PDFs without an explicit "Chapter N" convention will simply
     * have no auto-detected chapters, and that's fine; the UI falls back to a manual page range.
     *
     * Deliberately runs on the plain PDFBox text layer only (not OCR'd scanned pages) - it's
     * fast, and keeps chapter detection available immediately after picking a file, before any
     * slow OCR work happens.
     */
    fun detectPdfChapters(perPageText: List<String>): List<DocumentChapter> {
        val headingPages = mutableListOf<Pair<Int, String>>()
        perPageText.forEachIndexed { pageIndex, pageText ->
            val headingLine = pageText.lineSequence()
                .map { it.trim() }
                .firstOrNull { matchesHeadingPattern(it) }
            if (headingLine != null) {
                headingPages.add(pageIndex to headingLine)
            }
        }
        if (headingPages.isEmpty()) return emptyList()

        return headingPages.mapIndexed { i, (pageIndex, title) ->
            val end = if (i + 1 < headingPages.size) headingPages[i + 1].first - 1 else perPageText.lastIndex
            DocumentChapter(title = title, startIndex = pageIndex, endIndex = maxOf(pageIndex, end))
        }
    }

    /**
     * Same idea for a .docx, but the unit is a paragraph rather than a page, and a paragraph
     * counts as a heading if it matches the same "Chapter N" text pattern *or* Word itself
     * marked it with a Heading/Title style - covering both "typed a heading manually" and
     * "used Word's Styles panel properly" authoring habits.
     */
    fun detectDocxChapters(paragraphs: List<DocxParagraph>): List<DocumentChapter> {
        val headingParagraphs = paragraphs.withIndex()
            .filter { (_, p) -> p.isStyledHeading || matchesHeadingPattern(p.text) }
            .map { it.index to it.value.text.trim() }

        if (headingParagraphs.isEmpty()) return emptyList()

        return headingParagraphs.mapIndexed { i, (index, rawTitle) ->
            val end = if (i + 1 < headingParagraphs.size) headingParagraphs[i + 1].first - 1 else paragraphs.lastIndex
            val title = rawTitle.ifBlank { "Untitled section ${i + 1}" }
            DocumentChapter(title = title, startIndex = index, endIndex = maxOf(index, end))
        }
    }
}
