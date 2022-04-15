package com.example.perfpuppy.data.agent

import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader

class MemoryAgent(
    service: CollectorServiceCallback,
//    lifecycle: Lifecycle,
) : Agent(service) {

    companion object {
        // Not const to be overridden by testing
        val MEMINFO_FILE = "/proc/meminfo"
    }

    override val name: String
        get() = context.getString(R.string.mem_agent_name)

    override fun aboveThMessage(value: Int): String =
        context.getString(R.string.mem_above_th_message, value)

    override fun belowThMessage(value: Int): String =
        context.getString(R.string.mem_below_th_message)

    override suspend fun getData(): PerfValue {
        val th = prefs.getInt(
            context.getString(R.string.mem_alert_pref_key),
            context.resources.getInteger(R.integer.mem_alert_default_th)
        )

        return parseProcMemInfo().toPerfValue(th)
    }

    private fun parseProcMemInfo(): Int {
        try {
            val reader = BufferedReader(FileReader(MEMINFO_FILE))
            val lines = mutableListOf<Array<String>>()
            repeat(4) {
                lines.add(reader.readLine()?.split("[ ]+".toRegex())?.toTypedArray()!!)
            }
            reader.close()

            // MemAvailable: the amount of memory available for starting new applications without swapping.
            // MemFree: the amount of physical RAM, in KB, left unused by the system.
            val total = lines[0][1].toLong()
            val free = lines[1][1].toLong()
            val hasAvailableMem = lines[2][0].startsWith("MemAvailable")
            val available = if (hasAvailableMem) lines[2][1].toLong() else 0
            val cached = lines[3][1].toLong()

            // Some devices (e.g. Nexus 4) don't have MemAvailable, in that case fallback to MemFree
            val used =
                if (hasAvailableMem) total - available
                else total - free - cached

            return (used * 100 / total).toInt()
        } catch (e: Exception) {
            Timber.w("Cannot parse /proc/meminfo", e)
            return 0
        }
    }
}
