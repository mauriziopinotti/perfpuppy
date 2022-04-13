package com.example.perfpuppy.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
//        val notificationsViewModel =
//            ViewModelProvider(this).get(AlertsViewModel::class.java)

        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.adapter = adapter

//        val textView: TextView = binding.textNotifications
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.data.observe(viewLifecycleOwner) {
            if (it?.isNotEmpty() == true) {
                adapter.submitList(it)
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else {
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