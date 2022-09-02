package com.fimbleenterprises.whereyouat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.fimbleenterprises.whereyouat.data.usecases.DeleteServiceStatusUseCase
import com.fimbleenterprises.whereyouat.data.usecases.SaveServiceStatusUseCase
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class WhereYouAt : Application() {

    @Inject
    lateinit var deleteServiceStatusUseCase: DeleteServiceStatusUseCase

    @Inject
    lateinit var saveServiceStatusUseCase: SaveServiceStatusUseCase

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        FirebaseApp.initializeApp(this)

        CoroutineScope(IO).launch {
            saveServiceStatusUseCase.execute(ServiceStatus(1, false, System.currentTimeMillis(), false))
        }

    }

    init {
    }

    @Singleton
    object AppPreferences {
        private const val NAME = "ALL_PREFS"
        private const val MODE = Context.MODE_PRIVATE
        private lateinit var prefs: SharedPreferences

        // list of preferences
        private const val PREF_TRIPCODE = "PREF_TRIPCODE"
        private const val PREF_LAST_TRIPCODE = "PREF_LAST_TRIPCODE"
        private const val PREF_MEMBERID = "PREF_MEMBERID"
        private const val PREF_MEMBERNAME = "PREF_MEMBERNAME"
        private const val PREF_BG_PERMISSION_PESTER = "PREF_PERMISSION_PESTER"
        private const val PREF_MY_LOC_INTERVAL = "PREF_MY_LOC_INTERVAL"
        private const val PREF_MY_LOC_FASTEST_INTERVAL = "PREF_MY_LOC_FASTEST_INTERVAL"
        private const val PREF_MAX_WAIT_TIME = "PREF_MAX_WAIT_TIME"
        private const val PREF_SLIDER_POSITION_FOREGROUND = "PREF_SLIDER_POSITION_FOREGROUND"
        private const val PREF_SLIDER_POSITION_BACKGROUND = "PREF_SLIDER_POSITION_BACKGROUND"

        /**
         * The value used to set the slider on the map frag.
         */
        var tripCode : String?
            get() = prefs.getString(PREF_TRIPCODE, null)
            set(value) {
                prefs.edit().putString(PREF_TRIPCODE, value).apply()
            }

        /**
         * The value used to set the slider on the map frag.
         */
        var lastTripCode : String?
            get() = prefs.getString(PREF_LAST_TRIPCODE, null)
            set(value) {
                prefs.edit().putString(PREF_LAST_TRIPCODE, value).apply()
            }

        /**
         * The value used to set the slider on the map frag.
         */
        var sliderPositionForeground : Int
            get() = prefs.getInt(PREF_SLIDER_POSITION_FOREGROUND, 0)
            set(value) {
                prefs.edit().putInt(PREF_SLIDER_POSITION_FOREGROUND, value).apply()
            }

        /**
         * The value used to set the slider on the map frag.
         */
        var sliderPositionBackground : Int
            get() = prefs.getInt(PREF_SLIDER_POSITION_BACKGROUND, 0)
            set(value) {
                prefs.edit().putInt(PREF_SLIDER_POSITION_BACKGROUND, value).apply()
            }

        /**
        Set the desired interval for location updates, in milliseconds.
        The Fused Location Provider will attempt to obtain location updates for the client at this
        interval, so it has a direct influence on the amount of power used by your application.
        Choose your interval wisely.  You may receive updates faster or slower than the desired
        interval, or not at all (if location is off for example). The fastest rate that that you
        will receive updates can be controlled via setFastestInterval(long). By default the fastest
        rate is 6x the interval.  Applications with only the coarse location permission may have
        their interval throttled.
         */
        var scanInterval_MapVisible : Long
            get() = prefs.getLong(PREF_MY_LOC_INTERVAL, 1L)
            set(value) {
                prefs.edit().putLong(PREF_MY_LOC_INTERVAL, value).apply()
            }

        /**
        Set the fastest interval in milliseconds for location updates to an explicit value.
        This controls the absolute fastest rate at which a client can receive location updates.
        Clients can receive locations faster than their desired interval, and this parameter controls
        the absolute fastest allowed deliver rate. This allows clients to passively acquire locations
        at a rate faster than their interval. If you don't call this method, a fastest interval
        will be selected for you. It will be a value faster than your active interval (setInterval(long)).
        An interval of 0 is allowed, but not recommended as this may cause a client to use more power than desired.
        */
        var fastestScanInterval_MapVisible : Long
            get() = prefs.getLong(PREF_MY_LOC_FASTEST_INTERVAL, 1L)
            set(value) {
                prefs.edit().putLong(PREF_MY_LOC_FASTEST_INTERVAL, value).apply()
            }

        /**
         * Sets the maximum wait time in milliseconds for location updates. If you pass a value at
         * least 2x larger than the interval specified via setInterval(long) , then location
         * delivery may be delayed by up to the maximum wait time so that batches of locations
         * can be delivered at once. Locations are still calculated at the rate set by setInterval(long),
         * but can be delivered in batches in order to consume less battery. If clients do not
         * require immediate location delivery they should consider setting this value as large as
         * reasonably possible.
         */
        var maxWaitTime_MapVisible : Long
            get() = prefs.getLong(PREF_MAX_WAIT_TIME, 1L)
            set(value) {
                prefs.edit().putLong(PREF_MAX_WAIT_TIME, value).apply()
            }

        /**
         * Background version of the scan interval
         */
        var scanInterval_MapInBackground : Long
            get() = prefs.getLong(PREF_MY_LOC_INTERVAL, 60L)
            set(value) {
                prefs.edit().putLong(PREF_MY_LOC_INTERVAL, value).apply()
            }

        /**
         * Background version
         */
        var fastestScanInterval_MapInBackground : Long
            get() = prefs.getLong(PREF_MY_LOC_FASTEST_INTERVAL, 30L)
            set(value) {
                prefs.edit().putLong(PREF_MY_LOC_FASTEST_INTERVAL, value).apply()
            }

        /**
         * Background version
         */
        var maxWaitTime_MapInBackground : Long
            get() = prefs.getLong(PREF_MAX_WAIT_TIME, 120L)
            set(value) {
                prefs.edit().putLong(PREF_MAX_WAIT_TIME, value).apply()
            }

        var membername : String
            get() = prefs.getString(PREF_MEMBERNAME, "Me").orEmpty()
            set(value) {
                prefs.edit().putString(PREF_MEMBERNAME, value).apply()
            }

        var memberid : Long
            get() = prefs.getLong(PREF_MEMBERID, 0)
            set(value) {
                prefs.edit().putLong(PREF_MEMBERID, value).apply()
            }

        var nagUserAboutBgPermission : Boolean
            get() = prefs.getBoolean(PREF_BG_PERMISSION_PESTER, true)
            set(value) {
                prefs.edit().putBoolean(PREF_BG_PERMISSION_PESTER, value).apply()
            }

        fun init(context: Context) {
            prefs = context.getSharedPreferences(NAME, MODE)
        }

    }

}