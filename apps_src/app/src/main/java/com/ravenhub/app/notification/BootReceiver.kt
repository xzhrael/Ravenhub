package com.ravenhub.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ravenhub.app.data.planner.PlannerRepository
import com.ravenhub.app.security.MasterKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // ponytail: reschedule active todo reminders on system boot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (MasterKeyManager.isUnlocked()) {
                        val planner = PlannerRepository.load(context) ?: return@launch
                        planner.todos.filter { !it.isCompleted && it.dueDateTime != null }.forEach { todo ->
                            TodoScheduler.schedule(context, todo)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
