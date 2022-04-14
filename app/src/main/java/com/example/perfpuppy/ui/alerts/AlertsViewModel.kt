package com.example.perfpuppy.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perfpuppy.repository.AlertsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    val data = alertsRepository.alerts

    init {
        viewModelScope.launch(Dispatchers.IO) {
            alertsRepository.alerts
        }
    }
}