package com.gayatripatel.kotlinquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayatripatel.kotlinquest.core.KotlinHighlighter
import com.gayatripatel.kotlinquest.ui.theme.CodeColors
import com.gayatripatel.kotlinquest.ui.theme.Flame
import com.gayatripatel.kotlinquest.ui.theme.HeartRed
import com.gayatripatel.kotlinquest.ui.theme.KotlinPurple
import com.gayatripatel.kotlinquest.ui.theme.Locked

private fun styleFor(kind: KotlinHighlighter.Kind): SpanStyle = when (kind) {
    KotlinHighlighter.Kind.KEYWORD -> SpanStyle(color = CodeColors.keyword)
    KotlinHighlighter.Kind.STRING -> SpanStyle(color = CodeColors.string)
    KotlinHighlighter.Kind.NUMBER -> SpanStyle(color = CodeColors.number)
    KotlinHighlighter.Kind.COMMENT -> SpanStyle(color = CodeColors.comment)
    KotlinHighlighter.Kind.TYPE -> SpanStyle(color = CodeColors.type)
    KotlinHighlighter.Kind.FUNCTION -> SpanStyle(color = CodeColors.function)
    KotlinHighlighter.Kind.ANNOTATION -> SpanStyle(color = CodeColors.annotation)
    KotlinHighlighter.Kind.NULL_OP -> SpanStyle(color = CodeColors.nullOp, fontWeight = FontWeight.Bold)
}

/** Highlights [code]; [emphasis] marks a range (e.g. a filled-in blank) with a solid chip. */
fun highlight(code: String, emphasis: IntRange? = null): AnnotatedString = buildAnnotatedString {
    append(code)
    KotlinHighlighter.tokenize(code).forEach { addStyle(styleFor(it.kind), it.start, it.end) }
    if (emphasis != null && !emphasis.isEmpty()) {
        addStyle(SpanStyle(background = CodeColors.filledBlank, color = Color.White, fontWeight = FontWeight.Bold), emphasis.first, emphasis.last + 1)
    }
}

private fun lineRanges(code: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var start = 0
    while (true) {
        val nl = code.indexOf('\n', start)
        if (nl == -1) {
            ranges += start until code.length
            return ranges
        }
        ranges += start until nl
        start = nl + 1
    }
}

/** An IDE-style code card: line numbers, syntax colours, an optional red line marker and horizontal scroll. */
@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier, highlightLine: Int? = null, emphasis: IntRange? = null) {
    val text = remember(code, emphasis) { highlight(code, emphasis) }
    val lines = remember(code) { lineRanges(code) }
    BoxWithConstraints(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CodeColors.background),
    ) {
        val cardWidth = maxWidth
        Column(
            Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = cardWidth)
                .width(IntrinsicSize.Max)
                .padding(vertical = 12.dp),
        ) {
            lines.forEachIndexed { index, range ->
                val marked = highlightLine == index + 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (marked) CodeColors.highlightLine else Color.Transparent)
                        .padding(end = 16.dp),
                ) {
                    Text(
                        "${index + 1}",
                        modifier = Modifier.width(36.dp).padding(end = 12.dp),
                        textAlign = TextAlign.End,
                        style = CodeStyle,
                        color = if (marked) CodeColors.nullOp else CodeColors.gutter,
                    )
                    Text(
                        text.subSequence(range.first, range.last + 1),
                        style = CodeStyle,
                        color = CodeColors.plain,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

val CodeStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 22.sp)

/** Renders `backtick` spans as inline code, the way the prompts are written. */
fun inlineCode(text: String, codeBackground: Color, codeColor: Color): AnnotatedString = buildAnnotatedString {
    text.split('`').forEachIndexed { i, part ->
        if (i % 2 == 1) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground, color = codeColor)) { append(part) }
        } else {
            append(part)
        }
    }
}

@Composable
fun RichText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeColor = MaterialTheme.colorScheme.primary
    Text(remember(text, codeBg, codeColor) { inlineCode(text, codeBg, codeColor) }, modifier = modifier, style = style, color = color)
}

@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tint)
    }
}

/** Streak, XP and hearts: the three numbers a learner checks first. */
@Composable
fun TopStats(streak: Int, streakLit: Boolean, xp: Int, hearts: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatChip(Icons.Filled.LocalFireDepartment, "$streak", if (streakLit) Flame else Locked)
        StatChip(Icons.Filled.Bolt, "$xp XP", KotlinPurple)
        StatChip(Icons.Filled.Favorite, "$hearts", HeartRed)
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
