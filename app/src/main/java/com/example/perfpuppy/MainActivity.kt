package com.example.perfpuppy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.perfpuppy.data.CollectorService
import com.example.perfpuppy.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_settings,
                R.id.navigation_alerts,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onStop() {
        super.onStop()

        // Stop collector service if background mode is disabled
        val bgServiceEnabled = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getBoolean(
                getString(R.string.enable_bg_service_pref_key),
                resources.getBoolean(R.bool.enable_bg_service_default_value)
            )
        Timber.d("onStop: background service enabled: $bgServiceEnabled")
        if (!bgServiceEnabled) {
            stopService(Intent(this, CollectorService::class.java))
        }
    }
}
