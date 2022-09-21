package com.fimbleenterprises.whereyouat.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.service.ServiceMessenger
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
    private val createTripWithApiUseCase: CreateTripWithApiUseCase,
    private val getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
    private val getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
    private val validateTripCodeAgainstApiUseCase: ValidateTripCodeAgainstApiUseCase,
    private val serviceStateUseCases: ServiceStateUseCases,
    private val uploadMyLocToApiUseCase: UploadMyLocToApiUseCase,
    private val getMemberLocsFromApiUseCase: GetMemberLocsFromApiUseCase,
    private val serviceMessenger: ServiceMessenger,
    private val validateClientTripCodeUseCase: ValidateClientTripCodeUseCase,
    private val validateApiServerRunningUseCase: ValidateApiServerRunningUseCase
):ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            serviceStateUseCases,
            validateTripCodeAgainstApiUseCase,
            createTripWithApiUseCase,
            getMemberLocsFromDbUseCase,
            getMyLocFromDbUseCase,
            uploadMyLocToApiUseCase,
            getMemberLocsFromApiUseCase,
            serviceMessenger,
            validateClientTripCodeUseCase,
            validateApiServerRunningUseCase,
            app
        ) as T
    }

    init { Log.i(TAG, "Initialized:MainViewModelFactory") }
    companion object { private const val TAG = "FIMTOWN|MainViewModelFactory" }
}









