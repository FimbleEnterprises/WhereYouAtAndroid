package com.fimbleenterprises.whereyouat.data

import android.util.Log
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

abstract class BaseApiCaller {

    // we'll use this function in all
    // repos to handle api errors.
    suspend fun <T> safeApiCall(apiToBeCalled: suspend () -> Response<T>): Resource<T> {

        // Returning api response
        // wrapped in Resource class
        return withContext(Dispatchers.IO) {
            try {

                // Here we are calling api lambda
                // function that will return response
                // wrapped in Retrofit's Response class
                val response: Response<T> = apiToBeCalled()

                if (response.isSuccessful) {
                    // In case of success response we
                    // are returning Resource.Success object
                    // by passing our data in it.
                    Resource.Success(data = response.body()!!)
                } else {
                    // Simply returning api's own failure message
                    Resource.Error(response.errorBody()?.toString() ?: "Something went wrong")
                }

            } catch (e: HttpException) {
                // Returning HttpException's message
                // wrapped in Resource.Error
                Resource.Error(e.message ?: "Something went wrong")
            } catch (e: IOException) {
                // Returning no internet message
                // wrapped in Resource.Error
                Log.e(TAG, "safeApiCall: ${e.message}")
                Resource.Error("Please check your network connection")
            } catch (e: Exception) {
                // Returning 'Something went wrong' in case
                // of unknown error wrapped in Resource.Error
                Resource.Error( e.message ?: "Something went wrong")
            }
        }
    }
init { Log.i(TAG, "Initialized:BaseApiCaller") }
companion object { private const val TAG = "FIMTOWN|BaseApiCaller" }
}
