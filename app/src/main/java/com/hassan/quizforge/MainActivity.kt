package com.hassan.quizforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hassan.quizforge.ui.QuizForgeApp
import com.hassan.quizforge.ui.theme.QuizForgeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizForgeTheme {
                QuizForgeApp()
            }
        }
    }
}
