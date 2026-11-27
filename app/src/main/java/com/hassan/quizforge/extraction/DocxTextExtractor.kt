package com.hassan.quizforge.extraction

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * One paragraph of a .docx.
 *
 * @property headingLevel 0 for a Title, 1 for Heading 1 / outline level 1, 2 for Heading 2, ...
 *   or null for ordinary body text.
 * @property isTocEntry true for table-of-contents lines, which must never be mistaken for the
 *   chapters they point to.
 */
data class DocxParagraph(val text: String, val headingLevel: Int?, val isTocEntry: Boolean = false)

/**
 * Extracts text from a .docx file, paragraph by paragraph, without pulling in Apache POI
 * (which is heavy and not well suited to Android). A .docx is just a zip archive; this reads
 * the document body ("word/document.xml") and its style sheet ("word/styles.xml") using
 * Android's built-in XmlPullParser - no network, no third-party dependency.
 *
 * Heading levels come from the style sheet (each style's outline level or its "heading N" name,
 * following basedOn inheritance), so headings are recognised even when Word localises the style
 * ID (e.g. German "berschrift1") or the author uses a custom style based on a heading. That
 * per-paragraph structure is what lets [ChapterDetector] offer chapter selection for Word
 * documents, not just PDFs.
 */
object DocxTextExtractor {

    private const val DOCUMENT_ENTRY = "word/document.xml"
    private const val STYLES_ENTRY = "word/styles.xml"
    private const val BODY_TEXT_OUTLINE_LEVEL = 9

    private val HEADING_STYLE_ID = Regex("^heading\\s*(\\d)$", RegexOption.IGNORE_CASE)
    private val TITLE_STYLE = Regex("^title$", RegexOption.IGNORE_CASE)
    private val TOC_STYLE = Regex("^toc\\s*(\\d|heading)?$|^contents\\s*(\\d|heading)?$", RegexOption.IGNORE_CASE)

    private data class StyleInfo(val heading: Int?, val isToc: Boolean)

    private class RawStyle(
        val id: String,
        var name: String? = null,
        var basedOn: String? = null,
        var outlineLevel: Int? = null
    )

    fun extractParagraphs(context: Context, uri: Uri): List<DocxParagraph> {
        val stream = try {
            context.contentResolver.openInputStream(uri)
        } catch (t: Throwable) {
            throw FileReadException(t)
        } ?: throw FileReadException(IOException("Could not open an input stream for this file"))

        var documentXml: ByteArray? = null
        var stylesXml: ByteArray? = null
        try {
            stream.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null && (documentXml == null || stylesXml == null)) {
                        when (entry.name) {
                            DOCUMENT_ENTRY -> documentXml = zip.readBytes()
                            STYLES_ENTRY -> stylesXml = zip.readBytes()
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (t: Throwable) {
            throw FileReadException(t)
        }

        val body = documentXml ?: throw FileReadException(
            IOException("This doesn't look like a valid .docx file ($DOCUMENT_ENTRY not found)")
        )

        // A broken/missing style sheet shouldn't fail the whole import - style IDs are still
        // checked by name as a fallback.
        val styles = stylesXml?.let { runCatching { parseStyles(it) }.getOrNull() }.orEmpty()

        return try {
            parseDocumentXml(body, styles)
        } catch (t: Throwable) {
            throw FileReadException(t)
        }
    }

    /** Local (namespace-prefix-stripped) tag/attribute name, e.g. "w:t" -> "t". */
    private fun localName(name: String): String = name.substringAfterLast(':')

    private fun attributeValue(parser: XmlPullParser, localAttrName: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (localName(parser.getAttributeName(i)) == localAttrName) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun newParser(bytes: ByteArray): XmlPullParser =
        Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }

    /** Maps paragraph style ID -> resolved heading level / TOC flag. */
    private fun parseStyles(bytes: ByteArray): Map<String, StyleInfo> {
        val parser = newParser(bytes)
        val raw = HashMap<String, RawStyle>()
        var current: RawStyle? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "style" -> {
                        val id = attributeValue(parser, "styleId")
                        val type = attributeValue(parser, "type")
                        current = if (id != null && (type == null || type == "paragraph")) {
                            RawStyle(id).also { raw[id] = it }
                        } else null
                    }
                    "name" -> current?.name = attributeValue(parser, "val")
                    "basedOn" -> current?.basedOn = attributeValue(parser, "val")
                    "outlineLvl" -> current?.outlineLevel = attributeValue(parser, "val")?.toIntOrNull()
                }
                XmlPullParser.END_TAG -> if (localName(parser.name) == "style") current = null
            }
            event = parser.next()
        }

        fun isToc(style: RawStyle): Boolean =
            TOC_STYLE.matches(style.id) || style.name?.let { TOC_STYLE.matches(it) } == true

        fun ownHeading(style: RawStyle): Int? {
            style.outlineLevel?.let { lvl ->
                return if (lvl in 0 until BODY_TEXT_OUTLINE_LEVEL) lvl + 1 else -1 // -1 = explicitly body text
            }
            val name = style.name.orEmpty()
            HEADING_STYLE_ID.matchEntire(name)?.let { return it.groupValues[1].toInt() }
            HEADING_STYLE_ID.matchEntire(style.id)?.let { return it.groupValues[1].toInt() }
            if (TITLE_STYLE.matches(name) || TITLE_STYLE.matches(style.id)) return 0
            return null
        }

        return raw.values.associate { style ->
            var heading: Int? = null
            var toc = false
            var s: RawStyle? = style
            var hops = 0
            while (s != null && hops < 16) {
                toc = toc || isToc(s)
                if (heading == null) heading = ownHeading(s)
                s = s.basedOn?.let { raw[it] }
                hops++
            }
            style.id to StyleInfo(heading?.takeIf { it >= 0 && !toc }, toc)
        }
    }

    private fun styleInfoFor(styleId: String, styles: Map<String, StyleInfo>): StyleInfo {
        styles[styleId]?.let { return it }
        // No style sheet entry: fall back to recognising Word's built-in English style IDs.
        if (TOC_STYLE.matches(styleId)) return StyleInfo(null, true)
        HEADING_STYLE_ID.matchEntire(styleId)?.let { return StyleInfo(it.groupValues[1].toInt(), false) }
        if (TITLE_STYLE.matches(styleId)) return StyleInfo(0, false)
        return StyleInfo(null, false)
    }

    private fun parseDocumentXml(bytes: ByteArray, styles: Map<String, StyleInfo>): List<DocxParagraph> {
        val parser = newParser(bytes)

        val paragraphs = mutableListOf<DocxParagraph>()
        val currentText = StringBuilder()
        var insideTextRun = false
        var paragraphDepth = 0 // text boxes nest <w:p> inside <w:p>; only the outer one counts
        var styleHeading: Int? = null
        var directOutline: Int? = null
        var styleIsToc = false
        // One entry per open <w:sdt>; true when it is Word's generated table of contents.
        val sdtIsToc = ArrayDeque<Boolean>()

        fun flush() {
            val text = currentText.toString().trim()
            val toc = styleIsToc || sdtIsToc.any { it }
            val heading = when {
                toc -> null
                directOutline != null -> directOutline!!.takeIf { it in 0 until BODY_TEXT_OUTLINE_LEVEL }?.plus(1)
                else -> styleHeading
            }
            paragraphs.add(DocxParagraph(text, heading, toc))
            currentText.setLength(0)
            styleHeading = null
            directOutline = null
            styleIsToc = false
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "p" -> paragraphDepth++
                    "t" -> insideTextRun = true
                    "br", "tab", "cr" -> currentText.append(' ')
                    "sdt" -> sdtIsToc.addLast(false)
                    "docPartGallery" -> {
                        val gallery = attributeValue(parser, "val").orEmpty()
                        if (gallery.contains("Table of Contents", ignoreCase = true) && sdtIsToc.isNotEmpty()) {
                            sdtIsToc.removeLast()
                            sdtIsToc.addLast(true)
                        }
                    }
                    "pStyle" -> if (paragraphDepth == 1) {
                        val info = styleInfoFor(attributeValue(parser, "val").orEmpty(), styles)
                        styleHeading = info.heading
                        styleIsToc = info.isToc
                    }
                    "outlineLvl" -> if (paragraphDepth == 1) {
                        directOutline = attributeValue(parser, "val")?.toIntOrNull()
                    }
                }

                XmlPullParser.TEXT -> if (insideTextRun) {
                    currentText.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (localName(parser.name)) {
                    "t" -> insideTextRun = false
                    "sdt" -> sdtIsToc.removeLastOrNull()
                    "p" -> {
                        paragraphDepth = (paragraphDepth - 1).coerceAtLeast(0)
                        if (paragraphDepth == 0) flush() else currentText.append(' ')
                    }
                }
            }
            event = parser.next()
        }

        // Guard against a malformed/unusual file whose last paragraph never closes properly -
        // don't silently drop whatever text was collected.
        if (currentText.isNotBlank()) flush()

        return paragraphs
    }
}
