package com.fimbleenterprises.whereyouat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import com.fimbleenterprises.whereyouat.utils.Constants
import com.fimbleenterprises.whereyouat.utils.Resource
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */

@AndroidEntryPoint
class TripUsersLocationManagementService : LifecycleService() {

    // -----------------------------------------------------------
    //                      FIREBASE ANALYTICS
    // -----------------------------------------------------------
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    // -----------------------------------------------------------
    //                      SERVICE MANAGEMENT
    // -----------------------------------------------------------
    // region SERVICE MANAGEMENT
    /*
 * Checks whether the bound activity has really gone away (foreground service with notification
 * created) or simply orientation change (no-op).
 */
    private var configurationChange = false

    var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private var tripcode: String? = null

    private lateinit var serviceStatus: ServiceStatus


    // endregion

    // -----------------------------------------------------------
    //                   MY LOCATION MANAGEMENT
    // -----------------------------------------------------------
    // region MY LOCATION MANAGEMENT
    // TODO: Step 1.1, Review variables (no changes).
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // EXPERIMENTAL - Trying to implement a progressbar when using isWaitForAccurateLocation = true
    // in the location request.  Prolly won't use it as waiting for the initial ACCURATE loc seems
    // frivolous.
    private var initialLocReceived = false

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    private var lastUploadedLocation: Long = 0
    // endregion

    // -----------------------------------------------------------
    //                  MEMBER LOCATION API CALLS
    // -----------------------------------------------------------
    // region API
    // Handler and runner for continuously requesting member locations.
    private var myHandler: Handler = Handler(Looper.myLooper()!!)
    private var runner: Runnable? = null
    // Set true when request is made and false when it returns.
    private var isWaitingOnMemberLocApi = false
    private var isWaitingOnMyLocApi = false
    private var forceUpdate = false
    // endregion

    // -----------------------------------------------------------
    //                       USE CASES
    // -----------------------------------------------------------
    // region USE CASES
    @Inject
    lateinit var uploadMyLocToApiUseCase: UploadMyLocToApiUseCase

    @Inject
    lateinit var saveMyLocToDbUseCase: SaveMyLocToDbUseCase

    @Inject
    lateinit var deleteMyLocFromDbUseCase: DeleteMyLocFromDbUseCase

    @Inject
    lateinit var getMemberLocsFromApiUseCase: GetMemberLocsFromApiUseCase

    @Inject
    lateinit var deleteAllMemberLocsFromDbUseCase: DeleteAllMemberLocsFromDbUseCase

    @Inject
    lateinit var saveMemberLocsToDbUseCase: SaveMemberLocsToDbUseCase

    @Inject
    lateinit var saveServiceStatusUseCase: SaveServiceStatusUseCase

    @Inject
    lateinit var getServiceStatusUseCase: GetServiceStatusUseCase
    // endregion

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics



        // Tripcode should never be null here.
        tripcode = AppPreferences.tripCode
        if (tripcode == null) {
            Log.w(TAG, "onCreate: TRIPCODE SHOULD NOT BE NULL HERE AT SERVICE!  STOPPING!!")
            CoroutineScope(IO).launch {
                saveServiceStatusUseCase.execute(
                    ServiceStatus(
                        isRunning = false,
                        isStarting = false
                    )
                )
                withContext(Main) {
                    stopSelf()
                }
            }
            return
        }

/*      -- MOVED TO ONSTART INSTEAD --
        val bundle = Bundle()
        bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
        firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_1, bundle)

        isRunning = true

        // Set the service state to starting
        CoroutineScope(IO).launch {

            serviceStatus = ServiceStatus(
                isStarting = true,
                isRunning = false
            )

            // Set the state to running as the initial state here in onCreate()
            saveServiceStatusUseCase.execute(
                serviceStatus
            )

        }*/

        // Clear all locations for all members
        CoroutineScope(IO).launch {
            deleteMyLocFromDbUseCase.execute()
            deleteAllMemberLocsFromDbUseCase.execute()
            Log.i(TAG, "-=ForegroundOnlyLocationService:onCreate Cleared all locations from db =-")
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        initLocationRequests()

    } // onCreate

    override fun onDestroy() {
        stopRequestingMemberLocations()
        // unsubscribeToLocationUpdates(false)
        CoroutineScope(IO).launch {
            saveServiceStatusUseCase.execute(
                ServiceStatus(
                    1,
                    isRunning = false,
                    isStarting = false
                )
            )
            AppPreferences.tripCode = null
        }
        super.onDestroy()

        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        // Start the runner to repeated request member locations.
        startRequestingMemberLocations()

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
            stopSelf()
        } else {
            val bundle = Bundle()
            bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
            firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_1, bundle)

            isRunning = true

            // Set the service state to starting
            CoroutineScope(IO).launch {

                serviceStatus = ServiceStatus(
                    isStarting = true,
                    isRunning = true
                )

                // Set the state to running as the initial state here in onCreate()
                saveServiceStatusUseCase.execute(
                    serviceStatus
                )

            }
        }

        CoroutineScope(IO).launch {
            // Retrieve and continue to monitor the db service status.
            getServiceStatusUseCase.executeFlow().collect {
                forceUpdate = it.forceUpdate

                if (forceUpdate) {
                    performMyLocUpload()
                }

                if (!it.isStopping) {
                    unsubscribeToLocationUpdates()
                    stopSelf()
                }

                when (it.locationRequestState) {
                    ServiceStatus.LOCATION_STATE_RESTART -> {
                        unsubscribeToLocationUpdates(true)
                        it.isRunning = serviceStatus.isRunning
                        it.isStarting = serviceStatus.isStarting
                        it.locationRequestState = ServiceStatus.LOCATION_STATE_RUNNING
                    }
                }

                serviceStatus = it
            }
        }

        // Tells the system not to recreate the service after it's been killed.
        return super.onStartCommand(intent, START_FLAG_REDELIVERY, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MapFragment (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        unsubscribeToLocationUpdates(true)
        configurationChange = false
        super.onRebind(intent)

        if (AppPreferences.tripCode == null) {
            Log.e(TAG, "-=onRebind: Tripcode should not be null! =-")
        }

    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(currentLocation)
            try {
                unsubscribeToLocationUpdates(true)
                startForeground(NOTIFICATION_ID, notification)
                serviceRunningInForeground = true
            } catch (exception:RuntimeException) {
                Log.e(TAG, "onUnbind: ${exception.localizedMessage}\n"
                    , exception)

            }
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    /**
     * Sets the parameters for location request frequency as well as housing the callback where we
     * do things based on the results of those requests.
     */
    private fun initLocationRequests() {
        // TODO: Step 1.2, Review the FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO: Step 1.3, Create a LocationRequest.
        locationRequest = LocationRequest.create().apply {

            // Set foreground or background settings

            // Default to FAST updates
            /*var scanInterval = 1L
            var fastestInterval = 1L
            var maxWaitTime = 1L*/
            var scanInterval = AppPreferences.scanInterval_MapVisible
            var fastestInterval = AppPreferences.fastestScanInterval_MapVisible
            var maxWaitTime = AppPreferences.maxWaitTime_MapVisible

            if (serviceRunningInForeground) {
                // The app is in the BACKGROUND (the verbiage is confusing) so we slow updates.
                scanInterval = AppPreferences.scanInterval_MapInBackground
                fastestInterval = AppPreferences.fastestScanInterval_MapInBackground
                maxWaitTime = AppPreferences.maxWaitTime_MapInBackground
            }

            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.

            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = TimeUnit.SECONDS.toMillis(scanInterval)

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = TimeUnit.SECONDS.toMillis(fastestInterval)

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.SECONDS.toMillis(maxWaitTime)

            // I don't think there is a good reason to wait for this.  I'll leave it commented so as
            // to keep it visible in case I change my mind.
            // isWaitForAccurateLocation = true

            // The distance delta required before a new location can be reported back to the listener.
            smallestDisplacement = 1f

            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // TODO: Step 1.4, Initialize the LocationCallback.
        /**
         * What we DO when we get a location.
         */
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {

                if (!this@TripUsersLocationManagementService::serviceStatus.isInitialized) {
                    val bundle = Bundle()
                    bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
                    firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_3, bundle)
                    return
                }

                if (!serviceStatus.isStarting && !serviceStatus.isRunning) {
                    val bundle = Bundle()
                    bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
                    firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_3, bundle)
                    return
                }

                super.onLocationResult(locationResult)

                initialLocReceived = true

                // Normally, you want to save a new location to a database. We are simplifying
                // things a bit and just saving it as a local variable, as we only need it again
                // if a Notification is created (when the user navigates away from app).
                currentLocation = locationResult.lastLocation

                // Notify our Activity that a new location was added. Again, if this was a
                // production app, the Activity would be listening for changes to a database
                // with new locations, but we are simplifying things a bit to focus on just
                // learning the location side of things.
                val intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
                intent.putExtra(EXTRA_LOCATION, currentLocation)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                // Updates notification content if this service is running as a foreground service.
                if (serviceRunningInForeground) {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        generateNotification(currentLocation))
                }

                //Do upload actions
                performMyLocUpload()

                requestMemberLocations()

            } // on Loc received
        } // loc callback
    }

    private fun okayToUploadLocation(): Boolean {
    return true
    /*val diff = System.currentTimeMillis() - lastUploadedLocation
        val thresh = TimeUnit.SECONDS.toMillis(MAX_WAIT_LOCATION_REQUEST)
        val okay = (diff) > thresh
        Log.v(TAG, " !!!!!!! -= okayToUploadLocation =- !!!!!!!")
        Log.v(TAG, "-=okayToUploadLocation: LastUploaded: $lastUploadedLocation, DIFF: $diff, thresh: $thresh, OkayToUpload: $okay =-")
        return okay*/
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        if (!this::fusedLocationProviderClient.isInitialized) {
            val bundle = Bundle()
            bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
            firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_2, bundle)
            return
        }

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, TripUsersLocationManagementService::class.java))

        try {
            // TODO: Step 1.5, Subscribe to location changes.
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper())

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }

    }

    private fun unsubscribeToLocationUpdates(resubscribe: Boolean = false) {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
            if (this::fusedLocationProviderClient.isInitialized) {
                val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Location Callback removed.")
                        if (resubscribe) {
                            initLocationRequests()
                            subscribeToLocationUpdates()
                        }/* else {
                            stopSelf()
                        }*/
                    } else {
                        Log.d(TAG, "Failed to remove Location Callback.")
                    }
                }
                SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            }
        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    /**
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun generateNotification(location: Location?): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = "Uploaded location!\n${location?.toText()}"
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).

        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT)

        // Adds NotificationChannel to system. Attempting to create an
        // existing notification channel with its original values performs
        // no operation, so it's safe to perform the below sequence.
        notificationManager.createNotificationChannel(notificationChannel)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, TripUsersLocationManagementService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.map_icon1)
            .setSilent(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPendingIntent)
            .addAction(
                R.drawable.ic_cancel,
                getString(R.string.leave_trip_notification_button),
                servicePendingIntent
            )
            .build()
    }

    /**
     * Stops the runner and nulls it out.
     */
    private fun stopRequestingMemberLocations() {
        if (runner != null) {
            myHandler.removeCallbacks(runner!!)
            myHandler.removeCallbacksAndMessages(null)
            runner = null
        }
    }

    /**
     * Starts a runner that repeatedly requests member locations.
     */
    private fun startRequestingMemberLocations() {

        // In case we somehow have a runner that's already instantiated we stop that shit dead.
        if (runner != null) {
            myHandler.removeCallbacks(runner!!)
            myHandler.removeCallbacksAndMessages(null)
        }

        // Get locations manually, once, so we don't have to wait for the initial delay to elapse.
        requestMemberLocations()

        // What runs on each execution/loop
        runner = Runnable {
            // Get trip member locations from the API
            requestMemberLocations()
            Log.d(TAG, "startRequestingMemberLocations: Scheduling requestMemberLocations request.")
            // Schedule it again for the future
            myHandler.postDelayed(runner!!, 15000)
        }

        // Schedule the first loop
        myHandler.postDelayed(runner!!, 15000)
    }

    private fun performMyLocUpload() = CoroutineScope(IO).launch {

        if (AppPreferences.tripCode == null) {
            //stopSelf()
            stopRequestingMemberLocations()
            unsubscribeToLocationUpdates(false)
            //cancel()
            return@launch
        }

        currentLocation?.let {
                val myLocation = MyLocation(0,
                    System.currentTimeMillis(),
                    45,
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
                // Save to local storage, cause why not.
                saveMyLocToDbUseCase.execute(myLocation)

                if (okayToUploadLocation() || forceUpdate) {
                    if (forceUpdate) {
                        withContext(IO) {
                            forceUpdate = false
                            Log.w(TAG, "onLocationResult: FORCE UPDATE REQUESTED")
                        }
                    }

                    // Upload my loc to API
                    myLocation.toLocUpdate(tripcode!!)?.let { locUpdate ->
                        uploadMyLocToApiUseCase.execute(locUpdate).collect {
                            when (it) {
                                is Resource.Success -> {
                                    lastUploadedLocation = System.currentTimeMillis()
                                    // It's nice to see our location change on the map after
                                    // uploading our loc so we request that now.
                                    requestMemberLocations()
                                    Log.i(TAG, "-=onLocationResult: Uploaded location was successful. =-")
                                }
                                is Resource.Loading -> {
                                    Log.i(TAG,
                                        "-=ForegroundOnlyLocationService:onLocationResult LOADING =-")
                                }
                                is Resource.Error -> {
                                    Log.w(TAG,
                                        "-=ForegroundOnlyLocationService:onLocationResult ERROR =-")
                                }
                            } // when
                        } // collect
                    } // LocUpdate is not null
                } // okayToUploadLocation
            } // currentLocation?.let {
        } // bg coroutine

    private fun requestMemberLocations() = CoroutineScope(IO).launch {

        if (tripcode == null) {
            return@launch
        }

        if (!isWaitingOnMemberLocApi) {
            // writeOnetimeMsg(getString(R.string.requesting_member_locations_from_api))
            isWaitingOnMemberLocApi = true
            try {
                getMemberLocsFromApiUseCase.execute(tripcode!!).collect { apiResponse ->
                    when (apiResponse) {
                        is Resource.Success -> {
                            isWaitingOnMemberLocApi = false
                            deleteAllMemberLocsFromDbUseCase.execute()
                            apiResponse.data?.locUpdates?.let {
                                saveMemberLocsToDbUseCase.executeMany(it)
                            }
                            // writeOnetimeMsg(getString(R.string.member_locations_received))
                        }
                        is Resource.Loading -> {
                            isWaitingOnMemberLocApi = true
                        }
                        is Resource.Error -> {
                            isWaitingOnMemberLocApi = false
                            // writeOnetimeMsg(getString(R.string.failed_to_get_member_locations))
                        }
                    }
                }
            } catch (exception: NullPointerException) {
                Log.e(TAG, "requestMemberLocations: ${exception.localizedMessage}", exception)
            }
        }/* else {
            stopRequestingMemberLocations()
            unsubscribeToLocationUpdates()
            stopSelf()
        }*/
        // Keeping this here as a reminder not to let a function do more than it should do!  This
        // method to request something from the API has no business modifying the service for any
        // reason!  This cost me nearly 12 hours of development time trying to figure out why the
        // service was silently dying.
    }

    /*private fun writeOnetimeMsg(msg: String) {
        if (this@TripUsersLocationManagementService::serviceStatus.isInitialized) {
          CoroutineScope(IO).launch {
                saveServiceStatusUseCase.execute(
                    ServiceStatus(
                        isStarting =  serviceStatus.isStarting,
                        isRunning = serviceStatus.isRunning,
                        forceUpdate = serviceStatus.forceUpdate,
                        oneTimeMessage = msg
                    )
                )
            }*//*
        } else {
            Log.i(TAG, "-=writeOnetimeMsg:Somthin gon wrong =-")
        }
    }*/

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: TripUsersLocationManagementService
            get() = this@TripUsersLocationManagementService
    }

    companion object {
        private const val TAG = "FIMTOWN|ForegroundOnlyLocationService"

        private const val LOCATION_REQUEST_INTERVAL = 10L
        private const val FASTEST_INTERVAL = 1L
        private const val MAX_WAIT_LOCATION_REQUEST = 20L
        private const val UPLOAD_LOCATION_INTERVAL = 30
        private const val MINIMUM_DISTANCE_FOR_UPLOAD = 10

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"

        var isRunning = false
    }

    /**
     * Returns the `location` object as a human readable string.
     */
    private fun Location?.toText(): String {
        return if (this != null) {
            "($latitude, $longitude)"
        } else {
            "Unknown location"
        }
    }

}

/**
 * Returns the `location` object as a human readable string.
 */
fun Location?.toText(): String {
    return if (this != null) {
        "($latitude, $longitude)"
    } else {
        "Unknown location"
    }
}

/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {

    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }
}