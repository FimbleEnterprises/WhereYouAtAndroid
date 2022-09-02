package com.fimbleenterprises.whereyouat.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.data.MainRepositoryImpl
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.service.ServiceMessenger
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.cancellable
import javax.inject.Inject

class MainViewModel(
    private val saveServiceStatusUseCase: SaveServiceStatusUseCase,
    private val getServiceStatusUseCase: GetServiceStatusUseCase,
    private val validateTripCodeAgainstApiUseCase: ValidateTripCodeAgainstApiUseCase,
    private val createTripWithApiUseCase: CreateTripWithApiUseCase,
    private val getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
    private val getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
    private val uploadMyLocToApiUseCase: UploadMyLocToApiUseCase,
    private val getMemberLocsFromApiUseCase: GetMemberLocsFromApiUseCase,
    private val serviceMessenger: ServiceMessenger,
    private val validateClientTripCodeUseCase: ValidateClientTripCodeUseCase,
    val app: Application
) : AndroidViewModel(app) {

    private val _memberLocationsApiResponse: MutableLiveData<Resource<MemberLocationsApiResponse>> = MutableLiveData()
    val memberLocationsApiResponse: LiveData<Resource<MemberLocationsApiResponse>> = _memberLocationsApiResponse

    private val _tripcodeCreated: MutableLiveData<Boolean> = MutableLiveData()
    val tripcodeCreated: LiveData<Boolean> = _tripcodeCreated

    private val _oneTimeServiceStatusMsg: MutableLiveData<String?> = MutableLiveData()
    val oneTimeServiceStatusMsg: LiveData<String?> = _oneTimeServiceStatusMsg

    private val _memberLocations: MutableLiveData<List<LocUpdate>> = MutableLiveData()
    val memberLocations: LiveData<List<LocUpdate>> = _memberLocations

    private val _myLocation: MutableLiveData<MyLocation> = MutableLiveData()
    val myLocation: LiveData<MyLocation> = _myLocation

    private val _downloadResponse: MutableLiveData<Boolean> = MutableLiveData()
    val downloadResponse = _downloadResponse

    private val _serviceStatus: MutableLiveData<ServiceStatus> = MutableLiveData()
    val serviceStatus: LiveData<ServiceStatus> = _serviceStatus

    @Inject
    lateinit var repository: MainRepositoryImpl

    @Inject
    lateinit var s1: SaveMemberLocsToDbUseCase

    fun stopService() {

        setServiceStoppingStatus(true)

        // Kill the service from the main thread.
        CoroutineScope(Main).launch {
            val cancelIntent = Intent(getApplication(), TripUsersLocationManagementService::class.java)
            cancelIntent.putExtra(TripUsersLocationManagementService.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
            app.startService(cancelIntent)
        }
    }

/*    fun requestForcePush() {
        Toast.makeText(app, "Change this to a startService(withIntent) operation!", Toast.LENGTH_SHORT).show()
        setServiceStatus(
            ServiceStatus(
                isStarting = false,
                isRunning = true,
                forceUpdate = true
            )
        )
    }*/

    /**
     * Simply saves the ServiceStatus to isStarting in the db.
     */
    fun requestTripStart() {
        setServiceStartingStatus(true)
    }

    /*private fun setServiceStatus(serviceStatus: ServiceStatus) {

        // Update in mem
        _serviceStatus.value = serviceStatus

        // Update in persistent
        viewModelScope.launch(IO) {
            saveServiceStatusUseCase.execute(serviceStatus)
            withContext(Main) {
                getServiceStatusUseCase.executeFlow().cancellable().collect() {
                    _serviceStatus.value = it
                    cancel()
                }
            }
        }
    }*/

    fun setServiceRunningStatus(isRunning: Boolean) {
        viewModelScope.launch(IO) {
            saveServiceStatusUseCase.setServiceRunning(isRunning)
        }
    }

    fun setServiceStartingStatus(isStarting: Boolean) {
        viewModelScope.launch(IO) {
            saveServiceStatusUseCase.setServiceStarting(isStarting)
        }
    }

    fun setServiceStoppingStatus(isStopping: Boolean) {
        viewModelScope.launch(IO) {
            saveServiceStatusUseCase.setServiceStopping(isStopping)
        }
    }

    fun setServiceStatus(serviceStatus: ServiceStatus) {
        viewModelScope.launch(IO) {
            saveServiceStatusUseCase.execute(serviceStatus)
        }
    }

    fun tripcodeIsValidClientside(tripcode: String) = validateClientTripCodeUseCase.execute(tripcode)

    suspend fun validateCode(tripcode: String) = validateTripCodeAgainstApiUseCase.execute(tripcode)

    fun createTrip(memberid: Long) = viewModelScope.launch {
        createTripWithApiUseCase.execute(memberid).collect { apiResponse ->
            withContext(Main) {
                when(apiResponse) {
                    is Resource.Success -> {
                        WhereYouAt.AppPreferences.tripCode = apiResponse.data?.genericValue
                        _serviceStatus.value = ServiceStatus(isStarting = true, isRunning = false)
                        saveServiceStatusUseCase.execute(
                            serviceStatus.value!!
                        )
                        _tripcodeCreated.value = true
                        Log.i(TAG, "-=MainViewModel:createTrip ${apiResponse.data?.genericValue ?: "no code!"} =-")
                    }
                    is Resource.Loading -> { }
                    is Resource.Error -> { }
                    else -> {}
                }
            }
        }
    }

    // -----------------------------------------------------------
    //                       CHECK NETWORK
    // -----------------------------------------------------------
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // -----------------------------------------------------------
    //                       INITIALIZE
    // -----------------------------------------------------------
    init {
        Log.i(TAG, "Initialized:MainViewModel")


        viewModelScope.launch {
            // Continuously monitor the service status table.
            getServiceStatusUseCase.executeFlow().collect() {
                _serviceStatus.value = it
                _oneTimeServiceStatusMsg.value = it.oneTimeMessage
            }
        }
        viewModelScope.launch {
            // Continuously monitor the member_locations table.
            getMemberLocsFromDbUseCase.execute().collect {
                _memberLocations.value = it
            }
        }
        viewModelScope.launch {
            // Continuously monitor the my_location table.
            getMyLocFromDbUseCase.execute().collect {
                _myLocation.value = it
            }
        }
    }
    companion object {
        private const val TAG = "FIMTOWN|MainViewModel"
        private const val TAG2 = "TAG2"
    }

}