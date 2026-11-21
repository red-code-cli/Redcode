package com.redcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.ui.theme.AccentBlue
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val lines = content.split("\n")
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        // End code block
                        CodeBlockCard(code = codeBuffer.toString())
                        codeBuffer.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBuffer.append(line).append("\n")
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    FormattedBullet(line = line.substring(2))
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> {
                    FormattedText(line = line)
                }
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            CodeBlockCard(code = codeBuffer.toString())
        }
    }
}

@Composable
private fun FormattedBullet(line: String) {
    Box(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
        val annotated = parseInlineMarkdown("• $line")
        Text(
            text = annotated,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextPrimary
        )
    }
}

@Composable
private fun FormattedText(line: String) {
    val annotated = parseInlineMarkdown(line)
    Text(
        text = annotated,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun CodeBlockCard(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code.trimEnd(),
            color = Color(0xFFE0E0E0),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp
        )
    }
}

private fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Bold **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code `code`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF2C2C2E),
                            color = Color(0xFFE0E0E0),
                            fontSize = 13.sp
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
