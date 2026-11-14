package com.hassan.quizforge.extraction

/** Base type for every "expected" failure this app can show a friendly message for. */
sealed class QuizForgeException(message: String) : Exception(message)

class UnsupportedFileTypeException(extension: String) : QuizForgeException(
    "Unsupported file type${if (extension.isNotBlank()) " (.$extension)" else ""}. " +
        "Please choose a PDF or Word (.docx) file."
)

class FileReadException(cause: Throwable) : QuizForgeException(
    "Couldn't read that file: ${cause.message ?: cause.javaClass.simpleName}"
)

class EmptyDocumentException(ocrAttempted: Boolean = false) : QuizForgeException(
    if (ocrAttempted) {
        "No readable text could be found in this document, even after running on-device text " +
            "recognition on it as a scanned PDF. The scan quality may be too low, the pages may " +
            "be blank, or the content isn't recognizable text."
    } else {
        "No readable text was found in this document."
    }
)

class NotEnoughContentException : QuizForgeException(
    "This document is too short, or too repetitive, to generate meaningful quiz questions " +
        "from. Try a longer or more detailed document."
)

/**
 * The on-device OCR language data file is missing from this build (it ships bundled in
 * assets/tessdata/ by default, so this normally only happens if that file was removed from a
 * custom build variant). See the README's "How OCR works" section.
 */
class OcrModelMissingException : QuizForgeException(
    "On-device text recognition isn't available in this build: the English language data " +
        "file for the OCR engine (assets/tessdata/eng.traineddata) is missing. See the " +
        "\"How OCR works\" section in the project's README."
)
