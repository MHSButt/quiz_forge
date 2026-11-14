package com.hassan.quizforge.extraction

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/** One paragraph of a .docx, with whether Word itself styled it as a heading/title. */
data class DocxParagraph(val text: String, val isStyledHeading: Boolean)

/**
 * Extracts text from a .docx file, paragraph by paragraph, without pulling in Apache POI
 * (which is heavy and not well suited to Android). A .docx is just a zip archive; this reads
 * the single entry that holds the document body ("word/document.xml") using Android's built-in
 * XmlPullParser - no network, no third-party dependency - and, for each paragraph, pulls out
 * its text (every <w:t> run) plus whether its paragraph style (<w:pStyle>) is one of Word's
 * built-in "HeadingN"/"Title" styles. That per-paragraph structure is what lets
 * [ChapterDetector] offer chapter selection for Word documents, not just PDFs.
 */
object DocxTextExtractor {

    private const val DOCUMENT_ENTRY = "word/document.xml"
    private val HEADING_STYLE_ID = Regex("^(heading\\d*|title)$", RegexOption.IGNORE_CASE)

    fun extractParagraphs(context: Context, uri: Uri): List<DocxParagraph> {
        val stream = try {
            context.contentResolver.openInputStream(uri)
        } catch (t: Throwable) {
            throw FileReadException(t)
        } ?: throw FileReadException(IOException("Could not open an input stream for this file"))

        val documentXmlBytes = try {
            stream.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    var found: ByteArray? = null
                    while (entry != null) {
                        if (entry.name == DOCUMENT_ENTRY) {
                            found = zip.readBytes()
                            break
                        }
                        entry = zip.nextEntry
                    }
                    found
                }
            }
        } catch (t: Throwable) {
            throw FileReadException(t)
        } ?: throw FileReadException(
            IOException("This doesn't look like a valid .docx file ($DOCUMENT_ENTRY not found)")
        )

        return try {
            parseDocumentXml(documentXmlBytes)
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

    private fun parseDocumentXml(bytes: ByteArray): List<DocxParagraph> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        val paragraphs = mutableListOf<DocxParagraph>()
        val currentText = StringBuilder()
        var insideTextRun = false
        var currentIsHeading = false
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "t" -> insideTextRun = true
                    "br", "tab", "cr" -> currentText.append(' ')
                    "pStyle" -> {
                        val styleId = attributeValue(parser, "val")
                        if (styleId != null && HEADING_STYLE_ID.matches(styleId)) {
                            currentIsHeading = true
                        }
                    }
                }

                XmlPullParser.TEXT -> if (insideTextRun) {
                    currentText.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (localName(parser.name)) {
                    "t" -> insideTextRun = false
                    "p" -> {
                        paragraphs.add(DocxParagraph(currentText.toString().trim(), currentIsHeading))
                        currentText.setLength(0)
                        currentIsHeading = false
                    }
                }
            }
            event = parser.next()
        }

        // Guard against a malformed/unusual file whose last paragraph never closes properly -
        // don't silently drop whatever text was collected.
        if (currentText.isNotBlank()) {
            paragraphs.add(DocxParagraph(currentText.toString().trim(), currentIsHeading))
        }

        return paragraphs
    }
}
