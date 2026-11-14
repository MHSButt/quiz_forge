package com.hassan.quizforge.extraction

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException

/**
 * Extracts plain text from a PDF's embedded text layer (i.e. not a scan/image), entirely
 * offline via PDFBox-Android. [com.hassan.quizforge.QuizForgeApplication] must have already
 * called PDFBoxResourceLoader.init() before this runs.
 */
object PdfTextExtractor {

    /**
     * Returns one string per page, in page order. A page that is a pure image/scan with no
     * embedded text layer will simply come back as an empty (or near-empty) string for that
     * index - that's intentional, and is exactly what [ScanDetector] uses to decide which
     * individual pages need OCR.
     */
    fun extractPerPage(context: Context, uri: Uri): List<String> {
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

                    (1..pageCount).map { pageNumber ->
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        stripper.getText(document)
                    }
                }
            }
        } catch (e: QuizForgeException) {
            throw e
        } catch (t: Throwable) {
            throw FileReadException(t)
        }
    }
}
