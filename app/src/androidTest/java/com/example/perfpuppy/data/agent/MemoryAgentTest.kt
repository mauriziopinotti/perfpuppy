package com.example.perfpuppy.data.agent

import android.content.Context
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

@RunWith(AndroidJUnit4::class)
class MemoryAgentTest {

    private lateinit var agent: MemoryAgent

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
//        val lifecycleOwner: LifecycleOwner = Mockito.mock(LifecycleOwner::class.java)
//        val lifecycle = LifecycleRegistry(Mockito.mock(LifecycleOwner::class.java))
//        lifecycle.currentState = Lifecycle.State.STARTED
//        Mockito.`when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)

        val cb = object : CollectorServiceCallback {
            override val context: Context
                get() = appContext

            override fun onDataAboveTh(agent: Agent, value: Int) {
            }

            override fun onDataBelowTh(agent: Agent, value: Int) {
            }
        }

        agent = MemoryAgent(cb)
    }

    @Test
    fun getData() {
        val fakeMemInfoFile = File.createTempFile("meminfo", "")
        fakeMemInfoFile.deleteOnExit()

//        mockkStatic(MemoryAgent::MEMINFO_FILE)
//        every { MemoryAgent.MEMINFO_FILE } returns fakeMemInfoFile.absolutePath

        val memInfoFileField = MemoryAgent::class.java.getDeclaredField("MEMINFO_FILE")
        memInfoFileField.isAccessible = true
        memInfoFileField.set(null, fakeMemInfoFile.absolutePath)

        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

        runBlocking {
            // Fake content of /proc/meminfo that has 43% free memory
            fakeMemInfoFile.writeText(
                """
                MemTotal:       32590772 kB
                MemFree:         8025748 kB
                MemAvailable:   18532968 kB
                Buffers:          608612 kB
                Cached:         11676508 kB
                SwapCached:            0 kB
                Active:          5697980 kB
            """.trimIndent()
            )

            // With the default threshold this is below th
            prefs.edit().putInt(appContext.getString(R.string.mem_alert_pref_key), 80).commit()
            var data = agent.getData()
            assertEquals(43, data.value)
            assertEquals(false, data.valueIsAboveTh)

            // Set a lower threshold to get a value above th
            prefs.edit().putInt(appContext.getString(R.string.mem_alert_pref_key), 40).commit()
            data = agent.getData()
            assertEquals(43, data.value)
            assertEquals(true, data.valueIsAboveTh)
        }
    }
}