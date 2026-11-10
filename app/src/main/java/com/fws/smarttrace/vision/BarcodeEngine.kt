package com.fws.smarttrace.vision

import android.graphics.Bitmap
import com.fws.smarttrace.core.await
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

data class ScannedCode(val value: String, val format: String, val isUrl: Boolean)

/** Finds 1D/2D codes in a still image, fully on device. */
object BarcodeEngine {
    private fun formatName(f: Int): String = when (f) {
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_QR_CODE -> "QR"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_CODABAR -> "Codabar"
        else -> "Code"
    }

    suspend fun scan(bmp: Bitmap): List<ScannedCode> {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        val scanner = BarcodeScanning.getClient(options)
        try {
            val small = ImageUtils.scaleDown(bmp, 1280)
            for (candidate in listOf(bmp, small).distinct()) {
                for (rot in intArrayOf(0, 90)) {
                    val found = runCatching { scanner.process(InputImage.fromBitmap(candidate, rot)).await() }.getOrNull().orEmpty()
                    val list = found.mapNotNull { b ->
                        val v = (b.rawValue ?: b.displayValue)?.trim().orEmpty()
                        if (v.isEmpty()) null
                        else ScannedCode(
                            v.take(500), formatName(b.format),
                            b.valueType == Barcode.TYPE_URL || v.startsWith("http://", true) || v.startsWith("https://", true)
                        )
                    }.distinctBy { it.value }
                    if (list.isNotEmpty()) return list.take(4)
                }
            }
            return emptyList()
        } finally {
            scanner.close()
        }
    }
}
