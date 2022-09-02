package com.fimbleenterprises.whereyouat.data.remote

import android.util.Log
import com.fimbleenterprises.whereyouat.model.*
import retrofit2.Response
import javax.inject.Singleton

@Singleton
class RemoteDataSourceImpl(private val whereYouAtWebApi: WhereYouAtWebApi):
    RemoteDataSource {

    override suspend fun getMemberLocations(tripcode: String): Response<MemberLocationsApiResponse> {
        return whereYouAtWebApi.getMemberLocations(tripcode)
    }

    override suspend fun isTripActive(tripcode: String): Response<BaseApiResponse> {
        return whereYouAtWebApi.isTripActive(tripcode)
    }

    override suspend fun createTrip(memberid: Long): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(ApiRequest.CREATE_NEW_TRIP)
        apiRequest.arguments.add(Argument("memberid", memberid))
        return whereYouAtWebApi.performPostOperation(apiRequest)
    }

    override suspend fun uploadMyLocation(locUpdate: LocUpdate): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(ApiRequest.UPDATE_TRIP)
        apiRequest.arguments.add(Argument("locupdate", locUpdate))
        return whereYouAtWebApi.performPostOperation(apiRequest)
    }

    init { Log.i(TAG, "Initialized:RemoteDataSourceImpl") }
    companion object { private const val TAG = "FIMTOWN|RemoteDataSourceImpl" }
}