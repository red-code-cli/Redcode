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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.TextPrimary

@Composable
fun AttachMenuPopup(
    onCameraClick: () -> Unit,
    onPhotosClick: () -> Unit,
    onFilesClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .background(SurfaceCard, RoundedCornerShape(18.dp))
            .padding(vertical = 8.dp)
    ) {
        AttachItem(
            icon = Icons.Outlined.CameraAlt,
            title = "Camera",
            onClick = onCameraClick
        )
        AttachItem(
            icon = Icons.Outlined.Image,
            title = "Photos",
            onClick = onPhotosClick
        )
        AttachItem(
            icon = Icons.Outlined.Folder,
            title = "Files",
            onClick = onFilesClick
        )
        AttachItem(
            icon = Icons.Outlined.Navigation,
            title = "Location",
            onClick = onLocationClick
        )
    }
}

@Composable
private fun AttachItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp
        )
    }
}
