package com.fimbleenterprises.whereyouat.data.remote

import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import retrofit2.Response

interface RemoteDataSource {
    suspend fun getMemberLocations(tripcode: String): Response<MemberLocationsApiResponse>
    suspend fun isTripActive(tripcode: String): Response<BaseApiResponse>
    suspend fun createTrip(memberid: Long): Response<BaseApiResponse>
    suspend fun uploadMyLocation(locUpdate: LocUpdate): Response<BaseApiResponse>
}