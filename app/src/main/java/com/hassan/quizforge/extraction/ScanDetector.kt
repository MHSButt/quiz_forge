package com.hassan.quizforge.extraction

/**
 * Decides whether a single PDF page's extracted text layer is "real" or whether the page is
 * effectively just an image (a scan/photo) with no usable embedded text, in which case it's a
 * candidate for OCR.
 */
object ScanDetector {

    // A page with real embedded text almost always has well more than this many non-whitespace
    // characters, even a short/sparse one (a title page, a mostly-blank page). Pages that come
    // back right around 0 are the ones that are actually just a rendered image.
    private const val MIN_NON_WHITESPACE_CHARS = 25

    fun pageLooksScanned(pageText: String): Boolean =
        pageText.count { !it.isWhitespace() } < MIN_NON_WHITESPACE_CHARS
}
