package com.redcode.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import com.redcode.app.data.model.ProjectFolder
import com.redcode.app.ui.components.AttachMenuPopup
import com.redcode.app.ui.components.EmptyStateView
import com.redcode.app.ui.components.FixedComposer
import com.redcode.app.ui.components.MentionPopup
import com.redcode.app.ui.components.ModelPickerSheet
import com.redcode.app.ui.components.ShimmerSkeletonRow
import com.redcode.app.ui.components.SlashCommandPopup
import com.redcode.app.ui.components.TopBarWithDot
import com.redcode.app.ui.components.VoiceInputManager
import com.redcode.app.ui.theme.DarkBackground
import com.redcode.app.ui.theme.SurfaceSelected
import com.redcode.app.ui.theme.TextPrimary
import com.redcode.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    drawerState: DrawerState,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val isLoadingProjects by viewModel.isLoadingProjects.collectAsState()
    val selectedProjectName by viewModel.selectedProjectName.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val hasUnread by viewModel.hasUnreadSessions.collectAsState()
    val models by viewModel.models.collectAsState()
    val slashCommands by viewModel.slashCommands.collectAsState()
    val mentionFiles by viewModel.mentionFiles.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isModelSheetOpen by remember { mutableStateOf(false) }
    var isAttachPopupOpen by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopBarWithDot(
                title = "New Session",
                subtitle = selectedProjectName.ifBlank { null },
                hasUnread = hasUnread,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Autocomplete popups attached directly above composer
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
                    onSend = {
                        viewModel.startSession { sessionId ->
                            onNavigateToChat(sessionId)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoadingProjects) {
                repeat(4) {
                    ShimmerSkeletonRow()
                }
            } else if (projects.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Folder,
                    title = "Koi project nahi mila",
                    message = "Server par project add karo ya check karo ki server chal raha hai.",
                    actionLabel = "Retry",
                    onActionClick = { viewModel.loadData() }
                )
            } else {
                Text(
                    text = "SELECT PROJECT",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects, key = { it.id }) { project ->
                        ProjectRow(
                            project = project,
                            onClick = { viewModel.selectProject(project) }
                        )
                    }
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
private fun ProjectRow(
    project: ProjectFolder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (project.isSelected) SurfaceSelected else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (project.isSelected) Icons.Default.Folder else Icons.Outlined.Folder,
            contentDescription = null,
            tint = if (project.isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = project.name,
                color = TextPrimary,
                fontSize = 15.sp
            )
            Text(
                text = project.path,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
