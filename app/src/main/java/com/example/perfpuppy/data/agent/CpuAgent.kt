package com.example.perfpuppy.data.agent

import android.os.Build
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.max
import kotlin.math.min

class CpuAgent(
    service: CollectorServiceCallback,
    lifecycle: Lifecycle,
) : Agent(service, lifecycle) {

    internal class NotSupportedException : RuntimeException()

    override val name: String
        get() = context.getString(R.string.cpu_agent_name)

    override fun aboveThMessage(value: Int): String =
        context.getString(R.string.cpu_above_th_message, value)

    override fun belowThMessage(value: Int): String =
        context.getString(R.string.cpu_below_th_message)

    override suspend fun getData(): PerfValue {
        // Warning: since O there is no reliable way of collecting CPU usage on Android,
        // see https://issuetracker.google.com/issues/37140047?pli=1
        // Thus, we're trying different methods to get the CPU usage, if everything fails the
        // fallback method is infering the usage from the CPU frequency (inaccurate and ugly).

        // TODO: try to investigate HardwarePropertiesManager,
        // see https://developer.android.com/reference/android/os/HardwarePropertiesManager

        val th = prefs.getInt(context.getString(R.string.cpu_alert_pref_key), 0)
        var data: Int?

        // Try /proc/stat
        data = tryGetData(this::getFromProcStat, "procStatSupported")
        if (data != null) return data.toPerfValue(th)

        // Try top
        data = tryGetData(this::getFromTop, "topSupported")
        if (data != null) return data.toPerfValue(th)

        // Try vmstat
        data = tryGetData(this::getFromVmStat, "vmstatSupported")
        if (data != null) return data.toPerfValue(th)

        // Fallback to frequency
        data = tryGetData(this::getFromFrequency)
        if (data != null) return data.toPerfValue(th)

        return 0.toPerfValue(th)
    }

    private suspend fun tryGetData(method: suspend () -> Int, prefKey: String? = null): Int? {
        try {
            // Try loading data using this method, unless we already know it's not supported
            if (prefKey == null || prefs.getBoolean(prefKey, true))
                return method.invoke()
        } catch (e: NotSupportedException) {
            // Not supported, save this in prefs so that we don't try again next time
            Timber.w("$method is not supported on this device")
            if (prefKey != null) prefs.edit().putBoolean(prefKey, false).apply()
        }
        return null
    }

    private suspend fun getFromProcStat(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val (workBefore, totalBefore) = parseProcStat()
            delay(1000)
            val (workAfter, totalAfter) = parseProcStat()

            val totalDiff = totalAfter - totalBefore
            val workDiff = workAfter - workBefore
            val perc = if (totalDiff > 0) min(max(0, workDiff * 100 / totalDiff), 100) else 0
            Timber.d("getFromProcStat(): perc=$perc")

            return perc.toInt()
        } else {
            throw NotSupportedException()
        }
    }

    private fun parseProcStat(): List<Long> {
        // CPU usage percents calculation, it is possible negative values or values higher than 100% may appear.
        // http://kernel.org/doc/Documentation/filesystems/proc.txt
        val reader = BufferedReader(FileReader("/proc/stat"))
        val sa = reader.readLine().split("[ ]+".toRegex(), 9).toTypedArray()
        reader.close()

        val work: Long = sa[1].toLong() + sa[2].toLong() + sa[3].toLong()
        val total = work + sa[4].toLong() + sa[5].toLong() + sa[6].toLong() + sa[7].toLong()

        return listOf(work, total)
    }

    private suspend fun getFromVmStat(): Int {
        val reader = Runtime.getRuntime()
            .exec("vmstat")
            .inputStream
            .bufferedReader()
//        for (i in 1..3) {
//            output = reader.readLine()
//            Log.d("vmstat", "VMSTAT Riga $i: " + output)
//        }
        // TODO
        return 0;
    }

    private suspend fun getFromTop(): Int {
        var reader = Runtime.getRuntime()
            .exec("top -n1")
            .inputStream
            .bufferedReader()
        var output: String? = null
        for (i in 1..4) {
            output = reader.readLine()
            Log.d("vmstat", "TOP Riga $i: " + output)
        }
        // TODO
        return 0
    }

    private suspend fun getFromFrequency(): Int {
        // TODO
        return 0
    }
}
