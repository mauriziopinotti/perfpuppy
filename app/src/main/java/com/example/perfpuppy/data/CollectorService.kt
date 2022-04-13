package com.example.perfpuppy.data

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.preference.PreferenceManager
import com.example.perfpuppy.R
import com.example.perfpuppy.data.agent.CpuAgent
import com.example.perfpuppy.repository.AlertsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class CollectorService : LifecycleService() {

    @Inject
    lateinit var alertsRepository: AlertsRepository

    private lateinit var cpuAgent: CpuAgent

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CollectorService = this@CollectorService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun spawnAgents() {


        cpuAgent = CpuAgent(this, lifecycle) { location ->
            // update UI
        }

        runBlocking {
            val job1 = launch {
                println("${Thread.currentThread()} has run.")
                println("main runBlocking      : job1 I'm working in thread ${Thread.currentThread().name}")
            }
            val job2 = launch(Dispatchers.Unconfined) {
                println("${Thread.currentThread()} has run.")
                println("main runBlocking      : job2 I'm working in thread ${Thread.currentThread().name}")
            }
            val job3 = launch(Dispatchers.Default) {
                println("${Thread.currentThread()} has run.")
                println("main runBlocking      : job3 I'm working in thread ${Thread.currentThread().name}")
            }
            val job4 = launch(Dispatchers.Default) {
                println("${Thread.currentThread()} has run.")
                println("main runBlocking      : job4 I'm working in thread ${Thread.currentThread().name}")
            }
        }
        println("working FUORI DAL RUN BLOCKING")

//        lifecycleScope.launch(Dispatchers.Default) {
//            repeat(3) {
//                alertsRepository.addAlert(AlertItem(message = "Test message $it"))
//            }
//        }

//        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
//        println("BATTERY_PROPERTY_CURRENT_NOW: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
//        println("BATTERY_PROPERTY_STATUS: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_STATUS))
//        println("BATTERY_PROPERTY_CHARGE_COUNTER: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
//        println("BATTERY_PROPERTY_CURRENT_AVERAGE: "+bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        val name = sharedPreferences.getInt(getString(R.string.cpuAlertPrefKey), 0)

//        CpuAgent().start()
//        MemoryAgent().start()
//        BatteryAgent().start()
    }
}
