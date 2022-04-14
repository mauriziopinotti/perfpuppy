package com.example.perfpuppy.data.agent

import androidx.lifecycle.Lifecycle
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader

class MemoryAgent(
    service: CollectorServiceCallback,
    lifecycle: Lifecycle,
) : Agent(service, lifecycle) {

    override val name: String
        get() = context.getString(R.string.mem_agent_name)

    override fun aboveThMessage(value: Int): String =
        context.getString(R.string.mem_above_th_message, value)

    override fun belowThMessage(value: Int): String =
        context.getString(R.string.mem_below_th_message)

    override suspend fun getData(): PerfValue {
        val th = prefs.getInt(context.getString(R.string.mem_alert_pref_key), 0)

        return parseProcMemInfo().toPerfValue(th)
    }

    private fun parseProcMemInfo(): Int {
        try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            val lines = mutableListOf<Array<String>>()
            repeat(4) {
                lines.add(reader.readLine()?.split("[ ]+".toRegex())?.toTypedArray()!!)
            }
            reader.close()

            // MemAvailable: the amount of memory available for starting new applications without swapping.
            // MemFree: the amount of physical RAM, in KB, left unused by the system.
            val total = lines[0][1].toLong()
            val free = lines[1][1].toLong()
            val available = lines[2][1].toLong()
            val cached = lines[3][1].toLong()
//            val used = total - free - cached
            val used = available * 100 / total

            return 100 - used.toInt()
        } catch (e: Exception) {
            Timber.w("Cannot parse /proc/meminfo", e)
            return 0
        }
    }
}
