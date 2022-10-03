package com.fimbleenterprises.whereyouat.data.remote

import android.util.Log
import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.model.FunctionName.*
import retrofit2.Response
import javax.inject.Singleton

@Singleton
class RemoteDataSourceImpl(private val whereYouAtWebApi: WhereYouAtWebApi): RemoteDataSource {

    override suspend fun getMemberLocations(tripcode: String): Response<MemberLocationsApiResponse> {
        return whereYouAtWebApi.getMemberLocations(tripcode)
    }

    override suspend fun isTripActive(tripcode: String): Response<BaseApiResponse> {
        return whereYouAtWebApi.isTripActive(tripcode)
    }

    override suspend fun createTrip(memberid: Long): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(CREATE_NEW_TRIP)
        apiRequest.arguments.add(Argument("memberid", memberid))
        return whereYouAtWebApi.performPostOperation(apiRequest)
    }

    override suspend fun uploadMyLocation(locUpdate: LocUpdate): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(UPDATE_TRIP)
        apiRequest.arguments.add(Argument("locupdate", locUpdate))
        return whereYouAtWebApi.performPostOperation(apiRequest)
    }

    override suspend fun removeUserFromTrip(memberid: Long): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(LEAVE_TRIP)
        apiRequest.arguments.add(Argument("memberid", memberid))
        return whereYouAtWebApi.performPostOperation(apiRequest)
    }

    override suspend fun validateApiServerRunning(): Response<BaseApiResponse> {
        return whereYouAtWebApi.isServerUp()
    }

    override suspend fun retrieveUpdateRateFromApi(): Response<BaseApiResponse> {
        return whereYouAtWebApi.getUpdateRate()
    }

    override suspend fun retrieveServerUrlFromApi(): Response<BaseApiResponse> {
        return whereYouAtWebApi.getServerUrl()
    }

    init { Log.i(TAG, "Initialized:RemoteDataSourceImpl") }
    companion object { private const val TAG = "FIMTOWN|RemoteDataSourceImpl" }
}