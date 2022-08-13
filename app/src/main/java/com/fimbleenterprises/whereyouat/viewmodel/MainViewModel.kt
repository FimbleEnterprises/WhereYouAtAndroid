package com.fimbleenterprises.whereyouat.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fimbleenterprises.whereyouat.data.TripRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor
    (
    private val tripRepository: TripRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _memberLocationsApiResponse: MutableLiveData<Resource<MemberLocationsApiResponse>> = MutableLiveData()
    val memberLocationsApiResponse: LiveData<Resource<MemberLocationsApiResponse>> = _memberLocationsApiResponse

    private val _createTripApiResponse: MutableLiveData<Resource<BaseApiResponse>> = MutableLiveData()
    val createTripApiResponse: LiveData<Resource<BaseApiResponse>> = _createTripApiResponse

    private val _downloadResponse: MutableLiveData<Boolean> = MutableLiveData()
    val downloadResponse = _downloadResponse

    fun getMemberLocations(tripcode: String) = viewModelScope.launch {
        tripRepository.getMemberLocations(tripcode).collect { values ->
            _memberLocationsApiResponse.value = values

            values.data?.memberLocations?.forEach {
                tripRepository.saveMemberLocation(it)
            }

        }
    }

    fun createTrip(memberid: Long) = viewModelScope.launch {
        tripRepository.createTrip(memberid).collect { apiResponse ->
            _createTripApiResponse.value = apiResponse
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

}