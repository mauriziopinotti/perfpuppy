package com.example.perfpuppy.ui.dashboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.perfpuppy.R
import com.example.perfpuppy.data.CollectorService
import com.example.perfpuppy.databinding.FragmentDashboardBinding
import timber.log.Timber

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val dashboardViewModel =
//            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.serviceToggleButtonGroup.addOnButtonCheckedListener() { _, _, isChecked ->
            toggleCollectorService(isChecked)
            setServiceToggleButtonText(isChecked)
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        // Service enabled button
        CollectorService.isServiceRunning(requireContext()).also {
            Timber.d("Checking collector service running: $it")
            if (it) binding.serviceToggleButtonGroup.check(binding.serviceToggleButton.id)
//            else binding.serviceToggleButtonGroup.clearChecked()
            setServiceToggleButtonText(it)
        }
    }

    private fun setServiceToggleButtonText(enabled: Boolean) {
        Timber.d("setServiceToggleButtonText: enabled=$enabled")

        binding.serviceToggleButton.text =
            if (enabled) getString(R.string.disable_data_collection)
            else getString(R.string.enable_data_collection)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toggleCollectorService(enable: Boolean) {
        val intent = Intent(context, CollectorService::class.java)
        if (enable) {
            // Start collector service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.startForegroundService(intent)
            } else {
                activity?.startService(intent)
            }
        } else {
            // Stop collector service
            activity?.stopService(intent)
        }
    }
}
