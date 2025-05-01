package com.softrock.gesturesandwidgets.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask

enum class RecordingState { IDLE, RECORDING, ERROR }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteRecorderScreen() {
    var recodingState by remember { mutableStateOf(RecordingState.IDLE) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var elapsedTime by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val timer = remember { Timer() }

    LaunchedEffect(recodingState) {
        when (recodingState) {
            RecordingState.RECORDING -> {
                Log.d("REMIND_ME", "inside timer updating time")
//                scope.launch {
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        elapsedTime += 1000
                    }
                }, 0, 1000)
//                }
            }

            else -> {}
        }

    }

    var hasRecordingPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        hasRecordingPermission = result

        Log.d("REMIND_ME", "Permission Status: $hasRecordingPermission")
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Note") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (recodingState) {
                RecordingState.IDLE -> {
                    Button(
                        onClick = {
                            Log.d("REMIND_ME", "Checking Audio Recording Permission")
                            if (!hasRecordingPermission) {
                                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            Log.d("REMIND_ME", "Has Permission")


                            startRecording(
                                context,
                                onSuccess = { recorder, file ->
                                    mediaRecorder = recorder
                                    outputFile = file
                                    recodingState = RecordingState.RECORDING
                                    Log.d("REMIND_ME", "Recording successfully")
                                },
                                onError = {
                                    timer.cancel()
                                    outputFile = null
                                    mediaRecorder = null
                                    recodingState = RecordingState.ERROR
                                    Log.d("REMIND_ME", "Recording error")
                                }
                            )
                        }
                    ) {
                        Text("Start Recording")

                    }


                    outputFile?.let {
                        AudioPlayer(it)
                    }
                }

                RecordingState.RECORDING -> {
                    Text("Recording: ${elapsedTime / 1000}s")
                    Button(
                        onClick = {
                            Log.d("REMIND_ME", "Stopping Recorder")

                            stopRecoding(mediaRecorder) {
                                timer.cancel()
                                mediaRecorder = null
                                recodingState = RecordingState.IDLE
                                Log.d("REMIND_ME", "Recorder Stopped")
                            }
                        }
                    ) {
                        Text("Stop Recording")
                    }
                }

                RecordingState.ERROR -> {
                    Text("Recoding Failed")
                    Button(onClick = {
                        recodingState = RecordingState.IDLE
                    }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            Log.d("REMIND_ME", "Disposed the recorder and timer")
            mediaRecorder?.release()
            timer.cancel()
        }
    }
}

private fun startRecording(
    context: Context,
    onSuccess: (MediaRecorder, File) -> Unit,
    onError: () -> Unit
) {

    val outputFile = File(context.cacheDir, "remind_me_recording_${System.currentTimeMillis()}.mp3")

    Log.d("REMIND_ME", "Started Recording on path: ${outputFile.path}")

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
                Log.d("REMIND_ME", "Recorder Started")
                onSuccess(this, outputFile)
            }
        }
    } catch (e: Exception) {
        outputFile.delete()
        onError()
    }
}

private fun stopRecoding(
    recorder: MediaRecorder?,
    onStopped: () -> Unit
) {
    try {

        recorder?.apply {
            stop()
            release()
        }

        onStopped()
    } catch (e: Exception) {
        onStopped()
    }
}

@Composable
fun AudioPlayer(
    audioFile: File,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(audioFile) {
        mediaPlayer.apply {
            setDataSource(audioFile.absolutePath)
            prepareAsync()
        }

        onDispose {
            mediaPlayer.release()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (isPlaying) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.start()
                }

                isPlaying = !isPlaying
            }
        ) {
            Text(if (isPlaying) "Pause" else "Play Recording")


        }

        LinearProgressIndicator(
            progress = {
                if (mediaPlayer.duration > 0) {
                    mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration.toFloat()
                } else 0f
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}