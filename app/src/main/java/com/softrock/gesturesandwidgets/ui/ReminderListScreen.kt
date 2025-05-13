@file:OptIn(ExperimentalTime::class)

package com.softrock.gesturesandwidgets.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.softrock.gesturesandwidgets.ui.ReminderVMEvent.SetAudioUri
import com.softrock.gesturesandwidgets.ui.ReminderVMEvent.SetRecordingState
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    state: ReminderViewModelState,
    onEvent: (ReminderVMEvent) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioRecorderService = remember { AudioRecorderService(context) }
    val audioPlayerService = remember { AudioPlayerService() }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    var showPreviousReminders by remember { mutableStateOf(false) }

    var groupedReminders = state.upcomingReminders.groupBy {
        convertMillisToDate(it.timeInMillis)
    }
    var groupedPreviousReminders = state.previousReminders.groupBy {
        convertMillisToDate(it.timeInMillis)
    }

    val focusRequester = remember { FocusRequester() }
    var scrollState = rememberLazyListState()


    val calendar = remember { Calendar.getInstance() }

    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
    )

    var timerPickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    var modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )


    val selectedDate =
        convertMillisToDate(if (state.timeInMillis == 0L) calendar.timeInMillis else state.timeInMillis)
    val selectedTime =
        convertMillisToTime(if (state.timeInMillis == 0L) calendar.timeInMillis else state.timeInMillis)

    LaunchedEffect(
        datePickerState.selectedDateMillis,
        timerPickerState.hour,
        timerPickerState.minute
    ) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, timerPickerState.hour)
        cal.set(Calendar.MINUTE, timerPickerState.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        onEvent(ReminderVMEvent.SetTimeInMillis(cal.timeInMillis))
    }


    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionRequester = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        hasNotificationPermission = result
        if (hasNotificationPermission) {
            onEvent(ReminderVMEvent.SaveReminder)
        }
    }


    var hasRecordingPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var audioRecorderPermissionRequester = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        hasRecordingPermission = result
        if (hasRecordingPermission) {
            audioRecorderService.startRecording(
                onStart = {
                    onEvent(SetRecordingState(RecordingState.RECORDING))
                },
                onError = {
                    onEvent(SetRecordingState(RecordingState.ERROR))
                }
            )
        }
    }

    var elapsedTime by remember { mutableLongStateOf(0L) }
    var timer by remember { mutableStateOf<Timer?>(null) }

    LaunchedEffect(state.recordingState) {
        when (state.recordingState) {
            RecordingState.IDLE -> {
                elapsedTime = 0L
            }

            RecordingState.RECORDING -> {
                timer?.cancel()
                timer = Timer()
                scope.launch {
                    timer?.schedule(object : TimerTask() {
                        override fun run() {
                            elapsedTime += 1000
                        }
                    }, 0, 1000L)

                }
            }

            RecordingState.RECORDED -> {
                timer?.cancel()
            }

            RecordingState.ERROR -> {
                timer?.cancel()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text("Remind me")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onEvent(ReminderVMEvent.SetIsAddingTitle(true))
                    onEvent(ReminderVMEvent.SetIsAddingReminder(false))
                }
            ) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Text(
                    "Previous Reminders",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().clickable {
                        showPreviousReminders = !showPreviousReminders
                    },
                    textAlign = TextAlign.Center
                )
            }

            if (showPreviousReminders) {
                groupedPreviousReminders.forEach { (date, reminders) ->
                    item {
                        Text(
                            date,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }

                    items(reminders) { reminder ->
                        ScheduleListItemComposable(
                            reminder,
                            onPlay = {
                                selectedFileName = reminder.audioUri
                                var audioPath = File(context.cacheDir, reminder.audioUri)
                                if (audioPath.exists()) {
                                    audioPlayerService.startPlaying(
                                        filePath = audioPath.absolutePath,
                                        onStart = {
                                            isPlayingAudio = true
                                        },
                                        onError = {
                                            isPlayingAudio = false
                                            selectedFileName = null
                                        })
                                }
                            },
                            onStop = {
                                audioPlayerService.stopPlaying {
                                    isPlayingAudio = false
                                    selectedFileName = null
                                }
                            },
                            isPlaying = selectedFileName == reminder.audioUri,
                            currentPosition = if (selectedFileName == reminder.audioUri) audioPlayerService.getCurrentPosition() else 0f,
                            onSeek = { newPosition ->
                                audioPlayerService.seekTo(newPosition.toInt())
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    "Upcoming Reminders",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            groupedReminders.forEach { (date, reminders) ->
                item {
                    Text(
                        date,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }

                items(reminders) { reminder ->
                    ScheduleListItemComposable(
                        reminder,
                        onPlay = {
                            selectedFileName = reminder.audioUri
                            var audioPath = File(context.cacheDir, reminder.audioUri)
                            if (audioPath.exists()) {
                                audioPlayerService.startPlaying(
                                    filePath = audioPath.absolutePath,
                                    onStart = {
                                        isPlayingAudio = true
                                    },
                                    onError = {
                                        isPlayingAudio = false
                                        selectedFileName = null
                                    })
                            }
                        },
                        onStop = {
                            audioPlayerService.stopPlaying {
                                isPlayingAudio = false
                                selectedFileName = null
                            }
                        },
                        isPlaying = selectedFileName == reminder.audioUri,
                        currentPosition = if (selectedFileName == reminder.audioUri) audioPlayerService.getCurrentPosition() else 0f,
                        onSeek = { newPosition ->
                            audioPlayerService.seekTo(newPosition.toInt())
                        }
                    )
                }
            }
        }

        if (state.isAddingTitle) {

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            ModalBottomSheet(
                onDismissRequest = {
                    onEvent(ReminderVMEvent.SetIsAddingTitle(false))
                    onEvent(ReminderVMEvent.ResetFormValues)
                },
                dragHandle = null,
                modifier = Modifier.wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .wrapContentWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onEvent(ReminderVMEvent.ResetFormValues)
                            }
                        ) { Text("Cancel", color = MaterialTheme.colorScheme.primary) }
                        Text(
                            "Enter Note",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W700,
                        )
                        IconButton(
                            enabled = state.title.isNotEmpty() && state.title.isNotBlank(),
                            onClick = {
                                onEvent(ReminderVMEvent.SetIsAddingTitle(false))
                                onEvent(ReminderVMEvent.SetIsAddingReminder(true))
                            }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Date Icon"
                            )
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(16.dp),
                        value = state.title,
                        onValueChange = { onEvent(ReminderVMEvent.SetTitle(it)) },
                        label = { Text("Title") })
                }
            }
        }

        if (state.isAddingReminder) {
            ModalBottomSheet(
                sheetState = modalBottomSheetState,
                onDismissRequest = {
                    onEvent(ReminderVMEvent.SetIsAddingTitle(true))
                    onEvent(ReminderVMEvent.SetIsAddingReminder(false))
                },
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = { WindowInsets.safeDrawing },
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onEvent(ReminderVMEvent.SetIsAddingTitle(true))
                                onEvent(ReminderVMEvent.SetIsAddingReminder(false))
                            }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Schedule reminder", fontSize = 16.sp, fontWeight = FontWeight.W700)
                        TextButton(
                            enabled = state.timeInMillis != 0L && state.audioUri != null,
                            onClick = {
                            if (hasNotificationPermission) {
                                onEvent(ReminderVMEvent.SaveReminder)
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionRequester.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }) {
                            Text(
                                "Save",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 0.dp, vertical = 15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onEvent(ReminderVMEvent.SetIsSelectedDate(true))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isSelectedDate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (state.isSelectedDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                ) {
                                    Text(selectedDate)
                                }
                                Spacer(Modifier.width(5.dp))
                                Button(
                                    onClick = {
                                        onEvent(ReminderVMEvent.SetIsSelectedDate(false))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isSelectedDate == false) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (state.isSelectedDate == false) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                ) {
                                    Text(selectedTime)
                                }
                            }
                            if (state.isSelectedDate) {
                                DatePicker(
                                    state = datePickerState,
                                    title = null,
                                    showModeToggle = false,
                                    headline = null,
                                    colors = DatePickerDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                )
                            } else {
                                TimePicker(
                                    state = timerPickerState
                                )
                            }
                            state.error?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (state.recordingState) {
                            RecordingState.IDLE -> {
                                AppIconButton(onClick = {
                                    if (hasRecordingPermission) {
                                        audioRecorderService.startRecording(
                                            onStart = {
                                                onEvent(SetRecordingState(RecordingState.RECORDING))
                                            },
                                            onError = {
                                                onEvent(SetRecordingState(RecordingState.ERROR))
                                            }
                                        )
                                    } else {
                                        audioRecorderPermissionRequester.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Record Audio"
                                    )
                                }
                            }

                            RecordingState.RECORDING -> {
                                Text("Recording ${elapsedTime / 1000}s...")
                                Spacer(Modifier.height(10.dp))
                                AppIconButton(onClick = {
                                    audioRecorderService.stopRecording { fileName ->
                                        if (fileName != null) {
                                            onEvent(SetAudioUri(fileName))
                                            onEvent(SetRecordingState(RecordingState.RECORDED))
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop Recording")
                                }
                            }

                            RecordingState.ERROR -> {
                                AppIconButton(onClick = {
                                    onEvent(SetRecordingState(RecordingState.IDLE))
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                }
                            }

                            RecordingState.RECORDED -> {
                                audioRecorderService.getRecordedFile()?.let { file ->
                                    Row {
                                        AppIconButton(onClick = {
                                            if (isPlayingAudio == false) {
                                                audioPlayerService.startPlaying(
                                                    filePath = file.absolutePath,
                                                    onStart = {
                                                        isPlayingAudio = true
                                                    },
                                                    onError = {
                                                        isPlayingAudio = false
                                                    })
                                            }
                                        }) {
                                            Icon(
                                                if (isPlayingAudio) Icons.Default.Close else Icons.Default.PlayArrow,
                                                contentDescription = "Play Audio"
                                            )
                                        }
                                        Text(file.absolutePath, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Recorded About ${elapsedTime / 1000}s")
                                AppIconButton(onClick = {
                                    onEvent(SetRecordingState(RecordingState.IDLE))
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListItemComposable(
    reminder: ReminderEntity,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean = false,
    currentPosition: Float = 0f
) {

    val context = LocalContext.current
    val file = File(context.cacheDir, reminder.audioUri)
    val duration = getAudioDurationMillis(file.absolutePath)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppIconButton(onClick = {
                if (isPlaying) {
                    onStop()
                } else {
                    onPlay()
                }
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = "Play Audio"
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(convertMillisToTime(reminder.timeInMillis))
                Text(reminder.title)
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = currentPosition,
                    onValueChange = { newValue ->
                        onSeek(newValue)
                    },
                    valueRange = 0f..duration.toFloat()
                )
                Text(formatAudioTime(duration.toInt()))
            }
        }
    }
}

@Composable
fun AppIconButton(onClick: () -> Unit, content: @Composable (() -> Unit)) {
    IconButton(
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        onClick = onClick,
        content = content
    )
}

private fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault())
    return formatter.format(millis)
}

private fun convertMillisToTime(millis: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return formatter.format(millis)
}

class AudioRecorderService(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var fileName: String? = null

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    fun startRecording(onStart: () -> Unit, onError: () -> Unit) {
        fileName = "note_" + System.currentTimeMillis().toString() + ".m4a"
        outputFile = File(context.cacheDir, fileName!!)

        try {
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            onStart()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording failed: ${e.message}", e)
            outputFile?.delete()
            fileName = null
            release()
            onError()
        }
    }

    fun stopRecording(onStop: (fileName: String?) -> Unit) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Stop failed: ${e.message}", e)
        } finally {
            mediaRecorder = null
            onStop(fileName)
        }
    }

    fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Release failed: ${e.message}", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun getRecordedFile(): File? = outputFile
}

class AudioPlayerService() {
    private var mediaPlayer: MediaPlayer? = null

    fun startPlaying(filePath: String, onStart: () -> Unit, onError: () -> Unit) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                onStart()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Playing failed: ${e.message}", e)
            onError()
        }
    }

    fun stopPlaying(onStop: () -> Unit) {
        mediaPlayer?.release()
        mediaPlayer = null
        onStop()
    }

    fun seekTo(value: Int) {
        mediaPlayer?.seekTo(value)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun getCurrentPosition(): Float = mediaPlayer?.currentPosition?.toFloat() ?: 0f
    fun getTotalDuration() = mediaPlayer?.duration ?: 0f
}

fun formatAudioTime(ms: Int): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun getAudioDurationMillis(path: String) : Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        durationStr?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
       0L
    } finally {
        retriever.release()
    }
}