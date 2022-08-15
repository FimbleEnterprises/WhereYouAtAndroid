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
import com.fimbleenterprises.whereyouat.data.TripRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor
    (
    private val tripRepository: TripRepository,
    application: Application
) : AndroidViewModel(application) {


    private val _memberId: MutableLiveData<Long> = MutableLiveData()
    val memberId: LiveData<Long> = _memberId

    private val _tripcode: MutableLiveData<String> = MutableLiveData()
    val tripCode: LiveData<String> = _tripcode

    private val _memberLocationsApiResponse: MutableLiveData<Resource<MemberLocationsApiResponse>> = MutableLiveData()
    val memberLocationsApiResponse: LiveData<Resource<MemberLocationsApiResponse>> = _memberLocationsApiResponse

    private val _createTripApiResponse: MutableLiveData<Resource<BaseApiResponse>> = MutableLiveData()
    val createTripApiResponse: LiveData<Resource<BaseApiResponse>> = _createTripApiResponse

    private val _downloadResponse: MutableLiveData<Boolean> = MutableLiveData()
    val downloadResponse = _downloadResponse

    fun getMemberLocations(tripcode: String = _tripcode.value!!) = viewModelScope.launch {
        tripRepository.getMemberLocations(tripcode).collect { values ->
            // Set the livedata for the benefit of observers.
            _memberLocationsApiResponse.value = values
            // Save member locs to the db.
            values.data?.locUpdates?.forEach {
                tripRepository.saveMemberLocation(it)
            }
        }
    }

    fun setMemberId(memberid: Long) {
        _memberId.value = memberid
    }

    fun uploadMyLocation() = viewModelScope.launch {
        val locUpdate = LocUpdate(
            memberId.value!!,
            System.currentTimeMillis(),
            45,
            12.445554,
            5.444333,
            tripCode.value!!
        )
        tripRepository.uploadMyLocation(locUpdate).collect { apiResponse ->
            withContext(Main) {
                Log.i(TAG,
                    "-=MainViewModel:uploadMyLocation ${apiResponse.data?.genericValue} =-")
            }
        }
    }

    fun createTrip(memberid: Long) = viewModelScope.launch {
        tripRepository.createTrip(memberid).collect { apiResponse ->
            withContext(Main) {
                _createTripApiResponse.value = apiResponse
                _tripcode.value = apiResponse.data?.genericValue
                Log.i(TAG, "-=MainViewModel:createTrip ${apiResponse.data?.genericValue ?: "no code!"} =-")
            }
        }
    }

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

    fun setTripCode(tripcode: String) {
        _tripcode.value = tripcode
    }

    init { Log.i(TAG, "Initialized:MainViewModel") }
    companion object { private const val TAG = "FIMTOWN|MainViewModel" }

}