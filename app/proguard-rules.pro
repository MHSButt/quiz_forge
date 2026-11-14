# Add project specific ProGuard rules here.
# Release builds have minification disabled by default (see app/build.gradle.kts),
# so these rules are not exercised unless you turn isMinifyEnabled on.

# PDFBox-Android uses reflection and resource loading for font metrics; keep it intact
# if you enable shrinking/obfuscation.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**

# Tesseract4Android (OCR) has native/JNI methods that reflection and shrinking must not touch.
-keep class com.googlecode.tesseract.android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
