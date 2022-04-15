package com.example.perfpuppy.data.agent

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.example.perfpuppy.BuildConfig
import com.example.perfpuppy.data.CollectorServiceCallback
import kotlinx.coroutines.delay
import timber.log.Timber
import java.lang.reflect.Modifier.PROTECTED

/**
 * A generic data collection agent that runs an infinite loop and collects data every X seconds.
 *
 * Actual agents should subclass this and implement getData() plus a few more self-explaining methods.
 * If an agent wants to implement a different behavior than infinite-loop it can also override enable()
 * and prevent the loop by being created by not calling super(). In this case values should be reported
 * calling setData() directly.
 */
abstract class Agent(
    protected val service: CollectorServiceCallback,
//    private val lifecycle: Lifecycle,
) : DefaultLifecycleObserver {

    /**
     * An object holding two values: the data value collected and a boolean saying if it's above or below the threshold.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    data class PerfValue(val value: Int, val valueIsAboveTh: Boolean)

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
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getData(): PerfValue

    protected val context: Context = service.context

    protected val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(service.context)
    }

    private var enabled = false
    private var currentlyAboveTh = false

    /**
     * Enable agent and start collecting data.
     * The default behavior is to create an infinite loop and call getData() every X seconds.
     * This method can be overridden by subclasses to implement custom behavior.
     */
    open suspend fun enable() {
        Timber.d("Agent lifecycle: enable=$enabled $name")

//        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        collectDataLoop()
//        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Timber.d("Agent lifecycle: onCreate $name")

        enabled = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Timber.d("Agent lifecycle: onDestroy $name")

        enabled = false
    }

    private suspend fun collectDataLoop() {
        Timber.i("Starting loop for agent $name on thread ${Thread.currentThread().name}")

        currentlyAboveTh = false
        while (enabled) {
            // Get data
            setData(getData())

            // Sleep until next cycle
            // TODO: configurable interval?
            Timber.d("Agent $name waiting for next cycle...")
            delay(if (BuildConfig.DEBUG) 5000 else 60000)
        }

        Timber.i("Ending loop for agent $name")
    }

    protected fun setData(data: PerfValue) {
        // Ensure it's in range
        if (data.value < 0 || data.value > 100) {
            Timber.w("Invalid data from agent $name: ${data.value}")
        } else {
            Timber.i("$name returned value=${data.value}, isAbove=${data.valueIsAboveTh}/$currentlyAboveTh")

            // If above or below th then report back to CollectorService
            if (!currentlyAboveTh && data.valueIsAboveTh) {
                currentlyAboveTh = true
                service.onDataAboveTh(this@Agent, data.value)
            } else if (currentlyAboveTh && !data.valueIsAboveTh) {
                currentlyAboveTh = false
                service.onDataBelowTh(this@Agent, data.value)
            }
        }
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
