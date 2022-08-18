package com.fimbleenterprises.whereyouat.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.data.MainRepositoryImpl
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class MainViewModel(
    private val createTripWithApiUseCase: CreateTripWithApiUseCase,
    private val deleteAllMemberLocsFromDbUseCase: DeleteAllMemberLocsFromDbUseCase,
    private val getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
    private val getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val _memberLocationsApiResponse: MutableLiveData<Resource<MemberLocationsApiResponse>> = MutableLiveData()
    val memberLocationsApiResponse: LiveData<Resource<MemberLocationsApiResponse>> = _memberLocationsApiResponse

    private val _createTripApiResponse: MutableLiveData<Resource<BaseApiResponse>> = MutableLiveData()
    val createTripApiResponse: LiveData<Resource<BaseApiResponse>> = _createTripApiResponse

    private val _memberLocations: MutableLiveData<List<LocUpdate>> = MutableLiveData()
    val memberLocations: LiveData<List<LocUpdate>> = _memberLocations

    private val _myLocation: MutableLiveData<MyLocation> = MutableLiveData()
    val myLocation: LiveData<MyLocation> = _myLocation

    private val _downloadResponse: MutableLiveData<Boolean> = MutableLiveData()
    val downloadResponse = _downloadResponse

    @Inject
    lateinit var repository: MainRepositoryImpl

    @Inject
    lateinit var s1: SaveMemberLocsToDbUseCase

    fun removeAllSavedLocs() = viewModelScope.launch {
        deleteAllMemberLocsFromDbUseCase.execute()
    }

    fun createTrip(memberid: Long) = viewModelScope.launch {
        createTripWithApiUseCase.execute(memberid).collect { apiResponse ->
            withContext(Main) {
                _createTripApiResponse.value = apiResponse
                WhereYouAt.AppPreferences.tripcode = apiResponse.data?.genericValue ?: ""
                Log.i(TAG, "-=MainViewModel:createTrip ${apiResponse.data?.genericValue ?: "no code!"} =-")
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
    companion object { private const val TAG = "FIMTOWN|MainViewModel" }

}