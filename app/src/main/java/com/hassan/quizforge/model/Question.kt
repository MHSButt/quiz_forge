package com.hassan.quizforge.model

/**
 * A single multiple-choice question.
 *
 * @param stem the question text, with the chosen answer phrase blanked out (shown as "_____").
 * @param options exactly 4 possible answers, already shuffled.
 * @param correctIndex index into [options] of the correct answer.
 * @param sourceSentence the original sentence this question was derived from (shown as context
 *   / for debugging, not required by the UI but handy if you want to display "source" text).
 */
data class McqQuestion(
    val id: Int,
    val stem: String,
    val options: List<String>,
    val correctIndex: Int,
    val sourceSentence: String
) {
    val correctAnswer: String get() = options[correctIndex]
}

/**
 * A single short-answer question. There is no on-device grading - the app simply lets the
 * learner type their own answer and reveal the expected one to self-check, like a flashcard.
 */
data class ShortAnswerQuestion(
    val id: Int,
    val question: String,
    val answer: String
)

/**
 * The full result of processing one document.
 */
data class GeneratedQuiz(
    val sourceFileName: String,
    val mcqs: List<McqQuestion>,
    val shortAnswers: List<ShortAnswerQuestion>
)
