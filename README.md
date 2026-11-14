# QuizForge

An Android app that turns a PDF or Word (.docx) document into a quiz - multiple-choice
questions and short-answer questions - **entirely on the device, with no internet connection
at all.** This includes scanned/photographed PDF pages: those are read with on-device OCR, not
sent anywhere.

There is no cloud AI model and no server call anywhere in this app. `AndroidManifest.xml`
deliberately does not request the `INTERNET` permission, which means Android itself blocks the
app from opening any network socket - this isn't just a design choice, it's enforced by the OS.
Question generation is done with plain rule-based text processing (sentence splitting,
word-frequency scoring, pattern matching), which is fast and runs instantly on any phone. The
one place this app does run a model is OCR for scanned pages, and even that runs fully
on-device via Tesseract (not Google ML Kit, which would require Play Services and an internet
connection the first time it downloads its model) - see "How OCR works" below.

## What it does

1. You pick a PDF or `.docx` file from your phone's storage.
2. The app extracts the plain text from it (PDFBox-Android for PDFs; a small hand-written
   `.docx` XML reader for Word files - no Apache POI, which is too heavy for Android), and scans
   it for chapter/section headings (see "Chapter and page-range selection" below). For a PDF, it
   also checks *each page* for a usable embedded text layer at this stage - any page that
   doesn't have one is handled later, only if it's actually part of what you selected (see
   "How OCR works" below).
3. On a "Set up your quiz" screen, you choose:
   - **What to include**: the whole document, one or more auto-detected chapters, or (PDF only)
     a custom page range.
   - **How many questions**: separate counts for multiple-choice and short-answer questions,
     up to 300 of each.
4. A rule-based pipeline (see `nlp/` below) scores every sentence in the selected text for how
   "quiz-worthy" it looks, picks good answer phrases out of the best sentences, and builds:
   - **Multiple-choice questions**: the sentence with its key phrase blanked out, plus 3
     plausible wrong answers pulled from elsewhere in the same selection.
   - **Short-answer questions**: wherever a sentence has a "X is/was Y" shape, a natural
     "What/Who is X?" question with Y as the answer; otherwise a simple fill-in-the-blank
     question.
5. You can answer the MCQs right in the app (tap an option to see if you got it right) and
   self-check the short-answer questions against a revealed answer, like flashcards.
6. You can export the whole generated quiz (questions + answer key) as a `.txt` file via the
   download icon on the quiz screen.

## Chapter and page-range selection

Instead of always generating questions from the entire document, you can narrow it down:

- **Chapters**: right after you pick a file, the app scans it for headings and offers them as
  checkboxes. Detection is a best-effort heuristic, not a real document outline reader - it
  looks for two things:
  - A line that reads like `Chapter 3`, `Section II`, `Part 1`, `Unit 4`, `Lesson 5`, or
    `Module 2` (case-insensitive, roman or arabic numerals), on its own line, under 80
    characters. This works for both PDFs (checked per page) and `.docx` files (checked per
    paragraph).
  - For `.docx` files only, a paragraph that Word itself styled as `Heading 1`/`Heading 2`/.../
    `Title` via the Styles panel - this catches documents that use Word's real heading styles
    instead of typing the word "Chapter".
  - If no headings are detected at all, the chapter option simply doesn't appear, and you fall
    back to the whole document (or, for a PDF, a custom page range).
- **Custom page range (PDF only)**: enter a "from" and "to" page number directly. `.docx` files
  don't have fixed pages the way a rendered PDF does, so this option isn't offered for them -
  use chapter selection instead, or the whole document.
- **Question counts**: each of the multiple-choice and short-answer fields accepts up to 300.
  A short or narrow selection may still yield fewer questions than you asked for - the app
  never repeats itself or invents content just to hit a number.

For PDFs, OCR (see below) only ever runs on the pages that end up inside your final selection,
not the whole document - so picking one 10-page chapter out of a 300-page scanned book OCRs
just those 10 pages, not all 300.

## How OCR works

Scanned/photographed PDF pages are recognized with [Tesseract](https://github.com/tesseract-ocr/tesseract)
via the [tesseract4android](https://github.com/adaptech-cz/Tesseract4Android) wrapper library -
a native on-device OCR engine, not a cloud service, not Google ML Kit (ML Kit requires Google
Play Services and downloads its model from the internet the first time it runs, which would
mean this app could no longer honestly claim to need zero internet access - that's the whole
reason Tesseract was used instead).

The English trained-data file it needs (`eng.traineddata`, ~4 MB) is already bundled at
`app/src/main/assets/tessdata/eng.traineddata`, so OCR works out of the box with no setup. The
app copies it into app-private storage the first time OCR runs (Tesseract needs a real
filesystem path, not a path inside the APK) and reuses that copy after that - still with no
network involved.

**Adding another language:** download the matching `<lang>.traineddata` file from
[tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) (smaller/faster, recommended
for a phone) or [tessdata_best](https://github.com/tesseract-ocr/tessdata_best)
(larger/slower, more accurate) into `app/src/main/assets/tessdata/`, then update
`TessDataManager` and the `tess.init(tesseractRoot.absolutePath, "eng")` call in
`OcrTextExtractor` to handle the extra language code(s) (Tesseract accepts multiple languages
at once, e.g. `"eng+fra"`).

**Reducing APK size:** the OCR library bundles native code for every Android ABI
(armeabi-v7a/arm64-v8a/x86/x86_64), which is the main contributor to this app's size. If you
only need to support modern 64-bit devices, add this to `app/build.gradle.kts` under
`defaultConfig`:

```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")
}
```

## Honest limitations

- **OCR accuracy depends on scan quality.** A clean, well-lit, straight scan at a decent
  resolution works well. A blurry photo taken at an angle, low-contrast text, or handwriting
  will OCR poorly or not at all - that's a property of OCR in general, not something this app
  can fully work around. OCR also only reads printed/typed text, not handwriting.
- **OCR is slow and only supports English out of the box.** Expect on the order of a few
  seconds per scanned page on a mid-range phone (the app shows per-page progress while this
  runs). See "How OCR works" for how to add more languages.
- **Legacy `.doc` (old binary Word format) isn't supported**, only the modern `.docx` XML
  format. Converting a `.doc` to `.docx` in Word/Google Docs first works fine. A `.docx` whose
  content is itself just an embedded image of text (no real text run, no OCR fallback for that
  case) also isn't supported - that's a much rarer case than scanned PDFs, so it was left out of
  scope.
- **Question quality is "pretty good", not "perfectly human-written".** Because this uses
  heuristics instead of an AI model, you'll occasionally get a slightly awkward question, or a
  distractor option that's a bit too obviously wrong. That's the trade-off for a zero-download,
  instant, fully offline app. See "Tuning the question quality" below if you want to improve it
  further.
- The launcher icon is a simple placeholder vector (see `app/src/main/res/drawable/
  ic_launcher_foreground.xml`) - swap it any time via Android Studio's Image Asset tool
  (right-click `res` -> New -> Image Asset).

## Project structure

```
app/src/main/java/com/hassan/quizforge/
  QuizForgeApplication.kt      Application class - initializes PDFBox-Android once at startup
  MainActivity.kt              Single Activity, hosts the Compose UI
  extraction/                  Turning a picked file into plain text
    FileTypeDetector.kt          Detects PDF vs DOCX vs unsupported from a content:// Uri
    PdfTextExtractor.kt          Per-page PDF text extraction via PDFBox-Android
    DocxTextExtractor.kt         .docx text extraction via a hand-rolled zip+XML reader (per paragraph, with heading-style detection)
    ChapterDetector.kt            Best-effort chapter/heading detection for both PDF pages and .docx paragraphs
    ScanDetector.kt               Decides whether a page's text layer is "real" or missing
    TessDataManager.kt            Copies the bundled OCR language model into app storage once
    OcrTextExtractor.kt            Renders scanned PDF pages to bitmaps and OCRs them (Tesseract)
    ExtractionExceptions.kt      Friendly, typed error messages
  nlp/                          The rule-based question generator (no AI model)
    TextCleaner.kt                Whitespace/line cleanup
    SentenceSplitter.kt           Splits cleaned text into sentences
    WordFrequencyAnalyzer.kt      Term-frequency scoring
    VocabPools.kt                 Document-wide pools of proper nouns / numbers / common words
    CandidateSentenceSelector.kt  Ranks sentences by "quiz-worthiness"
    AnswerPhraseSelector.kt       Picks the best word/phrase to blank out in one sentence
    DistractorGenerator.kt        Picks plausible wrong MCQ options
    QuestionFormatter.kt          Turns a sentence + answer into a question string
    QuestionGenerator.kt          Orchestrates all of the above into a GeneratedQuiz
  model/Question.kt             Plain data classes: McqQuestion, ShortAnswerQuestion, GeneratedQuiz
  model/DocumentChapter.kt      A detected chapter: title + start/end page or paragraph index
  viewmodel/QuizViewModel.kt    Holds UI state; two-phase flow (read+detect, then select+generate) off the main thread
  ui/                           Jetpack Compose screens
    QuizForgeApp.kt               Top-level composable; wires up file pickers and screen navigation
    screens/HomeScreen.kt         Just the file picker
    screens/ConfigureScreen.kt    Choose chapters/page range and question counts (up to 300 each)
    screens/QuizScreen.kt         Tabs for MCQ / short-answer results
    components/                   Individual question cards
    theme/                        Material 3 theme (colors, type)
  util/ExportFormatter.kt       Formats a GeneratedQuiz as a plain-text study sheet

.github/workflows/build-apk.yml  Builds a debug APK on GitHub's servers (see Option C below)
```

## How to build and run it

### Option A - Android Studio (recommended)

1. Install [Android Studio](https://developer.android.com/studio) (any recent version that
   bundles JDK 17, e.g. Koala or newer).
2. Open the `QuizForge` folder as a project (`File -> Open`, select this folder).
3. Let Gradle sync - it will download the Android Gradle Plugin, Kotlin, Compose,
   PDFBox-Android and the OCR library's dependencies the first time (this is the only step in
   the whole workflow that needs the internet - building the app, not running it).
4. Plug in an Android phone (with USB debugging enabled) or start an emulator, and click Run.

### Option B - command line

From this folder, with `ANDROID_HOME`/`ANDROID_SDK_ROOT` pointing at an installed Android SDK:

```bash
./gradlew assembleDebug
```

The resulting APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

(`gradlew.bat` is included for Windows.)

### Option C - let GitHub build it for you (no local Android setup at all)

This project includes a ready-to-use GitHub Actions workflow
(`.github/workflows/build-apk.yml`) that builds an installable debug APK on GitHub's own
servers - useful if you don't have Android Studio installed anywhere. Those servers have full
internet access to Google's and Maven Central's repositories, which is all this build needs.

1. Create a new repository on [GitHub](https://github.com/new) (private is fine) and push this
   project to it:
   ```bash
   cd QuizForge
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. Open your repo on github.com and click the **Actions** tab. The "Build debug APK" workflow
   starts automatically on push (if it doesn't, click it in the sidebar and use **Run workflow**).
3. Wait for the run to finish (a green checkmark, usually a few minutes), then open it and
   scroll down to **Artifacts**. Download `QuizForge-debug-apk` - it's a zip containing
   `app-debug.apk`.
4. Get that APK onto your phone however's convenient (email it to yourself, upload it to
   Drive, `adb install app-debug.apk` over USB, etc.) and open it to install. Android will ask
   you to allow installing from whatever app you used to open it, since it isn't from the Play
   Store - that's expected for a debug build you built yourself.

This debug APK is signed with Android's standard shared debug key, which is perfectly fine for
installing on your own device(s). It is **not** suitable for distributing to other people or
publishing anywhere - that needs a real release signing key, which is intentionally outside the
scope of what this workflow sets up.

### Requirements

- JDK 17 (Android Studio bundles one; otherwise install Temurin 17).
- Android SDK with `compileSdk`/`targetSdk` 35 installed (Android Studio's SDK Manager handles
  this, or `sdkmanager "platforms;android-35"` on the command line).
- A device or emulator running Android 8.0 (API 26) or newer - that's the `minSdk`, chosen
  because it's what lets the app use the modern adaptive-icon format and covers the vast
  majority of real devices.

## Tuning the question quality

Everything that affects question quality lives in `nlp/`, with no Android dependencies, so you
can tweak and reason about it in isolation:

- Want fewer, higher-quality questions? Raise the thresholds in
  `CandidateSentenceSelector.rank()` (e.g. require a higher `score`, or a longer `wordCount`
  minimum).
- Want better distractors? `DistractorGenerator` is the place to make the "wrong answer" pool
  smarter (e.g. prefer distractors with a similar word length to the correct answer).
- Want more natural-sounding short-answer questions? `QuestionFormatter.tryPatternShortAnswer()`
  only recognizes "X is/are/was/were/means/refers to Y" sentences right now - add more patterns
  (e.g. "X causes Y", "X occurs when Y") to catch more sentence shapes.

## Privacy

No analytics, no crash reporting, no ads, no accounts, no network permission in the manifest at
all. Every document you process - including OCR on scanned pages - stays on your device.

## Third-party components

- [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) - Apache License 2.0.
- [Tesseract OCR](https://github.com/tesseract-ocr/tesseract) and the
  [tesseract4android](https://github.com/adaptech-cz/Tesseract4Android) wrapper - Apache
  License 2.0.
- `eng.traineddata` - from [tesseract-ocr/tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast),
  Apache License 2.0.
- AndroidX / Jetpack Compose - Apache License 2.0.

If you ship this app publicly, keep an entry for each of these (and whatever else you add) in
an open-source notices screen - that's a legal courtesy these licenses ask for, not something
this README can do for you.
# quiz_forge
