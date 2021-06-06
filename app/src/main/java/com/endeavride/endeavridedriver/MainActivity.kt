package com.endeavride.endeavridedriver

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.endeavride.endeavridedriver.databinding.ActivityMainBinding
import com.endeavride.endeavridedriver.ui.maps.MapsFragment
import com.endeavride.endeavridedriver.ui.ui.login.LoginFragment
import com.endeavride.endeavridedriver.ui.ui.login.LoginViewModel
import com.endeavride.endeavridedriver.ui.ui.login.LoginViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: MapsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapFragment = MapsFragment()

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
//        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
//            .get(LoginViewModel::class.java)
//
//        loginViewModel.loggedInUser.observe(this,
//            Observer { loggedInUser ->
//                loggedInUser ?: return@Observer
//                if (loggedInUser.displayName == "") {
//                    navController.navigate(R.id.navigation_login)
//                    navView.visibility = View.GONE
//                } else {
//                    navView.visibility = View.VISIBLE
//                }
//            })
//        loginViewModel.loadUserInfoIfAvailable()

        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(LoginFragment.LOGIN_SUCCESSFUL)
            .observe(currentBackStackEntry, Observer { success ->
                if (success) {
                    navView.visibility = View.VISIBLE
                }
            })
    }

    override fun onPause() {
        super.onPause()
        println("#K_app on pause")
    }

    override fun onStop() {
        super.onStop()
        println("#K_app on stop")
    }

    override fun onDestroy() {
        println("#K_app on destroy")
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MapsFragment.LOCATION_PERMISSION_REQUEST_CODE) {
            mapFragment.onRequestPermissionsResult(requestCode,
                permissions as Array<String>, grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}