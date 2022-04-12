package com.example.datapuppy.ui.home

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.datapuppy.databinding.FragmentHomeBinding
import java.io.BufferedReader
import java.io.FileReader

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Thread {
            while (true) {
//			CPU usage percents calculation. It is possible negative values or values higher than 100% may appear.
//			http://stackoverflow.com/questions/1420426
//			http://kernel.org/doc/Documentation/filesystems/proc.txt
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    val reader = BufferedReader(FileReader("/proc/stat"))
                    val sa = reader.readLine().split("[ ]+".toRegex(), 9).toTypedArray()
                    val work: Long = sa[1].toLong() + sa[2].toLong() + sa[3].toLong()
                    val total =
                        work + sa[4].toLong() + sa[5].toLong() + sa[6].toLong() + sa[7].toLong()
                    reader.close()
                    Log.d("vmstat", "OLD API total: $total work=$work")
                }

//                else {
//                val p = Runtime.getRuntime().exec("vmstat")
//                p.waitFor()
//                val stdOut = IOUtils.toString(p.inputStream, Charsets.UTF_8)
//                val stdErr = IOUtils.toString(p.errorStream, Charsets.UTF_8)

                    var reader = Runtime.getRuntime()
                        .exec("top -n1")
                        .inputStream
                        .bufferedReader()
                    var output: String? = null
                    for (i in 1..4) {
                        output = reader.readLine()
                        Log.d("vmstat", "TOP Riga $i: " + output)
                    }

                    reader = Runtime.getRuntime()
                        .exec("vmstat")
                        .inputStream
                        .bufferedReader()
                    for (i in 1..3) {
                        output = reader.readLine()
                        Log.d("vmstat", "VMSTAT Riga $i: " + output)
                    }

                    reader = Runtime.getRuntime()
                        .exec("cat /proc/stat")
                        .inputStream
                        .bufferedReader()
                    for (i in 1..3) {
                        output = reader.readLine()
                        Log.d("vmstat", "PROC/STAT Riga $i: " + output)
                    }

                    Log.d("vmstat", "---------------------------------------------------")

//                val output = executeCommand("vmstat")
//                val reader = BufferedReader(FileReader("/proc/stat"))
//                val sa = reader.readLine().split("[ ]+".toRegex(), 9).toTypedArray()
                        Log.d("vmstat", output ?: "N/D")
                    activity?.runOnUiThread(Runnable {
                        binding.textHome.text = "CPU: " + output
                    })
//                }

                SystemClock.sleep(5000)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

//fun String.runCommand(
//    workingDir: File = File("."),
//    timeoutAmount: Long = 60,
//    timeoutUnit: TimeUnit = TimeUnit.SECONDS
//): String? = runCatching {
//    ProcessBuilder("\\s".toRegex().split(this))
//        .directory(workingDir)
//        .redirectOutput(ProcessBuilder.Redirect.PIPE)
//        .redirectError(ProcessBuilder.Redirect.PIPE)
//        .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
//        .inputStream.bufferedReader().readText()
//}.onFailure { it.printStackTrace() }.getOrNull()

