package com.hassan.quizforge.model

/**
 * A best-effort detected chapter/section of a document.
 *
 * For a PDF, [startIndex]/[endIndex] are 0-based page indices (inclusive). For a .docx,
 * they're 0-based indices into the document's paragraph list (inclusive) - Word documents
 * don't have fixed pages the way a rendered PDF does, so paragraphs are the natural unit there.
 */
data class DocumentChapter(
    val title: String,
    val startIndex: Int,
    val endIndex: Int
)
