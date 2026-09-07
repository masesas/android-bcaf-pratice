package com.masesas.exercise.bcaf.bc.pratice.presentation.designsystem.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private const val GROUP_SIZE = 3

/** Pemisah ribuan gaya Indonesia. */
const val THOUSAND_SEPARATOR: Char = '.'

/**
 * Menampilkan deretan angka dengan pemisah ribuan tanpa mengubah nilai yang disimpan.
 *
 * Isi field tetap berupa digit polos (`"25000000"`) sementara yang terlihat pengguna
 * adalah `"25.000.000"`. Pemetaan posisi kursor dihitung dari hasil pemformatan itu
 * sendiri, sehingga kursor tetap berada di tempat yang benar saat menyisip atau
 * menghapus digit di tengah angka.
 */
class ThousandSeparatorVisualTransformation(
    private val separator: Char = THOUSAND_SEPARATOR
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter(Char::isDigit)
        val formatted = StringBuilder()
        val originalToTransformed = IntArray(digits.length + 1)

        digits.forEachIndexed { index, digit ->
            if (index > 0 && (digits.length - index) % GROUP_SIZE == 0) {
                formatted.append(separator)
            }
            // Dicatat setelah pemisah ditambahkan supaya kursor berhenti tepat
            // di depan digitnya, bukan di depan titik pemisah.
            originalToTransformed[index] = formatted.length
            formatted.append(digit)
        }
        originalToTransformed[digits.length] = formatted.length

        val transformedToOriginal = IntArray(formatted.length + 1)
        var digitCount = 0
        formatted.forEachIndexed { index, char ->
            transformedToOriginal[index] = digitCount
            if (char.isDigit()) digitCount++
        }
        transformedToOriginal[formatted.length] = digitCount

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, digits.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, formatted.length)]
        }

        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }

    override fun equals(other: Any?): Boolean =
        other is ThousandSeparatorVisualTransformation && other.separator == separator

    override fun hashCode(): Int = separator.hashCode()
}
