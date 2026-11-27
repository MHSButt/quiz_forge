package com.hassan.quizforge.extraction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.IOException

/**
 * Runs on-device OCR (Tesseract, via the tesseract4android library) on specific pages of a
 * PDF - used for pages that [ScanDetector] decided have no usable embedded text layer, i.e.
 * pages that are really just a scanned image. Entirely local: no network call, no Play
 * Services, no cloud model. The only one-time setup cost is the trained language data file
 * bundled in assets/tessdata (see [TessDataManager]).
 */
object OcrTextExtractor {

    private const val TARGET_DPI = 300
    private const val POINTS_PER_INCH = 72f
    private const val MAX_DIMENSION_PX = 3000

    /** DPI for the quick chapter-heading pass - headings are large, so this is plenty. */
    private const val HEADER_SCAN_DPI = 150
    /** Fraction of the page (from the top) read when looking for a chapter heading. */
    private const val HEADER_SCAN_FRACTION = 0.35f

    /**
     * @param pageIndices 0-based page indices to OCR (not necessarily every page in the doc).
     * @param onProgress called as (pages done so far, total pages being OCR'd), on whatever
     *   thread this function is called from.
     * @return a map of page index -> recognized text, only for the pages that were processed.
     */
    fun extractPages(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>,
        onProgress: (done: Int, total: Int) -> Unit
    ): Map<Int, String> = ocr(context, uri, pageIndices, TARGET_DPI, 1f, onProgress) { false }

    /**
     * Quick pass that reads only the top part of each page at a lower resolution - enough to
     * catch a "Chapter 3" heading on a scanned page, at a fraction of the cost of full OCR.
     * Stops early (returning what it has so far) once [shouldStop] returns true.
     */
    fun extractPageHeaders(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>,
        onProgress: (done: Int, total: Int) -> Unit,
        shouldStop: () -> Boolean
    ): Map<Int, String> =
        ocr(context, uri, pageIndices, HEADER_SCAN_DPI, HEADER_SCAN_FRACTION, onProgress, shouldStop)

    private fun ocr(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>,
        dpi: Int,
        topFraction: Float,
        onProgress: (done: Int, total: Int) -> Unit,
        shouldStop: () -> Boolean
    ): Map<Int, String> {
        if (pageIndices.isEmpty()) return emptyMap()

        val tesseractRoot = TessDataManager.ensureTrainedDataAvailable(context)

        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (t: Throwable) {
            throw FileReadException(t)
        } ?: throw FileReadException(IOException("Could not open this PDF for OCR"))

        val tess = TessBaseAPI()
        val initialized = tess.init(tesseractRoot.absolutePath, "eng")
        if (!initialized) {
            tess.recycle()
            pfd.close()
            throw OcrModelMissingException()
        }

        val results = LinkedHashMap<Int, String>()
        try {
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    for ((doneSoFar, pageIndex) in pageIndices.withIndex()) {
                        if (shouldStop()) break
                        if (pageIndex !in 0 until renderer.pageCount) continue
                        onProgress(doneSoFar + 1, pageIndices.size)

                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = renderPageToBitmap(page, dpi)
                            try {
                                tess.setImage(bitmap)
                                if (topFraction < 1f) {
                                    val height = (bitmap.height * topFraction).toInt().coerceAtLeast(1)
                                    tess.setRectangle(0, 0, bitmap.width, height)
                                }
                                results[pageIndex] = tess.getUTF8Text() ?: ""
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        } catch (e: QuizForgeException) {
            throw e
        } catch (t: Throwable) {
            throw FileReadException(t)
        } finally {
            tess.recycle()
        }

        return results
    }

    private fun renderPageToBitmap(page: PdfRenderer.Page, dpi: Int): Bitmap {
        // PdfRenderer page width/height are in points (1/72"); scale up to a DPI that gives
        // Tesseract enough resolution to recognize text reliably.
        val scale = dpi / POINTS_PER_INCH
        var width = (page.width * scale).toInt().coerceAtLeast(1)
        var height = (page.height * scale).toInt().coerceAtLeast(1)

        // Cap the longest side so a huge/unusual page size can't blow up memory.
        val longestSide = maxOf(width, height)
        if (longestSide > MAX_DIMENSION_PX) {
            val shrink = MAX_DIMENSION_PX.toFloat() / longestSide
            width = (width * shrink).toInt().coerceAtLeast(1)
            height = (height * shrink).toInt().coerceAtLeast(1)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // PDFs can have transparent backgrounds; OCR wants white.
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        return bitmap
    }
}
