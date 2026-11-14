package com.hassan.quizforge.extraction

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Tesseract needs its trained language model on a real filesystem path (it can't read directly
 * out of the APK's assets), in a directory called "tessdata". This copies the bundled
 * eng.traineddata from assets/tessdata/ into app-private storage the first time OCR is used,
 * and reuses it after that.
 */
object TessDataManager {

    private const val LANGUAGE = "eng"
    private const val ASSET_PATH = "tessdata/$LANGUAGE.traineddata"

    /** Returns the directory Tesseract's TessBaseAPI.init() should be pointed at - the parent
     *  of a "tessdata" folder, not the "tessdata" folder itself. */
    fun ensureTrainedDataAvailable(context: Context): File {
        val tesseractRoot = File(context.filesDir, "tesseract")
        val tessDataDir = File(tesseractRoot, "tessdata")
        val targetFile = File(tessDataDir, "$LANGUAGE.traineddata")

        if (targetFile.exists() && targetFile.length() > 0L) {
            return tesseractRoot
        }

        tessDataDir.mkdirs()

        val assetStream = try {
            context.assets.open(ASSET_PATH)
        } catch (e: Exception) {
            throw OcrModelMissingException()
        }

        try {
            assetStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (t: Throwable) {
            targetFile.delete()
            throw FileReadException(t)
        }

        if (targetFile.length() == 0L) {
            targetFile.delete()
            throw OcrModelMissingException()
        }

        return tesseractRoot
    }
}
