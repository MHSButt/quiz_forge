package com.hassan.quizforge.nlp

import com.hassan.quizforge.extraction.EmptyDocumentException
import com.hassan.quizforge.extraction.NotEnoughContentException
import com.hassan.quizforge.model.GeneratedQuiz
import com.hassan.quizforge.model.McqQuestion
import com.hassan.quizforge.model.ShortAnswerQuestion
import kotlin.random.Random

/**
 * Turns raw extracted document text into a [GeneratedQuiz], using nothing but rule-based text
 * processing (sentence splitting, word-frequency scoring, pattern matching). No AI model, no
 * network, 100% on-device.
 */
class QuestionGenerator(private val random: Random = Random.Default) {

    fun generate(rawText: String, mcqCount: Int, shortAnswerCount: Int): GeneratedQuiz {
        if (rawText.isBlank()) throw EmptyDocumentException()

        val cleaned = TextCleaner.clean(rawText)
        val sentences = SentenceSplitter.split(cleaned)
        if (sentences.size < 3) throw NotEnoughContentException()

        val freq = WordFrequencyAnalyzer.analyze(sentences)
        val pools = VocabPoolBuilder.build(sentences, freq)
        val ranked = CandidateSentenceSelector.rank(sentences, freq)
        if (ranked.isEmpty()) throw NotEnoughContentException()

        val usedAnswers = HashSet<String>()
        val usedSentenceIndices = HashSet<Int>()
        val mcqs = mutableListOf<McqQuestion>()
        val shortAnswers = mutableListOf<ShortAnswerQuestion>()
        var nextId = 1

        // Pass 1: natural "What/Who is X?" short-answer questions wherever the sentence shape
        // allows it - these read the most naturally, so grab them first.
        for (candidate in ranked) {
            if (shortAnswers.size >= shortAnswerCount) break
            if (candidate.index in usedSentenceIndices) continue

            val pattern = QuestionFormatter.tryPatternShortAnswer(candidate.sentence) ?: continue
            val answerKey = pattern.answer.lowercase()
            if (answerKey in usedAnswers) continue
            if (pattern.answer.split(Regex("\\s+")).size > 12) continue

            shortAnswers.add(ShortAnswerQuestion(nextId++, pattern.question, pattern.answer))
            usedAnswers.add(answerKey)
            usedSentenceIndices.add(candidate.index)
        }

        // Pass 2: multiple-choice questions from the remaining, still-unused sentences.
        for (candidate in ranked) {
            if (mcqs.size >= mcqCount) break
            if (candidate.index in usedSentenceIndices) continue

            val pick = AnswerPhraseSelector.select(candidate.sentence, freq, usedAnswers) ?: continue
            val distractors = DistractorGenerator.generate(pick, pools, usedAnswers, random)
            if (distractors.size < 3) continue // need 4 total options; skip if not enough variety

            val options = (distractors + pick.phrase).shuffled(random)
            val correctIndex = options.indexOf(pick.phrase)
            val stem = QuestionFormatter.clozeStem(candidate.sentence, pick.phrase)

            mcqs.add(McqQuestion(nextId++, stem, options, correctIndex, candidate.sentence))
            usedAnswers.add(pick.phrase.lowercase())
            usedSentenceIndices.add(candidate.index)
        }

        // Pass 3: top up any remaining short-answer slots with simple cloze-style questions.
        for (candidate in ranked) {
            if (shortAnswers.size >= shortAnswerCount) break
            if (candidate.index in usedSentenceIndices) continue

            val pick = AnswerPhraseSelector.select(candidate.sentence, freq, usedAnswers) ?: continue
            val question = QuestionFormatter.clozeShortAnswerQuestion(candidate.sentence, pick.phrase)

            shortAnswers.add(ShortAnswerQuestion(nextId++, question, pick.phrase))
            usedAnswers.add(pick.phrase.lowercase())
            usedSentenceIndices.add(candidate.index)
        }

        if (mcqs.isEmpty() && shortAnswers.isEmpty()) throw NotEnoughContentException()

        return GeneratedQuiz(sourceFileName = "", mcqs = mcqs, shortAnswers = shortAnswers)
    }
}
