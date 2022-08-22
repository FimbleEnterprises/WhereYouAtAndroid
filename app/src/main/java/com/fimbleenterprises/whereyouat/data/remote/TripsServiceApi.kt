package com.fimbleenterprises.whereyouat.data.remote

import com.fimbleenterprises.whereyouat.model.ApiRequest
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Constants
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface TripsServiceApi {

    @GET("api/Trips?")
    suspend fun getMemberLocations(
        @Query("tripcode") tripcode: String
    ): Response<MemberLocationsApiResponse>

    @POST("api/Trips")
    suspend fun  createTrip(
        @Body request: ApiRequest
    ): Response<BaseApiResponse>

    @POST("api/Trips")
    suspend fun uploadLocation(
        @Body request: ApiRequest
    ): Response<BaseApiResponse>
}
