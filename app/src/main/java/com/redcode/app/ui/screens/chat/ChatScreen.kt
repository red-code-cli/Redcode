package com.redcode.app.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcode.app.data.model.MessageItem
import com.redcode.app.data.model.MessageRole
import com.redcode.app.ui.components.AttachMenuPopup
import com.redcode.app.ui.components.EmptyStateView
import com.redcode.app.ui.components.FixedComposer
import com.redcode.app.ui.components.MarkdownContent
import com.redcode.app.ui.components.MentionPopup
import com.redcode.app.ui.components.ModelPickerSheet
import com.redcode.app.ui.components.SessionMenuPopup
import com.redcode.app.ui.components.SlashCommandPopup
import com.redcode.app.ui.components.ToolInvocationsGroup
import com.redcode.app.ui.components.TopBarWithDot
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.UserBubbleBg
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    drawerState: DrawerState,
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isBusy by viewModel.isAgentBusy.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val models by viewModel.models.collectAsState()
    val slashCommands by viewModel.slashCommands.collectAsState()
    val mentionFiles by viewModel.mentionFiles.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var isMenuOpen by remember { mutableStateOf(false) }
    var isModelSheetOpen by remember { mutableStateOf(false) }
    var isAttachPopupOpen by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    val showScrollToBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) false else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible < total - 2
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopBarWithDot(
                title = session?.title ?: "Chat",
                subtitle = session?.project?.ifBlank { null },
                hasUnread = false,
                isAgentBusy = isBusy,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onMoreClick = { isMenuOpen = !isMenuOpen }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (inputText.endsWith("@") || mentionFiles.isNotEmpty()) {
                    MentionPopup(
                        items = mentionFiles,
                        onSelect = { mention ->
                            val prefix = inputText.substringBeforeLast("@")
                            viewModel.onInputTextChanged("$prefix@${mention.title} ")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else if (inputText.startsWith("/")) {
                    val query = inputText.removePrefix("/").lowercase()
                    val filteredCmds = slashCommands.filter { it.command.contains(query, ignoreCase = true) }
                    SlashCommandPopup(
                        commands = filteredCmds,
                        onSelect = { cmd ->
                            viewModel.onInputTextChanged("${cmd.command} ")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (isAttachPopupOpen) {
                    Box(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)) {
                        AttachMenuPopup(
                            onCameraClick = { isAttachPopupOpen = false },
                            onPhotosClick = { isAttachPopupOpen = false },
                            onFilesClick = { isAttachPopupOpen = false },
                            onLocationClick = { isAttachPopupOpen = false }
                        )
                    }
                }

                FixedComposer(
                    text = inputText,
                    onTextChange = viewModel::onInputTextChanged,
                    selectedMode = selectedMode,
                    onModeChange = viewModel::selectMode,
                    selectedModel = selectedModel,
                    onModelClick = { isModelSheetOpen = true },
                    onAttachClick = { isAttachPopupOpen = !isAttachPopupOpen },
                    isAttachOpen = isAttachPopupOpen,
                    isRecording = isRecording,
                    onStartRecording = { isRecording = true },
                    onStopRecording = { isRecording = false },
                    onCancelRecording = { isRecording = false },
                    onSend = { viewModel.sendMessage() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Koi message nahi",
                        message = "Neeche message likho aur agent ko kaam do."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(messages, key = { it.id }) { message ->
                        MessageRow(message = message)
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Scroll to bottom FAB
            AnimatedVisibility(
                visible = showScrollToBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = TextPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to Bottom",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 3-Dot Session Menu Popup
            if (isMenuOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                ) {
                    SessionMenuPopup(
                        onPin = {
                            viewModel.togglePin()
                            isMenuOpen = false
                        },
                        onRename = { isMenuOpen = false },
                        onCopyId = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Session ID", viewModel.sessionId))
                            isMenuOpen = false
                        },
                        onMarkUnread = {
                            viewModel.markAsUnread()
                            isMenuOpen = false
                            onBackToHome()
                        },
                        onMute = { isMenuOpen = false },
                        onCompact = { isMenuOpen = false },
                        onFork = {
                            viewModel.forkSession { isMenuOpen = false }
                        },
                        onExport = { isMenuOpen = false },
                        onMedia = { isMenuOpen = false },
                        onInfo = { isMenuOpen = false },
                        onDelete = {
                            viewModel.deleteSession {
                                isMenuOpen = false
                                onBackToHome()
                            }
                        }
                    )
                }
            }
        }
    }

    if (isModelSheetOpen) {
        ModelPickerSheet(
            models = models,
            onSelectModel = { model ->
                viewModel.selectModel(model)
                isModelSheetOpen = false
            },
            onDismiss = { isModelSheetOpen = false },
            onOpenSettings = {
                isModelSheetOpen = false
                onNavigateToSettings()
            }
        )
    }
}

@Composable
private fun MessageRow(message: MessageItem) {
    if (message.role == MessageRole.USER) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .background(UserBubbleBg, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
        }
    } else {
        // Assistant Message
        Column(modifier = Modifier.fillMaxWidth()) {
            if (message.tools.isNotEmpty()) {
                ToolInvocationsGroup(
                    tools = message.tools,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (message.content.isNotBlank()) {
                MarkdownContent(content = message.content)
            }
        }
    }
}
