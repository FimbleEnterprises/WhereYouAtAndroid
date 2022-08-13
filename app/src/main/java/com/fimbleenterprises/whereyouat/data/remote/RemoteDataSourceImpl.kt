package com.fimbleenterprises.whereyouat.data.remote

import com.fimbleenterprises.whereyouat.model.ApiRequest
import com.fimbleenterprises.whereyouat.model.Argument
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.google.gson.Gson
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSourceImpl @Inject constructor(private val tripsServiceApi: TripsServiceApi):
    RemoteDataSource {

    override suspend fun getMemberLocations(tripcode: String): Response<MemberLocationsApiResponse> {
        return tripsServiceApi.getMemberLocations(tripcode)
    }

    override suspend fun createTrip(memberid: Long): Response<BaseApiResponse> {
        val apiRequest = ApiRequest(ApiRequest.CREATE_NEW_TRIP)
        apiRequest.arguments.add(Argument("memberid", memberid))
        return tripsServiceApi.createTrip(apiRequest)
    }


}