package com.fws.smarttrace.vision

import android.graphics.Bitmap
import com.fws.smarttrace.core.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/** Coarse on-device image labels, used only as a weak hint for the product category. */
object ImageLabelEngine {
    suspend fun labels(bmp: Bitmap): List<String> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.65f).build())
        return try {
            labeler.process(InputImage.fromBitmap(bmp, 0)).await().take(5).map { it.text }
        } catch (e: Exception) {
            emptyList()
        } finally {
            labeler.close()
        }
    }

    fun categoryFromHints(hints: List<String>): String? {
        val h = hints.joinToString(" ").lowercase()
        return when {
            Regex("tile|floor|flooring|brick|ceramic").containsMatchIn(h) -> "tile"
            Regex("paint").containsMatchIn(h) -> "paint"
            Regex("food|drink|snack|beverage|bottle|fruit|dish").containsMatchIn(h) -> "food"
            Regex("cosmetic|perfume|lipstick|skin care").containsMatchIn(h) -> "cosmetic"
            Regex("electronic|gadget|phone|cable").containsMatchIn(h) -> "electronics"
            else -> null
        }
    }
}
