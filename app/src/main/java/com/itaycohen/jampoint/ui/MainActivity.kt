package com.itaycohen.jampoint.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.ui.home.FindJamsFragment
import com.itaycohen.jampoint.ui.home.FindJamsViewModel

class MainActivity : AppCompatActivity() {

    private val navController: NavController by lazy {
        /*
        * When creating the NavHostFragment using FragmentContainerView or if manually adding the NavHostFragment to your activity
        * via a FragmentTransaction, attempting to retrieve the NavController in onCreate() of an Activity
        *  via Navigation.findNavController(Activity, @IdRes int) will fail. You should retrieve the NavController
        * directly from the NavHostFragment instead.*/
        (supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment).findNavController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // I must use this deprecate callback, because [ResolvableApiException.startResolutionForResult]
        // Doesn't support [ActivityResultContract] yet. and either doesn't support Fragment.onActivityResult
        // thats why im forwarding the callback manually.
        if (requestCode == FindJamsViewModel.REQUEST_CHECK_LOCATOIN_SETTINGS) {
            (supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment)
                .childFragmentManager
                .fragments
                .find { it is FindJamsFragment }
                ?.onActivityResult(requestCode, resultCode, data)
        }
    }
}

