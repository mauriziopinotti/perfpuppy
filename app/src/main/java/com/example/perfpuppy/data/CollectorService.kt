package com.example.perfpuppy.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.perfpuppy.MainActivity
import com.example.perfpuppy.R
import com.example.perfpuppy.data.agent.Agent
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
        private const val FOREGROUND_CHANNEL_ID = "Foreground"
        private const val FOREGROUND__NOTIF_ID = 666
        private const val THRESHOLD_CHANNEL_ID = "Threshold"
    }

    @Inject
    lateinit var alertsRepository: AlertsRepository

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var cpuAgent: Agent? = null
    private var cpuAgentJob: Job? = null
    private var memAgent: Agent? = null
    private var memAgentJob: Job? = null
    private var batAgent: Agent? = null
    private var batAgentJob: Job? = null

    private val notifIds = mutableMapOf<Agent, Int>()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CollectorService = this@CollectorService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Create a notification to avoid being killed
        createNotificationChannels()
        startForeground(FOREGROUND__NOTIF_ID, createForegroundNotification())

        // Start all agents
        startAgents()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all notifications
        notificationManager.cancelAll()

        // Stop all agents
        stopAgents()
    }

    private fun startAgents() {
        // Spawn agents
        thread(start = true) {
            runBlocking {
                var notifId = 1;

                // CPU agent
                cpuAgent = CpuAgent(this@CollectorService, lifecycle);
                notifIds[cpuAgent!!] = notifId++
                cpuAgentJob = launch(Dispatchers.IO) {
                    cpuAgent!!.enable()
                }

                // Memory agent
                memAgent = MemoryAgent(this@CollectorService, lifecycle);
                notifIds[memAgent!!] = notifId++
                memAgentJob = launch(Dispatchers.IO) {
                    memAgent!!.enable()
                }

                // Battery agent
//                batAgent = BatteryAgent(this@CollectorService, lifecycle);
//                notifIds[batAgent!!] = notifId++
//                batAgentJob = launch(Dispatchers.IO) {
//                    batAgent!!.enable()
//                }
            }
        }


//        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
//        println("BATTERY_PROPERTY_CURRENT_NOW: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
//        println("BATTERY_PROPERTY_STATUS: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_STATUS))
//        println("BATTERY_PROPERTY_CHARGE_COUNTER: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
//        println("BATTERY_PROPERTY_CURRENT_AVERAGE: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
    }

    private fun stopAgents() {
        cpuAgent?.disable()
        cpuAgentJob?.cancel()

        memAgent?.disable()
        memAgentJob?.cancel()

        batAgent?.disable()
        batAgentJob?.cancel()
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

    override suspend fun onDataAboveTh(agent: Agent, value: Int) {
        Timber.w("onDataAboveTh(): agent=${agent.name}, value=$value")

        // Log the alert in the repository
        val message = agent.aboveThMessage(value)
        alertsRepository.addAlert(AlertItem(message = message, aboveTh = true))

        // Create or update the notification for this agent
        notificationManager.notify(notifIds[agent]!!, createAlertNotification(agent, message))
    }

    override suspend fun onDataBelowTh(agent: Agent, value: Int) {
        Timber.w("onDataBelowTh(): agent=${agent.name}, value=$value")

        // Log the alert in the repository
        val message = agent.belowThMessage(value)
        alertsRepository.addAlert(AlertItem(message = message, aboveTh = false))

        // Create or update the notification for this agent
        notificationManager.notify(notifIds[agent]!!, createAlertNotification(agent, message))
    }
}

interface CollectorServiceCallback {
    val context: Context
    suspend fun onDataAboveTh(agent: Agent, value: Int)
    suspend fun onDataBelowTh(agent: Agent, value: Int)
}
