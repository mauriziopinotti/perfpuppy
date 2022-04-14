package com.example.perfpuppy.data.agent

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.example.perfpuppy.BuildConfig
import com.example.perfpuppy.data.CollectorServiceCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.concurrent.thread

abstract class Agent(
    private val service: CollectorServiceCallback,
    private val lifecycle: Lifecycle,
) : DefaultLifecycleObserver {

    protected data class PerfValue(val value: Int, val valueIsAboveTh: Boolean)

    /**
     * The name of this agent, it's used in the logs and also shown to the user. Must be short and yet expressive.
     */
    abstract val name: String

    /**
     * The message to show to the user when the value is above the notification threshold.
     */
    abstract fun aboveThMessage(value: Int): String

    /**
     * The message to show to the user when the value is back to normal (i.e. below the notification threshold).
     */
    abstract fun belowThMessage(value: Int): String

    /**
     * Returns the data for this agent, expressed as percentage in the range [0..100].
     */
    protected abstract suspend fun getData(): PerfValue

    protected val context: Context = service.context

    protected val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(service.context)
    }

    private var enabled = false

    override fun onStart(owner: LifecycleOwner) {
//        if (enabled) {
//            thread(start = true) {
//                runBlocking {
//                    launch(Dispatchers.IO) {
//                        collectDataLoop()
//                    }
//                }
//            }
//        }
    }

    suspend fun enable() {
        if (!enabled && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            enabled = true
//            runBlocking {
                collectDataLoop()
//            }
        }
    }

//    fun disable() {
//        enabled = false
//    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

//    override fun onStop(owner: LifecycleOwner) {
//        disable()
//    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.i("Agent lifecycle: onDestroy")
        super.onDestroy(owner)
//        disable()
        enabled = false
    }

    private suspend fun collectDataLoop() {
        Timber.i("Starting loop for agent $name on thread ${Thread.currentThread().name}")

        var currentlyAboveTh = false
        while (enabled) {
            // Get data
            with(getData()) {
                // Ensure it's in range
                if (value < 0 || value > 100) {
                    Timber.w("Invalid data from agent $name: $value")
                } else {
                    Timber.i("$name returned value=$value, isAbove=$valueIsAboveTh/$currentlyAboveTh")

                    // If above or below th then report back to CollectorService
                    if (!currentlyAboveTh && valueIsAboveTh) {
                        currentlyAboveTh = true
                        service.onDataAboveTh(this@Agent, value)
                    } else if (currentlyAboveTh && !valueIsAboveTh) {
                        currentlyAboveTh = false
                        service.onDataBelowTh(this@Agent, value)
                    }
                }
            }

            // Sleep until next cycle
            // TODO: configurable interval?
            Timber.d("Agent $name waiting for next cycle...")
            delay(if (BuildConfig.DEBUG) 5000 else 60000)
        }

        Timber.i("Ending loop for agent $name")
    }

    /**
     * Converts Int to PerfValue for values that are normally below th (i.e. cpu).
     */
    protected fun Int.toPerfValue(th: Int) = PerfValue(this, this >= th)

    /**
     * Converts Int to PerfValue for values that are normally above th (i.e. battery).
     */
    protected fun Int.toPerfValueInv(th: Int) = PerfValue(this, this < th)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Agent

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "Agent(name='$name', enabled=$enabled)"
    }
}
