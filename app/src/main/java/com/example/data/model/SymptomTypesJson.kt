package com.example.data.model

/**
 * Encoding/decoding helpers for [SymptomEntity.symptomTypesJson].
 *
 * The symptom catalogue is a fixed list, so a JSON array column is enough; the
 * activities and remedies (which users can extend) live in their own tables.
 *
 * This is a deliberately small, dependency-free JSON array implementation: the
 * analysis engine also runs in plain JVM unit tests where `org.json` is only a
 * stub. It supports exactly the shape we store (an array of strings) and
 * ignores other value types, matching the previous `JSONArray.opt` behaviour.
 */
object SymptomTypesJson {
    private const val EMPTY = "[]"

    fun encode(types: List<String>): String {
        val cleaned = types
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (cleaned.isEmpty()) return EMPTY
        return cleaned.joinToString(prefix = "[", postfix = "]", separator = ",") {
            encodeString(it)
        }
    }

    fun decode(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            Parser(json).parseArray()
        } catch (_: IllegalArgumentException) {
            // A corrupt/legacy value must not crash the UI; callers can fall
            // back to the primary symptomType.
            emptyList()
        }
    }

    /** Decodes [json] but falls back to [primary] for rows created before v2. */
    fun decodeOrPrimary(json: String, primary: String): List<String> {
        val decoded = decode(json)
        return decoded.ifEmpty { listOfNotNull(primary.takeIf { it.isNotBlank() }) }
    }

    private fun encodeString(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch < ' ') {
                        sb.append("\\u")
                        sb.append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class Parser(private val text: String) {
        private var index = 0

        fun parseArray(): List<String> {
            skipWhitespace()
            expect('[')
            skipWhitespace()
            val result = mutableListOf<String>()
            if (peek() == ']') {
                index++
            } else {
                while (true) {
                    skipWhitespace()
                    if (peek() == '"') {
                        val value = parseString().trim()
                        if (value.isNotEmpty()) result.add(value)
                    } else {
                        skipNonStringValue()
                    }
                    skipWhitespace()
                    when (peek()) {
                        ',' -> index++
                        ']' -> {
                            index++
                            break
                        }
                        else -> throw IllegalArgumentException("Expected , or ] at $index")
                    }
                }
            }
            skipWhitespace()
            if (index != text.length) {
                throw IllegalArgumentException("Trailing content at $index")
            }
            return result
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (index < text.length) {
                val ch = text[index++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (index >= text.length) {
                            throw IllegalArgumentException("Unterminated escape")
                        }
                        when (val escaped = text[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) {
                                    throw IllegalArgumentException("Bad unicode escape")
                                }
                                val code = text.substring(index, index + 4).toIntOrNull(16)
                                    ?: throw IllegalArgumentException("Bad unicode escape")
                                sb.append(code.toChar())
                                index += 4
                            }
                            else -> throw IllegalArgumentException("Unknown escape \\$escaped")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        /**
         * Skips a non-string value (null, number, boolean, object, array) until
         * the next comma or the closing bracket of the outer array.
         */
        private fun skipNonStringValue() {
            var depth = 0
            var inString = false
            var escaped = false
            while (index < text.length) {
                val ch = text[index]
                if (inString) {
                    when {
                        escaped -> escaped = false
                        ch == '\\' -> escaped = true
                        ch == '"' -> inString = false
                    }
                } else {
                    when (ch) {
                        '"' -> inString = true
                        '[', '{' -> depth++
                        ']', '}' -> {
                            if (depth == 0) return
                            depth--
                        }
                        ',' -> if (depth == 0) return
                    }
                }
                index++
            }
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun peek(): Char =
            if (index < text.length) text[index]
            else throw IllegalArgumentException("Unexpected end at $index")

        private fun expect(expected: Char) {
            if (peek() != expected) {
                throw IllegalArgumentException("Expected $expected at $index")
            }
            index++
        }
    }
}
