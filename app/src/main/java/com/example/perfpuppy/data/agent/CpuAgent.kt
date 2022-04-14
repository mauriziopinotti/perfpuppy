package com.example.perfpuppy.data.agent

import android.os.Build
import androidx.lifecycle.Lifecycle
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

class CpuAgent(
    service: CollectorServiceCallback,
    lifecycle: Lifecycle,
) : Agent(service, lifecycle) {

    internal class NotSupportedException : RuntimeException()

    companion object {
        private val TOP_PATTERN_1: Pattern =
            Pattern.compile("(\\d+)%cpu\\s+(\\d+)%user\\s+(\\d+)%nice\\s+(\\d+)%sys\\s+(\\d+)%idle\\s+(\\d+)%iow\\s+(\\d+)%irq")
        private val TOP_PATTERN_2: Pattern =
            Pattern.compile("User\\s+(\\d+)%,\\s+System\\s+(\\d+)%,\\s+IOW\\s+(\\d+)%,\\s+IRQ\\s+(\\d+)%")
    }

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

        val th = prefs.getInt(
            context.getString(R.string.cpu_alert_pref_key),
            context.resources.getInteger(R.integer.cpu_alert_default_th)
        )
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
//        data = tryGetData(this::getFromFrequency)
//        if (data != null) return data.toPerfValue(th)

        return 0.toPerfValue(th)
    }

    private suspend fun tryGetData(method: suspend () -> Int, prefKey: String? = null): Int? {
        try {
            // Try loading data using this method, unless we already know it's not supported
            if (prefKey == null || prefs.getBoolean(prefKey, true)) return method.invoke()
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

    private fun getFromVmStat(): Int {
        try {
            val reader = Runtime.getRuntime()
                .exec(arrayOf("vmstat", "0", "1"))
                .inputStream
                .bufferedReader()
            var output: String? = null
            repeat(3) { output = reader.readLine() }
            val sa = output!!.split("[ ]+".toRegex()).toTypedArray()
            reader.close()

            return 100 - sa[sa.size - 2].toInt()
        } catch (e: Exception) {
            Timber.w("Cannot parse vmstat output", e)
        }
        throw NotSupportedException()
    }

    private fun getFromTop(): Int {
        try {
            val output = Runtime.getRuntime()
                .exec(arrayOf("top", "-n", "1", "-m", "1"))
                .inputStream
                .bufferedReader()
                .use { it.readText() }

            // Try method 1 (new devices like Pixel 4)
            val m1: Matcher = TOP_PATTERN_1.matcher(output)
            if (m1.find()) {
                val total = m1.group(1)?.toInt() ?: 0
                val idle = m1.group(5)?.toInt() ?: 0
                val numCores = total / 100;

                return (total - idle) / numCores
            }

            // Try method 2 (legacy devices like Nexus 4)
            val m2: Matcher = TOP_PATTERN_2.matcher(output)
            if (m2.find()) {
                val user = m2.group(1)?.toInt() ?: 0
                val system = m2.group(2)?.toInt() ?: 0
                val iow = m2.group(3)?.toInt() ?: 0
                val irq = m2.group(4)?.toInt() ?: 0

                return user + system + iow + irq
            }
        } catch (e: Exception) {
            Timber.w("Cannot parse top output", e)
        }
        throw NotSupportedException()
    }
}
