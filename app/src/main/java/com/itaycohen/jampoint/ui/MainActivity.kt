package com.itaycohen.jampoint.ui

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.itaycohen.jampoint.R

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
}

