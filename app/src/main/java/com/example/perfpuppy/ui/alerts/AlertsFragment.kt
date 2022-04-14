package com.example.perfpuppy.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.example.perfpuppy.databinding.FragmentAlertsBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlertsFragment : Fragment() {
    @Inject
    lateinit var adapter: AlertsAdapter

    private val viewModel: AlertsViewModel by viewModels()

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.adapter = adapter

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.data.observe(viewLifecycleOwner) {
            if (it?.isNotEmpty() == true) {
                // Set data
                adapter.submitList(it)
                // Scroll to reveal first item
                if (binding.recyclerView.scrollState == SCROLL_STATE_IDLE && binding.recyclerView.computeVerticalScrollOffset() == 0) {
                    binding.recyclerView.smoothScrollToPosition(0)
                }
                // Ensure list is visible
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else {
                // Show empty view
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
        }
        adapter.clickListener.onItemClick = {
            Timber.d("Clicked item $it")
//            findNavController().navigate(UserListFragmentDirections.actionUsersListToUserDetails(it.username))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}