package com.softrock.gesturesandwidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softrock.gesturesandwidgets.ui.AppDatabase
import com.softrock.gesturesandwidgets.ui.ReminderRepository
import com.softrock.gesturesandwidgets.ui.ReminderViewModel
import com.softrock.gesturesandwidgets.ui.ReminderListScreen
import com.softrock.gesturesandwidgets.ui.ReminderScheduler
import com.softrock.gesturesandwidgets.ui.theme.GesturesAndWidgetsTheme



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val database = AppDatabase.getAppDatabase(this)
        val reminderRepository = ReminderRepository(database.reminderDao())

        val reminderScheduler = ReminderScheduler(this)

        val reminderViewModel by viewModels<ReminderViewModel>(
            factoryProducer = {
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ReminderViewModel(reminderRepository, reminderScheduler) as T
                    }
                }
            }
        )

        enableEdgeToEdge()
        setContent {
            GesturesAndWidgetsTheme {
                val state = reminderViewModel.state.collectAsStateWithLifecycle().value
                ReminderListScreen(state, reminderViewModel::onEvent)
            }
        }
    }
}

