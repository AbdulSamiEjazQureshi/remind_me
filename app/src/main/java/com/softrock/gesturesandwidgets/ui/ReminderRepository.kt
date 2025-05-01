package com.softrock.gesturesandwidgets.ui

import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun insert(entity: ReminderEntity) {
        reminderDao.insert(entity)
    }
}