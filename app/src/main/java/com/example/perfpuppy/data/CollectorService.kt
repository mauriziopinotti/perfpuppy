package com.example.perfpuppy.data

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.preference.PreferenceManager
import com.example.perfpuppy.MainActivity
import com.example.perfpuppy.R
import com.example.perfpuppy.data.agent.Agent
import com.example.perfpuppy.data.agent.BatteryAgent
import com.example.perfpuppy.data.agent.CpuAgent
import com.example.perfpuppy.data.agent.MemoryAgent
import com.example.perfpuppy.domain.AlertItem
import com.example.perfpuppy.repository.AlertsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import kotlin.concurrent.thread


/**
 * The collector service is responsible for spawning all the agents and displaying notifications
 * when a value is above/below thresholds.
 */
@AndroidEntryPoint
class CollectorService : LifecycleService(), CollectorServiceCallback {

    companion object {
        private const val THRESHOLD_CHANNEL_ID = "Threshold"
        private const val FOREGROUND_CHANNEL_ID = "Foreground"
        private const val FOREGROUND__NOTIF_ID = 666

        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (service.service.className == CollectorService::class.java.name) return true
            }
            return false
        }
    }

    @Inject
    lateinit var alertsRepository: AlertsRepository

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notifIds = mutableMapOf<Agent, Int>()
    private var agentsJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        // Create a notification to avoid being killed
        createNotificationChannels()
        startForeground(FOREGROUND__NOTIF_ID, createForegroundNotification())

        // Start all agents
        spawnAgents()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all notifications
        notificationManager.cancelAll()

        // Cancel agents job
        agentsJob?.cancel()
    }

    private fun spawnAgents() {
        // Create agents
        val agents = listOf(
            // CPU agent
            CpuAgent(this@CollectorService, lifecycle),
            // Memory agent
            MemoryAgent(this@CollectorService, lifecycle),
            // Battery agent
            BatteryAgent(this@CollectorService, lifecycle),
        )
        agents.forEachIndexed { i, agent ->
            // Group together notifications of the same agent
            notifIds[agent] = i
            // Tie agent lifecycle to collector service lifecycle
            lifecycle.addObserver(agent)
        }

        // Spawn agents on separate thread
        thread(start = true) {
            runBlocking {
                agents.forEach {
                    launch(Dispatchers.IO) { it.enable() }
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    getString(R.string.notif_foreground_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    THRESHOLD_CHANNEL_ID,
                    getString(R.string.notif_threshold_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private val contentIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
                else -> FLAG_UPDATE_CURRENT
            }
        )
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) getString(R.string.app_name) else null)
            .setContentText(getString(R.string.notif_background_message))
            .setSmallIcon(R.drawable.ic_notif_small)
            .setLargeIcon(
                BitmapFactory.decodeResource(resources, R.drawable.ic_large_notif, null)
            )
            .setWhen(0) // removes the time
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.notif_background_message))
            )
            .build()
    }

    private fun createAlertNotification(agent: Agent, message: String): Notification {
        return NotificationCompat.Builder(this, THRESHOLD_CHANNEL_ID)
            .setContentTitle(agent.name)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_tab_alerts)
            .setContentIntent(contentIntent)
            .build()
    }

    override val context: Context by lazy { applicationContext }

    override fun onDataAboveTh(agent: Agent, value: Int) {
        Timber.w("onDataAboveTh(): agent=${agent.name}, value=$value")

        // Log the alert in the repository
        val message = agent.aboveThMessage(value)
        alertsRepository.addAlert(AlertItem(message = message, aboveTh = true))

        // Create or update the notification for this agent
        notificationManager.notify(notifIds[agent]!!, createAlertNotification(agent, message))
    }

    override fun onDataBelowTh(agent: Agent, value: Int) {
        val notifBelowThEnabled = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getBoolean(
                getString(R.string.enable_below_th_pref_key),
                resources.getBoolean(R.bool.enable_below_th_default_value)
            )
        Timber.w("onDataBelowTh(): agent=${agent.name}, value=$value notifEnabled=$notifBelowThEnabled")

        if (notifBelowThEnabled) {
            // Log the alert in the repository
            val message = agent.belowThMessage(value)
            alertsRepository.addAlert(AlertItem(message = message, aboveTh = false))

            // Create or update the notification for this agent
            notificationManager.notify(notifIds[agent]!!, createAlertNotification(agent, message))
        }
    }
}

interface CollectorServiceCallback {
    val context: Context

    /**
     * Called when an agent finds a value above the threshold (i.e. in error state)
     */
    fun onDataAboveTh(agent: Agent, value: Int)

    /**
     * Called when an agent finds a value below the threshold (i.e. in normal state)
     */
    fun onDataBelowTh(agent: Agent, value: Int)
}
