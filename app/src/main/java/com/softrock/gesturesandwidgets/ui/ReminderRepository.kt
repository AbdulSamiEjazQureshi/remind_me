package com.softrock.gesturesandwidgets.ui

import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun insert(entity: ReminderEntity) : Int {
       return reminderDao.insert(entity).toInt()
    }

    suspend fun updateReminderStatus(id: Int, isReminded: Boolean) {
        reminderDao.updateReminderStatus(id, isReminded)
    }

    fun getAllRemindersByStatus(isReminded: Boolean) : Flow<List<ReminderEntity>> {
        return reminderDao.getAllRemindersByStatus(isReminded)
    }
}