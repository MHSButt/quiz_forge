package com.hassan.quizforge

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application entry point. The only thing it has to do is initialize PDFBox-Android's
 * resource loader once, before any PDF is parsed.
 */
class QuizForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
