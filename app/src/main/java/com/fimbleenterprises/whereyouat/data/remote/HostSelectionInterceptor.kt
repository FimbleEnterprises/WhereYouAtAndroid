package com.fimbleenterprises.whereyouat.data.remote

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.utils.Constants
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import com.fimbleenterprises.whereyouat.di.NetworkModule
import retrofit2.Retrofit

/**
 * This is a custom interceptor for use when building a [Retrofit] instance.  It was created so we
 * can use a custom base url for our web requests.  Since Retrofit is instantiated with dagger/hilt
 * I didn't see a good way to gain access to shared prefs so early in the app lifecycle without
 * injecting the app context into the [NetworkModule.provideRetrofit] module.  So this class was
 * created and...wait for it... created by dagger/hilt having the app context injected into the
 * [NetworkModule.provideHostSelectionInterceptor] module.  SO!  Since I could just access shared
 * prefs when instantiating the Retrofit instance, I don't really need this class.
 *
 * The only benefits to using it are:
 *
 * 1. It's cool to see inside an [Interceptor].
 * 2. This way means that if the base url value changes, its effects are felt immediately as opposed
 *    to having to wait until the next time Retrofit is instantiated (on app start).
 */
class HostSelectionInterceptor(
    private val app: Application,
    private val doLogging: Boolean = false
): Interceptor {

    private lateinit var sharedPreferences: SharedPreferences

    private fun toLog(msg: String) {
        Log.d(TAG, msg)
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        // Get a handle on shared prefs and retrieve the base url specifically.
        sharedPreferences = app.getSharedPreferences(
            AppPreferences.NAME,
            AppPreferences.MODE
        )
        val prefHost: String = sharedPreferences.getString(
            AppPreferences.PREF_BASE_URL,
            Constants.DEFAULT_BASE_URL
        )!!

        // Extract the request form the caller
        var request = chain.request()

        // Convert our url string from prefs to a proper HttpUrl
        val tempUrl = prefHost.toHttpUrlOrNull()!!

        // Replace the scheme/host properties in the request with our shared pref's host/scheme.
        request = request.newBuilder()
            .url(
                request.url.newBuilder()
                    .scheme(tempUrl.scheme)
                    .host(tempUrl.host)
                    .build()
            )
            .build()

        val response = chain.proceed(request)

        // Add logging if requested
        if (doLogging) {
            toLog("Method: ${request.method}, Url: ${request.url} Body: ${request.body?.contentType()}")
            toLog("Response: ${response.message}")
        }

        return response
    }

    init {
        Log.i(TAG, "Initialized:HostSelectionInterceptor")

    }
    companion object { private const val TAG = "FIMTOWN|HostSelectionInterceptor" }
}