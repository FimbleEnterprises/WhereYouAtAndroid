package com.fimbleenterprises.whereyouat.data.remote

import com.fimbleenterprises.whereyouat.model.ApiRequest
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Constants
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


interface WhereYouAtWebApi {

    @GET("api/trips/entries/{tripcode}")
    suspend fun getMemberLocations(@Path("tripcode") tripcode: String): Response<MemberLocationsApiResponse>

    @GET("api/trips/isactive/{tripcode}")
    suspend fun isTripActive(@Path("tripcode") tripcode: String): Response<BaseApiResponse>

    @POST("api/trips")
    suspend fun  performPostOperation(@Body request: ApiRequest): Response<BaseApiResponse>
}
