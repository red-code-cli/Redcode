package com.redcode.app.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.content.ContextCompat
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
import com.redcode.app.ui.components.VoiceInputManager
import com.redcode.app.ui.theme.BrandRed
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.RedcodeAccentBlue
import com.redcode.app.ui.theme.RedcodeAccentRed
import com.redcode.app.ui.theme.RedcodeCardBackground
import com.redcode.app.ui.theme.RedcodeTextPrimary
import com.redcode.app.ui.theme.RedcodeTextSecondary
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.UserBubbleBg
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    drawerState: DrawerState,
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSession: ((String) -> Unit)? = null
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
    val isMuted by viewModel.isSessionMuted.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var isMenuOpen by remember { mutableStateOf(false) }
    var isModelSheetOpen by remember { mutableStateOf(false) }
    var isAttachPopupOpen by remember { mutableStateOf(false) }

    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitleInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMediaDialog by remember { mutableStateOf(false) }

    // Voice recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingAmplitude by remember { mutableFloatStateOf(0f) }
    var textBeforeRecording by remember { mutableStateOf("") }

    val voiceManager = remember { VoiceInputManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            textBeforeRecording = inputText
            isRecording = true
            voiceManager.startListening(
                onPartialResult = { partial ->
                    val base = if (textBeforeRecording.isBlank()) "" else "$textBeforeRecording "
                    viewModel.onInputTextChanged("$base$partial")
                },
                onFinalResult = { final ->
                    val base = if (textBeforeRecording.isBlank()) "" else "$textBeforeRecording "
                    viewModel.onInputTextChanged("$base$final")
                    isRecording = false
                    recordingAmplitude = 0f
                },
                onRmsChanged = { rms ->
                    recordingAmplitude = rms
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    isRecording = false
                    recordingAmplitude = 0f
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceInput() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            textBeforeRecording = inputText
            isRecording = true
            voiceManager.startListening(
                onPartialResult = { partial ->
                    val base = if (textBeforeRecording.isBlank()) "" else "$textBeforeRecording "
                    viewModel.onInputTextChanged("$base$partial")
                },
                onFinalResult = { final ->
                    val base = if (textBeforeRecording.isBlank()) "" else "$textBeforeRecording "
                    viewModel.onInputTextChanged("$base$final")
                    isRecording = false
                    recordingAmplitude = 0f
                },
                onRmsChanged = { rms ->
                    recordingAmplitude = rms
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    isRecording = false
                    recordingAmplitude = 0f
                }
            )
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
        isRecording = false
        recordingAmplitude = 0f
    }

    fun cancelVoiceInput() {
        voiceManager.cancel()
        viewModel.onInputTextChanged(textBeforeRecording)
        isRecording = false
        recordingAmplitude = 0f
    }

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
                    recordingAmplitude = recordingAmplitude,
                    onStartRecording = { startVoiceInput() },
                    onStopRecording = { stopVoiceInput() },
                    onCancelRecording = { cancelVoiceInput() },
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
                            val pinned = session?.isPinned == true
                            Toast.makeText(context, if (!pinned) "Session pinned" else "Session unpinned", Toast.LENGTH_SHORT).show()
                        },
                        onRename = {
                            renameTitleInput = session?.title.orEmpty()
                            showRenameDialog = true
                            isMenuOpen = false
                        },
                        onCopyId = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Session ID", viewModel.sessionId))
                            Toast.makeText(context, "Session ID copied to clipboard", Toast.LENGTH_SHORT).show()
                            isMenuOpen = false
                        },
                        onMarkUnread = {
                            viewModel.markAsUnread {
                                Toast.makeText(context, "Marked as unread", Toast.LENGTH_SHORT).show()
                                isMenuOpen = false
                                onBackToHome()
                            }
                        },
                        onMute = {
                            viewModel.toggleMute { muted ->
                                Toast.makeText(context, if (muted) "Session notifications muted" else "Session unmuted", Toast.LENGTH_SHORT).show()
                                isMenuOpen = false
                            }
                        },
                        onCompact = {
                            isMenuOpen = false
                            Toast.makeText(context, "Compacting session...", Toast.LENGTH_SHORT).show()
                            viewModel.compactSession { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFork = {
                            isMenuOpen = false
                            Toast.makeText(context, "Forking session...", Toast.LENGTH_SHORT).show()
                            viewModel.forkSession { newId ->
                                if (newId != null) {
                                    Toast.makeText(context, "Forked to new session", Toast.LENGTH_SHORT).show()
                                    onNavigateToSession?.invoke(newId) ?: onBackToHome()
                                } else {
                                    Toast.makeText(context, "Failed to fork session", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onExport = {
                            isMenuOpen = false
                            Toast.makeText(context, "Exporting session...", Toast.LENGTH_SHORT).show()
                            viewModel.exportSession { json ->
                                if (json != null) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        putExtra(Intent.EXTRA_SUBJECT, "Exported Session: ${session?.title}")
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Session")
                                    context.startActivity(shareIntent)
                                } else {
                                    Toast.makeText(context, "Failed to export session", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onMedia = {
                            isMenuOpen = false
                            showMediaDialog = true
                        },
                        onInfo = {
                            isMenuOpen = false
                            showInfoDialog = true
                        },
                        onDelete = {
                            isMenuOpen = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Session", color = RedcodeTextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = RedcodeTextPrimary,
                        unfocusedTextColor = RedcodeTextPrimary,
                        focusedBorderColor = RedcodeAccentBlue,
                        unfocusedBorderColor = RedcodeTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = renameTitleInput.trim()
                        if (newTitle.isNotBlank()) {
                            viewModel.renameSession(newTitle) {
                                showRenameDialog = false
                            }
                        }
                    }
                ) {
                    Text("Rename", color = RedcodeAccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = RedcodeTextSecondary)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session?", color = RedcodeTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this session? This action cannot be undone.",
                    color = RedcodeTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSession {
                            Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                            onBackToHome()
                        }
                    }
                ) {
                    Text("Delete", color = RedcodeAccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = RedcodeTextSecondary)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    // Session Info Dialog
    if (showInfoDialog) {
        val s = session
        val dateStr = s?.updatedAt?.let {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Session Info", color = RedcodeTextPrimary) },
            text = {
                Column {
                    Text("Title: ${s?.title ?: "Untitled"}", color = RedcodeTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ID: ${viewModel.sessionId}", color = RedcodeTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Project: ${s?.project?.ifBlank { "Default / None" } ?: "Default / None"}", color = RedcodeTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Messages: ${messages.size}", color = RedcodeTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Updated: $dateStr", color = RedcodeTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Muted: ${if (isMuted) "Yes" else "No"}", color = RedcodeTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pinned: ${if (s?.isPinned == true) "Yes" else "No"}", color = RedcodeTextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", color = RedcodeAccentBlue)
                }
            },
            containerColor = RedcodeCardBackground
        )
    }

    // Session Media Dialog
    if (showMediaDialog) {
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            title = { Text("Session Media", color = RedcodeTextPrimary) },
            text = {
                Text(
                    "No media, attachments, or image files attached in this session.",
                    color = RedcodeTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showMediaDialog = false }) {
                    Text("Close", color = RedcodeAccentBlue)
                }
            },
            containerColor = RedcodeCardBackground
        )
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
