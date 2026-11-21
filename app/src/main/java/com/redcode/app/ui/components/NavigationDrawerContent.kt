package com.redcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.SessionSummary
import com.redcode.app.ui.theme.AccentBlue
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.SurfaceCardElevated
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@Composable
fun NavigationDrawerContent(
    sessions: List<SessionSummary>,
    selectedSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opencode",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Categories (Catch Up / Archived)
        val unreadCount = sessions.count { it.isUnread }
        val archivedCount = sessions.count { it.isArchived }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Catch Up",
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (unreadCount > 0) {
                Text(
                    text = "$unreadCount",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Archived",
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (archivedCount > 0) {
                Text(
                    text = "$archivedCount",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sessions List
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Koi session nahi",
                    message = "Naya session shuru karo aur kaam start karo.",
                    actionLabel = "New Session",
                    onActionClick = onNewSession
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        isSelected = session.id == selectedSessionId,
                        onClick = { onSelectSession(session.id) }
                    )
                }
            }
        }

        // Bottom Floating Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New Session Pill Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                    .clickable(onClick = onNewSession)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Session",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) SurfaceCardElevated else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (session.isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentBlue, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = session.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (session.timeAgo.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = session.timeAgo,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (session.project.isNotBlank()) {
            Text(
                text = session.project,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (session.snippet.isNotBlank()) {
            Text(
                text = session.snippet,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 18.sp
            )
        }
    }
}
