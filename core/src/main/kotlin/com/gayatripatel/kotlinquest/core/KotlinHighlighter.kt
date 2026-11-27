package com.gayatripatel.kotlinquest.core

/**
 * A small single-pass Kotlin lexer for the code cards. No regex and no library, so it's cheap enough to run
 * during composition. It returns styled spans only; any text between spans is plain.
 *
 * Null-safety operators (`?.`, `?:`, `!!`) get their own kind so the UI can make them stand out:
 * they're what most of the first unit is about.
 */
object KotlinHighlighter {

    enum class Kind { KEYWORD, STRING, NUMBER, COMMENT, TYPE, FUNCTION, ANNOTATION, NULL_OP }

    data class Span(val kind: Kind, val start: Int, val end: Int)

    private val keywords = setOf(
        "val", "var", "fun", "if", "else", "when", "for", "while", "do", "return", "class", "object",
        "interface", "data", "sealed", "enum", "open", "abstract", "override", "private", "public", "internal",
        "protected", "companion", "in", "is", "as", "null", "true", "false", "this", "super", "try", "catch",
        "finally", "throw", "import", "package", "suspend", "inline", "reified", "lateinit", "const", "by",
        "init", "typealias", "out", "vararg", "break", "continue",
    )

    fun tokenize(code: String): List<Span> {
        val spans = mutableListOf<Span>()
        val n = code.length
        var i = 0

        fun add(kind: Kind, start: Int, end: Int) {
            if (end > start) spans += Span(kind, start, end)
        }

        /** End of a quoted literal starting at [start]; stops at the line end if it's never closed. */
        fun quotedEnd(start: Int, quote: Char): Int {
            var j = start + 1
            while (j < n && code[j] != quote && code[j] != '\n') {
                if (code[j] == '\\') j++
                j++
            }
            return if (j < n && code[j] == quote) j + 1 else j.coerceAtMost(n)
        }

        while (i < n) {
            val c = code[i]
            when {
                code.startsWith("//", i) -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) n else it }
                    add(Kind.COMMENT, i, end); i = end
                }
                code.startsWith("/*", i) -> {
                    val close = code.indexOf("*/", i + 2)
                    val end = if (close == -1) n else close + 2
                    add(Kind.COMMENT, i, end); i = end
                }
                code.startsWith("\"\"\"", i) -> {
                    val close = code.indexOf("\"\"\"", i + 3)
                    val end = if (close == -1) n else close + 3
                    add(Kind.STRING, i, end); i = end
                }
                c == '"' || c == '\'' -> {
                    val end = quotedEnd(i, c)
                    add(Kind.STRING, i, end); i = end
                }
                code.startsWith("?.", i) || code.startsWith("?:", i) || code.startsWith("!!", i) -> {
                    add(Kind.NULL_OP, i, i + 2); i += 2
                }
                c == '@' && i + 1 < n && code[i + 1].isLetter() -> {
                    var j = i + 1
                    while (j < n && code[j].isIdentPart()) j++
                    add(Kind.ANNOTATION, i, j); i = j
                }
                c.isDigit() -> {
                    var j = i + 1
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' ||
                            (code[j] == '.' && j + 1 < n && code[j + 1].isDigit()))
                    ) j++
                    add(Kind.NUMBER, i, j); i = j
                }
                c.isLetter() || c == '_' -> {
                    var j = i + 1
                    while (j < n && code[j].isIdentPart()) j++
                    val word = code.substring(i, j)
                    var k = j
                    while (k < n && code[k] == ' ') k++
                    val calls = k < n && (code[k] == '(' || code[k] == '{')
                    val kind = when {
                        word in keywords -> Kind.KEYWORD
                        word.first().isUpperCase() -> Kind.TYPE
                        calls -> Kind.FUNCTION
                        else -> null
                    }
                    if (kind != null) add(kind, i, j)
                    i = j
                }
                else -> i++
            }
        }
        return spans
    }

    private fun Char.isIdentPart() = isLetterOrDigit() || this == '_'
}
