package com.hassan.quizforge.extraction

import com.hassan.quizforge.model.DocumentChapter

/**
 * Best-effort chapter/section detection.
 *
 * PDF, in order of reliability:
 *  1. The PDF's own bookmarks (outline), when it has them.
 *  2. Text headings like "Chapter 3", "CHAPTER THREE", "Unit IV", "Lesson-2" near the top of a
 *     page. Table-of-contents pages are skipped, and a heading repeated on later pages (a
 *     running header) only starts its chapter once.
 *
 * .docx: Word heading styles (resolved through the style sheet), using only the top heading
 * level so sub-sections don't split chapters apart, with the same text patterns as a fallback.
 *
 * Anything else (an unlabeled heading in a PDF with no bookmarks, a scanned PDF with no text
 * layer) won't be picked up - that's why there's always a manual page-range fallback in the UI.
 */
object ChapterDetector {

    private const val MAX_HEADING_LENGTH = 80
    private const val MAX_TITLE_SUFFIX_LENGTH = 60
    /** Chapter headings sit near the top of a page; looking further down mostly finds body text. */
    private const val LINES_TO_SCAN_PER_PAGE = 10
    /** A page with this many heading-like lines is a table of contents, not a chapter start. */
    private const val TOC_PAGE_HEADING_COUNT = 3

    private val NUMBER_WORDS = listOf(
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen", "twenty"
    )

    /** Preferred keyword first: when a document uses several, the first one present wins. */
    private val KEYWORD_PRIORITY = listOf("chapter", "unit", "module", "lesson", "part", "section")

    private val KEYWORD_PATTERN = "chapter|ch\\.|section|part|unit|lesson|module"

    // Roman numerals are matched upper-case only, so ordinary words like "did" or "mix" after
    // "Unit"/"Lesson" aren't read as numbers. "Section 2.1" (a sub-section) is rejected.
    private val HEADING_REGEX = Regex(
        "^(?i:($KEYWORD_PATTERN))\\s*[-–—:.#]?\\s*" +
            "(\\d{1,3}(?![.,]\\d)|[IVXLCDM]{1,7}|(?i:${NUMBER_WORDS.joinToString("|")}))" +
            "(?![\\p{L}\\d])(.*)$"
    )

    private val KEYWORD_ONLY_REGEX = Regex("^(?i:($KEYWORD_PATTERN))$")

    /** Dot leaders or a trailing page number: "Chapter 1 Introduction ........ 5". */
    private val TOC_LINE_REGEX = Regex("""(\.{3,}|…|\s\.(\s\.){2,}|\S\s+\d{1,4})\s*$""")

    private val WHITESPACE_REGEX = Regex("\\s+")

    private val CONTENTS_TITLE_REGEX = Regex("^(table of )?contents$", RegexOption.IGNORE_CASE)

    private data class Heading(val keyword: String, val number: Int, val title: String)

    // ---------------------------------------------------------------------------------------
    // PDF
    // ---------------------------------------------------------------------------------------

    /**
     * @param ocrPageHeaders OCR'd text from the top of scanned pages (page index -> text),
     *   used in place of those pages' empty text layer. Ignored when the PDF has bookmarks.
     */
    fun detectPdfChapters(
        content: PdfContent,
        ocrPageHeaders: Map<Int, String> = emptyMap()
    ): List<DocumentChapter> {
        val lastPage = content.pages.lastIndex
        if (lastPage < 0) return emptyList()

        if (hasUsableOutline(content)) {
            val outline = content.outline.filter { it.pageIndex in 0..lastPage }
            return buildChapters(outline.map { it.pageIndex to it.title }, lastPage)
        }

        val pages = if (ocrPageHeaders.isEmpty()) content.pages
        else content.pages.mapIndexed { i, text -> ocrPageHeaders[i] ?: text }
        return detectPdfChaptersFromText(pages)
    }

    /** True if the PDF's bookmarks are enough to define chapters - no text scanning needed. */
    fun hasUsableOutline(content: PdfContent): Boolean =
        content.outline.count { it.pageIndex in content.pages.indices } >= 2

    private fun detectPdfChaptersFromText(perPageText: List<String>): List<DocumentChapter> {
        val found = mutableListOf<Pair<Int, Heading>>()
        perPageText.forEachIndexed { pageIndex, pageText ->
            val lines = pageText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (looksLikeTocPage(lines)) return@forEachIndexed

            val heading = firstHeading(lines.take(LINES_TO_SCAN_PER_PAGE + 1))
            if (heading != null) found.add(pageIndex to heading)
        }
        return finishTextHeadings(found, perPageText.lastIndex)
    }

    private fun looksLikeTocPage(lines: List<String>): Boolean {
        if (lines.take(LINES_TO_SCAN_PER_PAGE).any { CONTENTS_TITLE_REGEX.matches(it) }) return true
        return lines.count { parseHeading(it, allowTocLine = true) != null } >= TOC_PAGE_HEADING_COUNT
    }

    /** First heading in [lines], joining "CHAPTER" / "3" split across two lines. */
    private fun firstHeading(lines: List<String>): Heading? {
        for (i in 0 until minOf(lines.size, LINES_TO_SCAN_PER_PAGE)) {
            val line = lines[i]
            val next = lines.getOrNull(i + 1)
            val joined = KEYWORD_ONLY_REGEX.matches(line) && next != null
            val candidate = if (joined) "$line $next" else line
            val heading = parseHeading(candidate) ?: continue
            return withTitleFromNextLine(heading, lines.getOrNull(if (joined) i + 2 else i + 1))
        }
        return null
    }

    // ---------------------------------------------------------------------------------------
    // DOCX
    // ---------------------------------------------------------------------------------------

    fun detectDocxChapters(paragraphs: List<DocxParagraph>): List<DocumentChapter> {
        val lastIndex = paragraphs.lastIndex
        if (lastIndex < 0) return emptyList()

        val styled = styledHeadingStarts(paragraphs)
        if (styled.size >= 2) return buildChapters(styled, lastIndex)

        val found = mutableListOf<Pair<Int, Heading>>()
        paragraphs.forEachIndexed { index, p ->
            if (p.isTocEntry) return@forEachIndexed
            val heading = parseHeading(p.text) ?: return@forEachIndexed
            val nextText = paragraphs.drop(index + 1).firstOrNull { it.text.isNotBlank() }?.text
            found.add(index to withTitleFromNextLine(heading, nextText))
        }
        val fromText = finishTextHeadings(found, lastIndex)
        if (fromText.isNotEmpty()) return fromText

        return buildChapters(styled, lastIndex)
    }

    /**
     * Uses the shallowest heading level that occurs at least twice, so a single document title
     * (Title / lone Heading 1) doesn't swallow everything, and Heading 2/3 sub-sections don't
     * chop chapters into fragments.
     */
    private fun styledHeadingStarts(paragraphs: List<DocxParagraph>): List<Pair<Int, String>> {
        val headings = paragraphs.withIndex()
            .filter { (_, p) -> p.headingLevel != null && !p.isTocEntry && p.text.isNotBlank() }
        if (headings.isEmpty()) return emptyList()

        val levels = headings.map { it.value.headingLevel!! }.distinct().sorted()
        val boundaryLevel = levels.firstOrNull { level ->
            headings.count { it.value.headingLevel!! <= level } >= 2
        } ?: levels.first()

        val selected = headings.filter { it.value.headingLevel!! <= boundaryLevel }
        // A lone Title above real headings is the document name, not a chapter.
        val withoutDocTitle = if (selected.count { it.value.headingLevel == 0 } == 1 && selected.size > 2) {
            selected.filterNot { it.value.headingLevel == 0 }
        } else selected

        return withoutDocTitle.map { it.index to it.value.text.trim() }
    }

    // ---------------------------------------------------------------------------------------
    // Shared
    // ---------------------------------------------------------------------------------------

    /** True if this single line/paragraph looks like a "Chapter N ..." style heading. */
    fun matchesHeadingPattern(line: String): Boolean = parseHeading(line) != null

    private fun parseHeading(rawLine: String, allowTocLine: Boolean = false): Heading? {
        val line = rawLine.trim().replace(WHITESPACE_REGEX, " ")
        if (line.isEmpty() || line.length > MAX_HEADING_LENGTH) return null
        val match = HEADING_REGEX.matchEntire(line) ?: return null

        val rest = match.groupValues[3].trim()
        if (!allowTocLine && rest.isNotEmpty() && TOC_LINE_REGEX.containsMatchIn(line)) return null
        // "Section 2 describes how the results were gathered." is a sentence, not a heading.
        if (rest.endsWith('.') && rest.split(' ').size > 6) return null

        val number = parseNumber(match.groupValues[2]) ?: return null
        val keyword = match.groupValues[1].lowercase().let { if (it == "ch.") "chapter" else it }
        return Heading(keyword, number, line)
    }

    /** "Chapter 3" alone on a line is usually followed by the chapter's name on the next one. */
    private fun withTitleFromNextLine(heading: Heading, nextLine: String?): Heading {
        val rest = HEADING_REGEX.matchEntire(heading.title)?.groupValues?.get(3)?.trim().orEmpty()
        val next = nextLine?.trim().orEmpty()
        if (rest.trim(':', '-', '–', '—').isNotEmpty() || next.isEmpty()) return heading
        if (next.length > MAX_TITLE_SUFFIX_LENGTH || next.endsWith('.') || parseHeading(next) != null) return heading
        if (next.all { it.isDigit() }) return heading
        return heading.copy(title = "${heading.title.trimEnd(':', ' ')}: $next")
    }

    private fun parseNumber(token: String): Int? {
        token.toIntOrNull()?.let { return it }
        NUMBER_WORDS.indexOf(token.lowercase()).takeIf { it >= 0 }?.let { return it + 1 }
        return romanToInt(token)
    }

    private fun romanToInt(token: String): Int? {
        if (!Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$").matches(token)) return null
        val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0
        for (i in token.indices) {
            val v = values.getValue(token[i])
            val nextV = token.getOrNull(i + 1)?.let { values.getValue(it) } ?: 0
            total += if (v < nextV) -v else v
        }
        return total.takeIf { it > 0 }
    }

    /**
     * Keeps one keyword (so "Section" headings inside chapters don't fragment them), and only
     * the first occurrence of each number (so running headers don't make every page a chapter).
     */
    private fun finishTextHeadings(found: List<Pair<Int, Heading>>, lastIndex: Int): List<DocumentChapter> {
        if (found.isEmpty()) return emptyList()

        val keyword = KEYWORD_PRIORITY.firstOrNull { k -> found.count { it.second.keyword == k } >= 2 }
            ?: KEYWORD_PRIORITY.first { k -> found.any { it.second.keyword == k } }

        val seenNumbers = HashSet<Int>()
        val starts = found
            .filter { it.second.keyword == keyword }
            .filter { seenNumbers.add(it.second.number) }
            .map { (index, heading) -> index to heading.title }

        return buildChapters(starts, lastIndex)
    }

    /**
     * Turns (start index, title) pairs into chapters. A chapter runs up to the unit before the
     * next chapter that starts *later*; two chapters starting on the same PDF page share it.
     */
    private fun buildChapters(starts: List<Pair<Int, String>>, lastIndex: Int): List<DocumentChapter> {
        val sorted = starts.sortedBy { it.first }
        return sorted.mapIndexed { i, (start, rawTitle) ->
            val nextStart = sorted.drop(i + 1).firstOrNull { it.first > start }?.first
            val end = (nextStart?.minus(1)) ?: lastIndex
            val title = rawTitle.ifBlank { "Untitled section ${i + 1}" }
            DocumentChapter(title = title, startIndex = start, endIndex = maxOf(start, end))
        }
    }
}
