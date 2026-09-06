package com.masesas.exercise.bcaf_test_1.presentation.legacy

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.masesas.exercise.bcaf_test_1.R
import com.masesas.exercise.bcaf_test_1.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/** Host nav graph; bottom navigation disembunyikan pada child fragment. */
@AndroidEntryPoint
class HomeActivityLegacy : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private companion object {
        /** Destination yang tampil tanpa bottom navigation. */
        val FULL_SCREEN_DESTINATIONS = setOf(
            R.id.transactionDetailFragment,
            R.id.formProfileFragment,
            R.id.dokumenProfileFragment,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavController diambil dari NavHostFragment, bukan dari view.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.transactionFragment,
                R.id.notificationFragment,
                R.id.profileFragment,
            )
        )

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Tab dipetakan ke destination lewat kesamaan ID.
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in FULL_SCREEN_DESTINATIONS) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
