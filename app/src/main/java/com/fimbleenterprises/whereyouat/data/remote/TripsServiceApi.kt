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

    @GET(Constants.GET_TRIP_UPDATES_URL)
    suspend fun getMemberLocations(@Query("tripcode") tripcode: String): Response<MemberLocationsApiResponse>

    // as we are making a post request to post a data
    // so we are annotating it with post
    // and along with that we are passing a parameter as users
    @POST("api/Trips")
    suspend fun  createTrip(@Body request: ApiRequest): Response<BaseApiResponse>
}
