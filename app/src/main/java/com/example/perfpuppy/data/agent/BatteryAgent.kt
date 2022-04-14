package com.example.perfpuppy.data.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback


class BatteryAgent(
    service: CollectorServiceCallback,
    lifecycle: Lifecycle,
) : Agent(service, lifecycle) {

    override val name: String
        get() = context.getString(R.string.bat_agent_name)

    override fun aboveThMessage(value: Int): String =
        context.getString(R.string.bat_above_th_message, value)

    override fun belowThMessage(value: Int): String =
        context.getString(R.string.bat_below_th_message)

    private val batteryLevelReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            intent?.let { parseBatteryLevelIntent(it) }
        }
    }

    private fun parseBatteryLevelIntent(intent: Intent) {
        val th = prefs.getInt(
            context.getString(R.string.bat_alert_pref_key),
            context.resources.getInteger(R.integer.bat_alert_default_th)
        )

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        val perc = if (scale > 0) level * 100 / scale else 0

        setData(perc.toPerfValueInv(th))
    }

    override suspend fun getData(): PerfValue {
        // Not needed, data will be reported using batteryLevelReceiver
        return 0.toPerfValue(0)
    }

    override suspend fun enable() {
        // Register receiver for battery level
        context.registerReceiver(batteryLevelReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        // Unregister receiver for battery level
        context.unregisterReceiver(batteryLevelReceiver)
    }
}
