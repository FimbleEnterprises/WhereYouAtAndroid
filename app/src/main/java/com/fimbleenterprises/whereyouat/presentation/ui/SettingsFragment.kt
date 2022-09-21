package com.fimbleenterprises.whereyouat.presentation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.navigation.NavBackStackEntry
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R


class SettingsFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    init { Log.i(TAG, "Initialized:SettingsFragment") }
    companion object { private const val TAG = "FIMTOWN|SettingsFragment" }
}