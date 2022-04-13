package com.example.perfpuppy.ui.dashboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.perfpuppy.data.CollectorService
import com.example.perfpuppy.databinding.FragmentDashboardBinding

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

//        dashboardViewModel.serviceEnabled.observe(viewLifecycleOwner) {
//        }
        binding.serviceToggleButton.setOnCheckedChangeListener { _, isChecked ->
            toggleCollectorService(isChecked)
        }

        return root
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
