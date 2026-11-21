package com.redcode.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.ToolInvocation
import com.redcode.app.ui.theme.SoftRed
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.SurfaceCode
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@Composable
fun ToolInvocationsGroup(
    tools: List<ToolInvocation>,
    onOpenFile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (tools.isEmpty()) return

    var isGroupExpanded by remember { mutableStateOf(false) }
    val failedCount = tools.count { it.isFailed }

    Column(modifier = modifier.fillMaxWidth()) {
        // Summary row: "Used N tools >"
        val chevronRotation by animateFloatAsState(
            targetValue = if (isGroupExpanded) 90f else 0f,
            label = "chevron_rot"
        )

        Row(
            modifier = Modifier
                .clickable { isGroupExpanded = !isGroupExpanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText = if (tools.size == 1) "Used 1 tool" else "Used ${tools.size} tools"
            Text(
                text = countText,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (failedCount > 0) {
                Text(
                    text = " · $failedCount failed",
                    color = SoftRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }

        // Expanded list of tools
        AnimatedVisibility(visible = isGroupExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tools.forEach { tool ->
                    ToolCardItem(tool = tool, onOpenFile = onOpenFile)
                }
            }
        }
    }
}

@Composable
private fun ToolCardItem(
    tool: ToolInvocation,
    onOpenFile: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf<Boolean>(tool.isExpanded) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tool.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))

            // Code container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCode, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (!tool.newString.isNullOrBlank()) {
                    Text(
                        text = "newString",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = tool.newString,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (!tool.oldString.isNullOrBlank()) {
                    Text(
                        text = "oldString",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = tool.oldString,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (!tool.path.isNullOrBlank()) {
                    Text(
                        text = "path   ${tool.path}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (!tool.result.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tool.result,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (!tool.targetFile.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(10.dp))
                        .clickable { onOpenFile?.invoke(tool.path ?: tool.targetFile) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open ${tool.targetFile}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
