package com.redcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.ui.theme.BrandRed
import com.redcode.app.ui.theme.SubtleDivider
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.TextPrimary

@Composable
fun SessionMenuPopup(
    onPin: () -> Unit,
    onRename: () -> Unit,
    onCopyId: () -> Unit,
    onMarkUnread: () -> Unit,
    onMute: () -> Unit,
    onCompact: () -> Unit,
    onFork: () -> Unit,
    onExport: () -> Unit,
    onMedia: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .background(SurfaceCard, RoundedCornerShape(18.dp))
            .padding(vertical = 6.dp)
    ) {
        MenuItem(icon = Icons.Outlined.PushPin, title = "Pin", onClick = onPin)
        MenuItem(icon = Icons.Outlined.Edit, title = "Rename", onClick = onRename)
        MenuItem(icon = Icons.Outlined.ContentCopy, title = "Copy Session ID", onClick = onCopyId)
        MenuItem(icon = Icons.Outlined.Mail, title = "Mark as Unread", onClick = onMarkUnread)
        MenuItem(icon = Icons.Outlined.NotificationsOff, title = "Mute", onClick = onMute)

        HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(vertical = 4.dp))

        MenuItem(icon = Icons.Outlined.Compress, title = "Compact", onClick = onCompact)
        MenuItem(icon = Icons.Outlined.CallSplit, title = "Fork", onClick = onFork)
        MenuItem(icon = Icons.Outlined.Share, title = "Export", onClick = onExport)
        MenuItem(icon = Icons.Outlined.Image, title = "Session Media", onClick = onMedia)
        MenuItem(icon = Icons.Outlined.Info, title = "Session Info", onClick = onInfo)

        HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(vertical = 4.dp))

        MenuItem(
            icon = Icons.Outlined.Delete,
            title = "Delete Session",
            tint = BrandRed,
            onClick = onDelete
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = tint,
            fontSize = 15.sp
        )
    }
}
