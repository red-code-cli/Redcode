package com.redcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.ModelItem
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.SearchBarBg
import com.redcode.app.ui.theme.SurfaceCard
import com.redcode.app.ui.theme.SurfaceSelected
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    models: List<ModelItem>,
    onSelectModel: (ModelItem) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            if (models.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Memory,
                    title = "Koi model nahi mila",
                    message = "Pehle provider add karo ya server settings check karo.",
                    actionLabel = "Settings",
                    onActionClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )
            } else {
                val filtered = if (searchQuery.isBlank()) models else models.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.provider.contains(searchQuery, ignoreCase = true)
                }

                val favorites = filtered.filter { it.isFavorite }
                val recents = filtered.filter { it.isRecent && !it.isFavorite }
                val others = filtered.filter { !it.isFavorite && !it.isRecent }

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (favorites.isNotEmpty()) {
                        item {
                            Text(
                                text = "Favorites",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                            )
                        }
                        items(favorites, key = { it.id }) { model ->
                            ModelRow(model = model, onClick = { onSelectModel(model) })
                        }
                    }

                    if (recents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                            )
                        }
                        items(recents, key = { it.id }) { model ->
                            ModelRow(model = model, onClick = { onSelectModel(model) })
                        }
                    }

                    if (others.isNotEmpty()) {
                        val grouped = others.groupBy { it.provider }
                        grouped.forEach { (provider, providerModels) ->
                            item {
                                Text(
                                    text = provider,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                )
                            }
                            items(providerModels, key = { it.id }) { model ->
                                ModelRow(model = model, onClick = { onSelectModel(model) })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Models Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SearchBarBg, RoundedCornerShape(16.dp)),
                placeholder = { Text("Search Models", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelItem,
    onClick: () -> Unit
) {
    val bg = if (model.isSelected) SurfaceSelected else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val initial = model.name.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            Text(
                text = initial,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = model.provider,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        if (model.variant.isNotBlank()) {
            Text(
                text = model.variant,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
