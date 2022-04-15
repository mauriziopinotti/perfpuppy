package com.example.perfpuppy.data.agent

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorServiceCallback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class CpuAgentTest {

    private lateinit var agent: CpuAgent

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        val cb = object : CollectorServiceCallback {
            override val context: Context
                get() = appContext

            override fun onDataAboveTh(agent: Agent, value: Int) {
            }

            override fun onDataBelowTh(agent: Agent, value: Int) {
            }
        }

        agent = CpuAgent(cb)
    }

    @Test
    fun getData() {
        val fakeProcStatFile = File.createTempFile("cpuinfo", "")
        fakeProcStatFile.deleteOnExit()

//        mockkStatic(CpuAgent::CPUINFO_FILE)
//        every { CpuAgent.CPUINFO_FILE } returns fakeProcStatFile.absolutePath

        val procStatField = CpuAgent::class.java.getDeclaredField("CPUINFO_FILE")
        procStatField.isAccessible = true
        procStatField.set(null, fakeProcStatFile.absolutePath)

        // Override SDK_INT to pretend /proc/stat is readable
        val sdkIntField = Build.VERSION::class.java.getDeclaredField("SDK_INT")
        sdkIntField.isAccessible = true
        sdkIntField.set(null, Build.VERSION_CODES.M)

        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

        runBlocking {
            // Fake content of /proc/stat
            fakeProcStatFile.writeText(
                """
                cpu  329581 338 37331 2262853 3321 0 4096 0 22839 0
                cpu0 40426 33 4583 284415 387 0 1257 0 3124 0
                cpu1 41831 24 4311 278744 399 0 1209 0 3283 0
                cpu2 42200 88 4538 281858 447 0 524 0 2852 0
                cpu3 45063 24 7307 278667 369 0 188 0 2514 0
                cpu4 40961 85 4365 284031 452 0 238 0 2489 0
                cpu5 42158 44 4009 282982 402 0 151 0 2728 0
                cpu6 41162 24 3798 284473 417 0 98 0 3134 0
                cpu7 35778 13 4419 287680 446 0 427 0 2712 0
                """.trimIndent()
            )
            thread(start = true) {
                SystemClock.sleep(1000)
                fakeProcStatFile.writeText(
                    """
                    cpu  444646 339 50164 3198916 4033 0 5864 0 30945 0
                    cpu0 54596 33 6434 402057 493 0 1774 0 4069 0
                    cpu1 56776 24 5973 393835 502 0 1723 0 4384 0
                    cpu2 56631 89 6233 399022 544 0 753 0 3924 0
                    cpu3 60427 24 8690 393802 461 0 267 0 3491 0
                    cpu4 55714 85 6035 401241 532 0 341 0 3487 0
                    cpu5 56838 44 5462 400303 466 0 201 0 3767 0
                    cpu6 54959 24 5371 402627 504 0 159 0 4239 0
                    cpu7 48701 13 5963 406025 526 0 643 0 3581 0
                    """.trimIndent()
                )
            }

            // With the default threshold this is below th
            prefs.edit().apply {
                putInt(appContext.getString(R.string.cpu_alert_pref_key), 40).commit()
                putBoolean("procStatSupported", true)
                commit()
            }
            val data = agent.getData()
            assertEquals(11, data.value)
            assertEquals(false, data.valueIsAboveTh)
        }
    }
}
