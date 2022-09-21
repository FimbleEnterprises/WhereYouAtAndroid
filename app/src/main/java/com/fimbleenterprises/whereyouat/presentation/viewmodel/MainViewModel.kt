package com.fimbleenterprises.whereyouat.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.data.MainRepositoryImpl
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.model.ServiceState.Companion.SERVICE_STATE_IDLE
import com.fimbleenterprises.whereyouat.model.ServiceState.Companion.SERVICE_STATE_RUNNING
import com.fimbleenterprises.whereyouat.model.ServiceState.Companion.SERVICE_STATE_STARTING
import com.fimbleenterprises.whereyouat.model.ServiceState.Companion.SERVICE_STATE_STOPPED
import com.fimbleenterprises.whereyouat.model.ServiceState.Companion.SERVICE_STATE_STOPPING
import com.fimbleenterprises.whereyouat.service.ServiceMessenger
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService.Companion.RIGOROUS_UPDATES_INTENT_EXTRA
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import javax.inject.Inject


class MainViewModel(
    private val serviceStateUseCases: ServiceStateUseCases,
    private val validateTripCodeAgainstApiUseCase: ValidateTripCodeAgainstApiUseCase,
    private val createTripWithApiUseCase: CreateTripWithApiUseCase,
    private val getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
    private val getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
    private val uploadMyLocToApiUseCase: UploadMyLocToApiUseCase,
    private val getMemberLocsFromApiUseCase: GetMemberLocsFromApiUseCase,
    private val serviceMessenger: ServiceMessenger,
    private val validateClientTripCodeUseCase: ValidateClientTripCodeUseCase,
    private val validateApiServerRunning: ValidateApiServerRunningUseCase,
    val app: Application,
) : AndroidViewModel(app) {

    private val _shareCodeAction: MutableLiveData<ShareCodeAction> = MutableLiveData()
    val shareCodeAction: LiveData<ShareCodeAction> = _shareCodeAction

    private val _apiEvent: MutableLiveData<ApiEvent?> = MutableLiveData()
    val apiEvent: LiveData<ApiEvent?> = _apiEvent

    private val _memberLocations: MutableLiveData<List<LocUpdate>> = MutableLiveData()
    val memberLocations: LiveData<List<LocUpdate>> = _memberLocations

    private val _myLocation: MutableLiveData<MyLocation> = MutableLiveData()
    val myLocation: LiveData<MyLocation> = _myLocation

    private val _downloadResponse: MutableLiveData<Boolean> = MutableLiveData()
    val downloadResponse = _downloadResponse

    private val _serviceState: MutableLiveData<ServiceState> = MutableLiveData()
    val serviceState: LiveData<ServiceState> = _serviceState

    @Inject
    lateinit var repository: MainRepositoryImpl

    @Inject
    lateinit var s1: SaveMemberLocsToDbUseCase

    fun shareTripcode(tripcode: String) {
        val action = ShareCodeAction(
            tripcode = tripcode,
            message = "\n\nHere's my WhereYouAt code:\n\n$tripcode\n\nGet WhereYouAt here: www.google.com",
            intentTag = TRIPCODE_INTENT_EXTRA_TAG
        )
        _shareCodeAction.value = action
    }

    fun requestServiceStop() {

        setServiceStopping()

    }

    /**
     * Simply saves the ServiceStatus to isStarting in the db.
     */
    fun requestTripStart() {
        setServiceStarting()
    }

    fun setServiceStarting() {
        viewModelScope.launch(IO) {
            Log.e(TAG2, "-= Setting:STARTING... =-")
            serviceStateUseCases.setServiceStarting()
        }
    }

    fun setServiceStopping() {
        viewModelScope.launch(IO) {
            Log.e(TAG2, "-= Setting:STOPPING... =-")
            serviceStateUseCases.setServiceStopping()
        }
    }
    
    fun setServiceIdle() {
        viewModelScope.launch(IO) {
            Log.e(TAG2, "-= Setting:IDLE... =-")
            serviceStateUseCases.setServiceState(
                ServiceState(
                    state = SERVICE_STATE_IDLE
                )
            )
        }
    }

    /**
     * Informs the service to either begin or cease rigorous updating.  When NOT rigorous the service
     * will send/receive locs according to its internal runners.  When rigorous, the service will
     * still use the runners but also send/receive any time the user's location has changed.
     */
    fun requestVigorousUpdates(boolean: Boolean) {
        val rigorousRequestIntent = Intent(app, TripUsersLocationManagementService::class.java)
        rigorousRequestIntent.putExtra(RIGOROUS_UPDATES_INTENT_EXTRA, boolean)
        app.startService(rigorousRequestIntent)
    }

    fun tripcodeIsValidClientside(tripcode: String) = validateClientTripCodeUseCase.execute(tripcode)

    suspend fun validateCode(tripcode: String) = validateTripCodeAgainstApiUseCase.execute(tripcode)

    fun createTrip(memberid: Long) = viewModelScope.launch {
        createTripWithApiUseCase.execute(memberid).collect { apiResponse ->
            withContext(IO) {
                when(apiResponse) {
                    is Resource.Success -> {
                        if (apiResponse.data?.wasSuccessful == true) {
                            // We know that the API will return the tripcode as a string using
                            // the GenericValue property.
                            AppPreferences.tripCode = apiResponse.data.genericValue.toString()
                            if (AppPreferences.tripCode != null) {
                                withContext(Main) {
                                    _apiEvent.value = ApiEvent(ApiEvent.Event.REQUEST_SUCCEEDED)
                                    serviceStateUseCases.setServiceStarting()
                                }
                            } else {
                                Toast.makeText(app, "Failed to create trip!", Toast.LENGTH_SHORT).show()
                                withContext(Main) {
                                    _apiEvent.value = ApiEvent(
                                        ApiEvent.Event.REQUEST_FAILED,
                                        apiResponse.data.genericValue?.toString()
                                    )
                                    _apiEvent.value = null
                                }
                            }
                            Log.i(TAG, "-=MainViewModel:createTrip ${ apiResponse.data.genericValue } =-")
                        } else {
                            withContext(Main) {
                                _apiEvent.value = ApiEvent(
                                    ApiEvent.Event.REQUEST_FAILED,
                                    apiResponse.data?.genericValue?.toString()
                                )
                                _apiEvent.value = null
                            }
                        }
                    }
                    is Resource.Loading -> { }
                    is Resource.Error -> {
                        withContext(Main) {
                            _apiEvent.value = ApiEvent(
                                ApiEvent.Event.REQUEST_FAILED
                            )
                            _apiEvent.value = null
                        }
                    }
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
            serviceStateUseCases.getServiceStateAsFlow().collect {
                _serviceState.value = it
                
                when (it.state) {
                    SERVICE_STATE_STARTING -> {
                        Log.e(TAG2, "-= viewmodel.collect.serviceState:STARTING =-")        
                    }
                    SERVICE_STATE_RUNNING -> {
                        Log.e(TAG2, "-= viewmodel.collect.serviceState:RUNNING =-")
                    }
                    SERVICE_STATE_STOPPING -> {
                        Log.e(TAG2, "-= viewmodel.collect.serviceState:STOPPING =-")
                    }
                    SERVICE_STATE_STOPPED -> {
                        setServiceIdle()
                        Log.e(TAG2, "-= viewmodel.collect.serviceState:STOPPED =-")
                    }
                    SERVICE_STATE_IDLE -> {
                        Log.e(TAG2, "-= viewmodel.collect.serviceState:IDLE =-")
                    }
                    else -> {
                        Log.e(TAG2, "-=viewmodel.collect.serviceState: $it =-")
                    }
                }
                
            }
        }
        viewModelScope.launch {
            // Continuously monitor the member_locations table.
            getMemberLocsFromDbUseCase.executeAsFlow().collect {
                _memberLocations.value = it
                Log.i("TAG4", "-=VIEWMODEL OBSERVES ${it.size} MEMBERS=-")
            }
        }
        viewModelScope.launch {
            // Continuously monitor the my_location table.
            getMyLocFromDbUseCase.execute().collect {
                _myLocation.value = it
                Log.d(TAG1, "-= MyLocation changed in database!: =-")
            }
        }
        viewModelScope.launch {
            validateApiServerRunning.execute().collect {
                Log.i(TAG, "-=Server running:${it.data?.wasSuccessful} =-")
            }
        }
    }


    companion object {
        private const val TAG = "FIMTOWN|MainViewModel"
        private const val TAG1 = "DATABASEOBSERVER"
        private const val TAG2 = "SERVICESTATE"
        const val TRIPCODE_INTENT_EXTRA_TAG = "com.fimbleenterprises.whereyouat.tripcode"
    }

}