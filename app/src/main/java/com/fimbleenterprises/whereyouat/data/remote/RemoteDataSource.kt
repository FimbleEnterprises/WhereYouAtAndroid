package com.fimbleenterprises.whereyouat.data.remote

import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import retrofit2.Response

interface RemoteDataSource {
    suspend fun getMemberLocations(tripcode: String): Response<MemberLocationsApiResponse>
    suspend fun createTrip(memberid: Long): Response<BaseApiResponse>
}