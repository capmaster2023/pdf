package com.pdfpocket.lite.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrService @Inject constructor(@ApplicationContext private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(uri: Uri): String = recognize(InputImage.fromFilePath(context, uri))
    suspend fun recognize(bitmap: Bitmap): String = recognize(InputImage.fromBitmap(bitmap, 0))

    private suspend fun recognize(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result.text) }
            .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    }
}
