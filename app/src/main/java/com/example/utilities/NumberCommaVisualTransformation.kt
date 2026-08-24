package com.example.utilities

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class NumberCommaVisualTransformation(private val isIndian: Boolean = false) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val isNegative = originalText.startsWith("-")
        val textWithoutSign = if (isNegative) originalText.substring(1) else originalText

        if (textWithoutSign.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = textWithoutSign.split(".")
        val intPart = parts[0]
        val fracPart = if (parts.size > 1) parts[1] else null
        val hasTrailingDot = textWithoutSign.endsWith(".")

        val formattedInt = StringBuilder()
        val intLen = intPart.length

        if (isIndian) {
            // Indian numbering: last 3 digits, then groups of 2
            var count = 0
            for (i in intLen - 1 downTo 0) {
                formattedInt.append(intPart[i])
                count++
                if (count == 3 && i > 0) {
                    formattedInt.append(',')
                } else if (count > 3 && (count - 3) % 2 == 0 && i > 0) {
                    formattedInt.append(',')
                }
            }
            formattedInt.reverse()
        } else {
            // International standard: groups of 3
            var count = 0
            for (i in intLen - 1 downTo 0) {
                formattedInt.append(intPart[i])
                count++
                if (count % 3 == 0 && i > 0) {
                    formattedInt.append(',')
                }
            }
            formattedInt.reverse()
        }

        val formattedString = buildString {
            if (isNegative) append('-')
            append(formattedInt)
            if (hasTrailingDot) {
                append('.')
            }
            if (fracPart != null) {
                append('.')
                append(fracPart)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, originalText.length)
                
                var transformed = 0
                var originalSeen = 0

                for (char in formattedString) {
                    if (originalSeen == clampedOffset) break
                    if (char != ',') {
                        originalSeen++
                    }
                    transformed++
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, formattedString.length)
                
                var originalCount = 0
                for (i in 0 until clampedOffset) {
                    if (formattedString[i] != ',') {
                        originalCount++
                    }
                }
                return originalCount.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedString), offsetMapping)
    }
}
