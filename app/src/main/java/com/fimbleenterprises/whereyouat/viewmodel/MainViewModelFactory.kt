package com.fimbleenterprises.whereyouat.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fimbleenterprises.whereyouat.data.TripRepository
import javax.inject.Singleton

/**
 * Since our ViewModel class has constructors we need to create this factory class in order to
 * create one as viewmodels that contain constructors cannot be directly instantiated (this is just
 * one of the quirks of Android's ViewModel framework).  To create the ViewModel you will create
 * it like so:
 * ```
 * viewModel = ViewModelProvider(this,factory).get(NewsViewModel::class.java)
 */
@Singleton
@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val app:Application,
    private val tripRepository: TripRepository
):ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            tripRepository,
            app
        ) as T
    }

    init { Log.i(TAG, "Initialized:MainViewModelFactory") }
    companion object { private const val TAG = "FIMTOWN|MainViewModelFactory" }
}









