package com.softrock.gesturesandwidgets

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softrock.gesturesandwidgets.ui.AppDatabase
import com.softrock.gesturesandwidgets.ui.NoteRecorderScreen
import com.softrock.gesturesandwidgets.ui.ReminderRepository
import com.softrock.gesturesandwidgets.ui.ReminderViewModel
import com.softrock.gesturesandwidgets.ui.ReminderViewModelFactory
import com.softrock.gesturesandwidgets.ui.ScheduleListComposable
import com.softrock.gesturesandwidgets.ui.theme.GesturesAndWidgetsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val alarmScheduler: AlarmScheduler = AlarmSchedulerImpl(this)
//        var alarmItem: AlarmItem? = null

        val database = AppDatabase.getAppDatabase(this)
        val reminderRepository = ReminderRepository(database.reminderDao())
        val reminderViewModel by viewModels<ReminderViewModel>(
            factoryProducer = {
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ReminderViewModel(reminderRepository) as T
                    }
                }
            }
        )

        enableEdgeToEdge()
        setContent {
            GesturesAndWidgetsTheme {
                ScheduleListComposable()
//                NoteRecorderScreen()
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//
//
//                    var secondText by remember {
//                        mutableStateOf("")
//                    }
//                    var messageText by remember {
//                        mutableStateOf("")
//                    }
//
//                    var hasNotificationPermission by remember {
//                        mutableStateOf(
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                                ContextCompat.checkSelfPermission(
//                                    this,
//                                    Manifest.permission.POST_NOTIFICATIONS
//                                ) == PackageManager.PERMISSION_GRANTED
//                            } else true
//                        )
//                    }
//
//                    val permissionRequest = rememberLauncherForActivityResult(
//                        contract = ActivityResultContracts.RequestPermission()
//                    ) {}
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(innerPadding)
//                            .padding(horizontal = 16.dp),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        OutlinedTextField(
//                            value = secondText, onValueChange = {
//                                secondText = it
//                            },
//                            label = {
//                                Text(text = "Delay Second")
//                            }
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        OutlinedTextField(
//                            value = messageText, onValueChange = {
//                                messageText = it
//                            },
//                            label = {
//                                Text(text = "Message")
//                            }
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.Center,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Button(onClick = {
//                                if (hasNotificationPermission) {
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                        alarmItem = AlarmItem(
//                                            alarmTime = LocalDateTime.now().plusSeconds(
//                                                secondText.toLong()
//                                            ),
//                                            message = messageText
//                                        )
//                                        alarmItem.let(alarmScheduler::schedule)
//                                    }
//                                    secondText = ""
//                                    messageText = ""
//
//                                } else {
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                                        permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
//                                    }
//                                }
//                            }) {
//                                Text(text = "Schedule")
//                            }
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Button(onClick = {
//                                alarmItem?.let(alarmScheduler::cancel)
//                            }) {
//                                Text(text = "Cancel")
//                            }
//                            Spacer(modifier = Modifier.width(8.dp))
//                        }
//                    }
//                }
            }
        }
    }
}

