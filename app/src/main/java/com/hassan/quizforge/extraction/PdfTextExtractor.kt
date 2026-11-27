package com.hassan.quizforge.extraction

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException

/** One bookmark from a PDF's outline: its title and the 0-based page it points to. */
data class PdfOutlineEntry(val title: String, val pageIndex: Int)

/** Per-page text plus the PDF's own bookmarks (empty if the file has none). */
data class PdfContent(val pages: List<String>, val outline: List<PdfOutlineEntry>)

/**
 * Extracts plain text from a PDF's embedded text layer (i.e. not a scan/image), entirely
 * offline via PDFBox-Android. [com.hassan.quizforge.QuizForgeApplication] must have already
 * called PDFBoxResourceLoader.init() before this runs.
 */
object PdfTextExtractor {

    /**
     * Returns one string per page, in page order, plus the document outline. A page that is a
     * pure image/scan with no embedded text layer will simply come back as an empty (or
     * near-empty) string for that index - that's intentional, and is exactly what [ScanDetector]
     * uses to decide which individual pages need OCR.
     */
    fun extract(context: Context, uri: Uri): PdfContent {
        val stream = try {
            context.contentResolver.openInputStream(uri)
        } catch (t: Throwable) {
            throw FileReadException(t)
        } ?: throw FileReadException(IOException("Could not open an input stream for this file"))

        return try {
            stream.use { input ->
                PDDocument.load(input).use { document ->
                    if (document.isEncrypted) {
                        throw FileReadException(
                            IOException("This PDF is password-protected and can't be opened")
                        )
                    }

                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    val pageCount = document.numberOfPages

                    val pages = (1..pageCount).map { pageNumber ->
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        stripper.getText(document)
                    }
                    PdfContent(pages, readOutline(document))
                }
            }
        } catch (e: QuizForgeException) {
            throw e
        } catch (t: Throwable) {
            throw FileReadException(t)
        }
    }

    /**
     * Reads the PDF's bookmarks - the author's own chapter list, far more reliable than any
     * text heuristic. Uses the top level of the outline, except when the top level is a single
     * wrapper entry (e.g. the book title) whose children are the real chapters. Never throws:
     * a broken outline just means falling back to text-based detection.
     */
    private fun readOutline(document: PDDocument): List<PdfOutlineEntry> = try {
        var level: PDOutlineNode? = document.documentCatalog.documentOutline
        var items = level?.children()?.toList().orEmpty()
        while (items.size == 1 && items[0].hasChildren()) {
            level = items[0]
            items = level.children().toList()
        }
        items.mapNotNull { item -> toEntry(document, item) }
    } catch (t: Throwable) {
        emptyList()
    }

    private fun toEntry(document: PDDocument, item: PDOutlineItem): PdfOutlineEntry? {
        val title = item.title?.trim().orEmpty()
        val page = try {
            item.findDestinationPage(document)
        } catch (t: Throwable) {
            null
        } ?: return null
        val index = document.pages.indexOf(page)
        if (index < 0 || title.isEmpty()) return null
        return PdfOutlineEntry(title, index)
    }
}
