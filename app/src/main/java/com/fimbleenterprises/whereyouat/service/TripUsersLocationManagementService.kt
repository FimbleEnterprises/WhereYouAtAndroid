package com.fimbleenterprises.whereyouat.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.*
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.MainActivity.Companion.ACTION_SHARE_CODE_FROM_NOTIFICATION
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.fimbleenterprises.whereyouat.utils.Constants
import com.fimbleenterprises.whereyouat.utils.Resource
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first
import java.lang.Runnable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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

    // Handler and runner for continuously requesting member locations.
    private var locationClientMonitorHandler: Handler = Handler(Looper.myLooper()!!)
    private var locationClientMonitorRunner: Runnable? = null

    // Handler and runner for continuously monitoring the desired server URL.
    private var serverUrlMonitorHandler: Handler = Handler(Looper.myLooper()!!)
    private var serverUrlMonitorRunner: Runnable? = null

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

    var rigorousUpdates = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private var tripcode: String? = null

    private lateinit var serviceState: ServiceState
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

    private var lastLocReceivedAt: Long = 0L

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // EXPERIMENTAL - Trying to implement a progressbar when using isWaitForAccurateLocation = true
    // in the location request.  Prolly won't use it as waiting for the initial ACCURATE loc seems
    // frivolous.
    private var initialLocReceived = false

    // Used to keep a constant tally of our location in our proprietary format.
    private var lastKnownLocation: MyLocation? = null

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    // private var lastKnownLocation: Location? = null

    /**
     * Time in millis that my location was uploaded to the API
     */
    private var lastUploadedLocation: Long = 0

    private var lastRequestedLocations: Long = 0

    // Handler and runner for continuously requesting member locations.
    private var myLocationUploadHandler: Handler = Handler(Looper.myLooper()!!)
    private var myLocationUploadRunner: Runnable? = null

    // endregion

    // -----------------------------------------------------------
    //                  MEMBER LOCATION API CALLS
    // -----------------------------------------------------------
    // region API
    // Handler and runner for continuously requesting member locations.
    private var memberLocationRequestHandler: Handler = Handler(Looper.myLooper()!!)
    private var memberLocationRequestRunner: Runnable? = null
    /** Set true when request is made and false when it returns.*/
    private var isWaitingOnMemberLocApi = false
    /** Set true when request is made and false when it returns.*/
    private var isWaitingOnMyLocApi = false
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
    lateinit var serviceStateUseCases: ServiceStateUseCases

    @Inject
    lateinit var removeMemberFromTripInApiUseCase: RemoveMemberFromTripInApiUseCase

    @Inject
    lateinit var getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase

    @Inject
    lateinit var leaveTripWithApiUseCase: LeaveTripWithApiUseCase

    @Inject
    lateinit var getWaypointPositionUseCase: GetWaypointPositionUseCase

    @Inject
    lateinit var getServerUrlFromApiUseCase: GetServerUrlFromApiUseCase

    // endregion

    init {
        Log.i(TAG, "Initialized:TripUsersLocationManagementService")
    }

    /**
     * The first thing to be called.  Called before onStart().
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics

        // Tripcode should never be null here.
        tripcode = AppPreferences.tripCode
        if (tripcode == null) {
            stopSelf()
            return
        }

        // Start watching the DB (service state) - interested in STOPPING so we can kill the service
        startObservingServiceStatus()

        // Clear all locations for all members
        CoroutineScope(IO).launch {
            deleteMyLocFromDbUseCase.execute()
            deleteAllMemberLocsFromDbUseCase.execute()
            Log.i(TAG, "-=ForegroundOnlyLocationService:onCreate Cleared all locations from db =-")
        }

        // How we manage notifications
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build the location client
        initLocationRequests()

        AppPreferences.waypointPosition = null

    } // onCreate

    /**
     * Retrieve and continue to monitor the db service status.  If we detect STOPPING then we
     * start the shutdown process.  This is how the outsiders shutdown the service, by changing
     * the ServiceState in the database and expecting us to observe it.
     */
    private fun startObservingServiceStatus() = CoroutineScope(IO).launch {
        // -----------------------------------------------------------
        //   Retrieve and continue to monitor the db service status.
        // -----------------------------------------------------------
        serviceStateUseCases.getServiceStateAsFlow().cancellable().collect {
            serviceState = it
            when (it.state) {
                ServiceState.SERVICE_STATE_STOPPING -> {
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:STOPPING =-")
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:KILLING SERVICE BECAUSE STOPPING STATE =-")
                    unsubscribeToLocationUpdates()
                    stopSelf()
                }
            }
        } // Collector
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancellationRequest =
            intent?.getBooleanExtra(ACTION_SHARE_CODE_FROM_NOTIFICATION, false)

        val fastUpdatesRequest =
            intent?.getBooleanExtra(RIGOROUS_UPDATES_INTENT_EXTRA, false)

        // See if we are here because the user is tryna actually stop the service.
        if (cancellationRequest == true) {
            unsubscribeToLocationUpdates()
            stopSelf()
        } else if (intent?.hasExtra(RIGOROUS_UPDATES_INTENT_EXTRA) == true) {
            rigorousUpdates = fastUpdatesRequest!!
        } else { // start command sent while NOT running.  We start anew!
            if (!isRunning) {
                val bundle = Bundle()
                bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
                firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_1, bundle)

                isRunning = true

                if (this::serviceState.isInitialized) {
                    // Set the service state to running
                    CoroutineScope(IO).launch {
                        AppPreferences.lastTripCode = tripcode
                        serviceState.state = ServiceState.SERVICE_STATE_RUNNING
                        // Set the state to running as the initial state here in onCreate()
                        serviceStateUseCases.setServiceRunning()
                    }

                    // Start the runner to repeated request member locations.
                    startRequestingMemberLocations()

                    // Start the runner for uploading our location
                    startMyLocationUploadRunner()

                    // Start the runner for ensuring we are using the correct server url
                    startServerUrlMonitorRunner()

                    postServiceStatus()

                    monitorLastLocationClientLocationReceived()
                }
            }
        }

        // Tells the system not to recreate the service after it's been killed.
        return super.onStartCommand(intent, START_FLAG_REDELIVERY, startId)
    }

    /**
     * The last thing this service will ever do.
     */
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopRequestingMemberLocations()
        stopMyLocationUploadRunner()
        stopServerUrlMonitorRunner()
        unsubscribeToLocationUpdates(false)

        CoroutineScope(IO).launch {
            if (!requestLeaveFromApi()) {
                Log.i(TAG, "-=onDestroy:FAILED TO LEAVE TRIP ON SERVER =-")
            } else {
                Log.i(TAG, "-=onDestroy:SUCCESSFULLY LEFT TRIP! =-")
            }
            deleteAllMemberLocsFromDbUseCase.execute()
            serviceStateUseCases.setServiceStopped()
            AppPreferences.tripCode = null
        }
        stopMonitoringLocationClient()
    }

    /**
     * Sends a post request to the API asking it to remove us from the party.
     */
    private suspend fun requestLeaveFromApi(): Boolean {

        return suspendCoroutine { cont ->
            CoroutineScope(IO).launch {
                removeMemberFromTripInApiUseCase.execute(AppPreferences.memberid).collect {
                    when (it) {
                        is Resource.Success -> {
                            if (it.data?.wasSuccessful == true) {
                                Log.i(TAG, " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                Log.i(TAG,
                                    " USER REMOVED FROM TRIP IN API (RESULT: ${it.data.wasSuccessful}) ")
                                Log.i(TAG, " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                cont.resume(true)
                            } else {
                                cont.resume(false)
                            }
                        }
                        is Resource.Error -> {
                            cont.resume(false)
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    // MapFragment (client) comes into foreground and binds to service, so the service can
    // become a background services.
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind()")
        Log.i(TAG3, "-=onBind: UPLOADING MY LOC =-")
        uploadMyLocation()
        if (isRunning) {
            startMyLocationUploadRunner()
            startRequestingMemberLocations()
            startServerUrlMonitorRunner()
        }
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    // MapFragment (client) returns to the foreground and rebinds to service, so the service
    // can become a background services.
    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")
        if (isRunning) {
            uploadMyLocation()
            startMyLocationUploadRunner()
            startRequestingMemberLocations()
            startServerUrlMonitorRunner()
        }
        stopForeground(true)
        serviceRunningInForeground = false
        unsubscribeToLocationUpdates(true)
        configurationChange = false
        super.onRebind(intent)

        if (AppPreferences.tripCode == null) {
            Log.e(TAG, "-=onRebind: Tripcode should not be null! =-")
        }

    }

    // MainActivity (client) leaves foreground, so service needs to become a foreground service
    // to maintain the 'while-in-use' label.
    // NOTE: If this method is called due to a configuration change in MainActivity,
    // we do nothing.
    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        /*if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            stopMyLocationUploadRunner()
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(lastKnownLocation)
            try {
                unsubscribeToLocationUpdates(true)
                startForeground(NOTIFICATION_ID, notification)
                serviceRunningInForeground = true
            } catch (exception:RuntimeException) {
                Log.e(TAG, "onUnbind: ${exception.localizedMessage}\n"
                    , exception)
            }
        }*/
        stopMyLocationUploadRunner()
        stopRequestingMemberLocations()
        stopServerUrlMonitorRunner()
        Log.d(TAG, "Start foreground service")

        // TODO I DON'T FUCKING KNOW, BRO
        // This feels SO wrong but I have gotten below and had serviceState in fact be uninitialized
        if (!this::serviceState.isInitialized) {
            notificationManager.cancelAll()
            stopSelf()
        }

        if (serviceState.state == ServiceState.SERVICE_STATE_RUNNING) {
            startMyLocationUploadRunner()
            startRequestingMemberLocations()
            startServerUrlMonitorRunner()
            val notification = generateNotification()
            try {
                unsubscribeToLocationUpdates(true)
                startForeground(NOTIFICATION_ID, notification)
                serviceRunningInForeground = true
            } catch (exception:RuntimeException) {
                Log.e(TAG, "onUnbind: ${exception.localizedMessage}\n"
                    , exception)
            }
            Log.i(TAG3, "-=onUnbind: UPLOADING MY LOC =-")
            uploadMyLocation()
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    /**
     * Called when the device is rotated.  Not currently implemented but could be pretty important
     * if we allow the device to be rotated (it's disabled in manifest presently) and will play
     * hell with onBind/unBind etc.
     */
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

            // Default to FAST updates when map is visible in the foreground.
            var scanInterval = LOCATION_REQUEST_INTERVAL_RIGOROUS
            var fastestInterval = LOCATION_REQUEST_INTERVAL_RIGOROUS
            var maxWaitTime = LOCATION_REQUEST_INTERVAL_RIGOROUS

            // Use alternate intervals if in background
            if (serviceRunningInForeground) {
                // The app is in the BACKGROUND (the verbiage is confusing) so we slow updates.
                scanInterval = 60L
                fastestInterval = 30L
                maxWaitTime = 120L
            }

            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.

            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            this.interval = TimeUnit.SECONDS.toMillis(scanInterval)

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            this.fastestInterval = TimeUnit.SECONDS.toMillis(fastestInterval)

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            this.maxWaitTime = TimeUnit.SECONDS.toMillis(maxWaitTime)

            // I don't think there is a good reason to wait for this.  I'll leave it commented so as
            // to keep it visible in case I change my mind.
            // isWaitForAccurateLocation = true

            // The distance delta required before a new location can be reported back to the listener.
            this.smallestDisplacement = 1f

            this.priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // TODO: Step 1.4, Initialize the LocationCallback.
        /**
         * What we DO when we get a location.
         */
        locationCallback = object : LocationCallback() {

            @SuppressLint("MissingPermission")
            override fun onLocationResult(locationResult: LocationResult) {

                if (!isRunning) {
                    return
                }

                lastLocReceivedAt = System.currentTimeMillis()

                if (!this@TripUsersLocationManagementService::serviceState.isInitialized) {
                    val bundle = Bundle()
                    bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
                    firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_3, bundle)
                    return
                }

                if (serviceState.state == ServiceState.SERVICE_STATE_STOPPING) {
                    val bundle = Bundle()
                    bundle.putString("TRIPCODE", AppPreferences.tripCode ?: "")
                    firebaseAnalytics.logEvent(Constants.FIREBASE_EVENT_3, bundle)
                    return
                }

                super.onLocationResult(locationResult)

                initialLocReceived = true

                // Update the mutable live data containing the Location object as returned by this callback.
                _location.value = locationResult.lastLocation

                // Update the lastKnownMyLocation live data for outside observers.
                val lastKnownLoc = locationResult.lastLocation
                lastKnownLoc?.let {
                    var isBg = 0
                    if (serviceRunningInForeground) {
                        isBg = 1
                    }

                    lastKnownLocation = MyLocation(
                        rowid = 0,
                        createdon = System.currentTimeMillis(),
                        elevation = locationResult.lastLocation?.altitude,
                        lat = it.latitude,
                        lon = it.longitude,
                        speed = it.speed,
                        bearing = it.bearing,
                        accuracy = it.accuracy,
                        isBg = isBg
                    )
                }

                // Save our location to the database.
                Log.d(TAG3, "-=onLocationResult:Location change detected - saving to local DB =-")
                CoroutineScope(IO).launch {
                    lastKnownLocation.let { saveMyLocToDbUseCase.execute(it!!) }
                }

                // Updates notification content if this service is running as a foreground service.
                if (serviceRunningInForeground) {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        generateNotification()
                    )
                }

                if (rigorousUpdates) {
                    //Do upload actions
                    uploadMyLocation()
                    requestMemberLocations()
                }

            } // on Loc received
        } // loc callback


    }

    /**
     * Spam protection, probably not needed.
     */
    private fun okayToUploadLocation(): Boolean {
        val lastUpload = System.currentTimeMillis() - lastUploadedLocation
        val lastLocal = System.currentTimeMillis() - lastLocReceivedAt
        val thresh = 500
        Log.i(TAG, "-=okayToUploadLocation:lastUploaded: ${lastUpload / 1000} lastLocal: ${lastLocal / 1000} =-")
        return (lastUpload > thresh)
    }

    /**
     * Spam protection, probably not needed.
     */
    private fun okayToRequestLocations(): Boolean {
        val diff = System.currentTimeMillis() - lastRequestedLocations
        val thresh = 500
        return (diff) > thresh
    }

    /**
     * Starts everything!!  Called from outside.
     */
    fun startWhereYouAtService() {
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

    /**
     * Releases the location client from receiving location results.  Results will be received
     * in the callback where the client was created and configured.
     */
    private fun unsubscribeToLocationUpdates(resubscribe: Boolean = false) {
        try {
            // Unsubscribe to location changes.
            if (this::fusedLocationProviderClient.isInitialized) {
                val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Location Callback removed.")
                        if (resubscribe) {
                            initLocationRequests()
                            startWhereYouAtService()
                        }
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
    private fun generateNotification(): Notification {

        // 0. Get data for title/main text
        val titleText = getString(
            R.string.notif_title_text
        )
        val mainNotificationText = "Code: ${AppPreferences.tripCode}"

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
        val launchActivityIntent = Intent(applicationContext, MainActivity::class.java)
        val shareIntent = Intent(applicationContext, MainActivity::class.java)
        shareIntent.action = ACTION_SHARE_CODE_FROM_NOTIFICATION

        val shareCodePendingIntent = PendingIntent.getActivity(applicationContext, 0, shareIntent,PendingIntent.FLAG_IMMUTABLE or  PendingIntent.FLAG_UPDATE_CURRENT)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        // Changed setOngoing to false as there is a bug that will cause the service/frag binding
        // to get screwed up and the notification to remain despite not being in a trip.  This is
        // a stop-gap that at least allows the user to swipe away the notification.
        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.map_icon1)
            .setSilent(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activityPendingIntent)
            .addAction(
                R.drawable.share_icon_black_128,
                getString(R.string.share_trip_notification_button),
                shareCodePendingIntent
            )
            .build()
    }

    /**
     * Starts a runner that tracks the last time the location client detected a location change.
     * Useful for debugging.
     */
    private fun monitorLastLocationClientLocationReceived() {

        if (locationClientMonitorRunner != null) {
            locationClientMonitorHandler.removeCallbacks(locationClientMonitorRunner!!)
            locationClientMonitorHandler.removeCallbacksAndMessages(null)
        }

        locationClientMonitorRunner = Runnable {
            // What runs each time
            val seconds = (System.currentTimeMillis() - lastLocReceivedAt) / 1000
            Log.i(TAG3, "-= Last fusedLocationProviderClient location was $seconds seconds ago. =-")
            locationClientMonitorHandler.postDelayed(locationClientMonitorRunner!!, 1000)
        }

        // Starts it up initially
        locationClientMonitorHandler.postDelayed(locationClientMonitorRunner!!, 0)
    }

    /**
     * Stops the runner that detects the last fusedlocationprovider location (used for debugging)
     */
    private fun stopMonitoringLocationClient() {
        if (locationClientMonitorRunner != null) {
            try {
                locationClientMonitorHandler.removeCallbacks(locationClientMonitorRunner!!)
                locationClientMonitorHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Starts a runner that repeatedly uploads the last known location.  It does not request
     * a new location - it just uploads the last known location from the client.
     */
    private fun startMyLocationUploadRunner() {

        // Null check etc.
        if (myLocationUploadRunner != null) {
            try {
                myLocationUploadHandler.removeCallbacks(myLocationUploadRunner!!)
                myLocationUploadHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Record our location now before we start the runner
        getAndUpdateOurLocationOneTime()

        // What runs each time
        myLocationUploadRunner = Runnable {

            val interval = if (serviceRunningInForeground) {
                TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_BACKGROUND)
            } else {
                TimeUnit.SECONDS.toMillis(AppPreferences.apiRequestInterval)
            }

            uploadMyLocation()

            myLocationUploadHandler.postDelayed(myLocationUploadRunner!!, interval)
        }

        // Starts it up initially
        myLocationUploadHandler.postDelayed(myLocationUploadRunner!!, 0)
    }

    /**
     * Starts a runner that repeatedly checks the desired server url.  At least in testing this url
     * is in flux and if this ever got popular may be necessary to switch urls too.
     */
    private fun startServerUrlMonitorRunner() {

        // Null check etc.
        if (serverUrlMonitorRunner != null) {
            try {
                serverUrlMonitorHandler.removeCallbacks(serverUrlMonitorRunner!!)
                serverUrlMonitorHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // What runs each time
        serverUrlMonitorRunner = Runnable {
            Log.d(TAG, "startServerUrlMonitorRunner: Checking server url...")
            CoroutineScope(IO).launch {
                AppPreferences.baseUrl = getServerUrlFromApiUseCase.execute()
            }

            serverUrlMonitorHandler.postDelayed(
                serverUrlMonitorRunner!!, TimeUnit.SECONDS.toMillis(BASE_URL_CHECK_INTERVAL)
            )
        }

        // Starts it up initially
        serverUrlMonitorHandler.postDelayed(serverUrlMonitorRunner!!, 0)
    }

    private fun stopServerUrlMonitorRunner() {
        if (serverUrlMonitorRunner != null) {
            try {
                serverUrlMonitorHandler.removeCallbacks(serverUrlMonitorRunner!!)
                serverUrlMonitorHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Starts a runner that repeatedly requests member locations.
     */
    private fun startRequestingMemberLocations() {

        // In case we somehow have a runner that's already instantiated we stop that shit dead.
        if (memberLocationRequestRunner != null) {
            memberLocationRequestHandler.removeCallbacks(memberLocationRequestRunner!!)
            memberLocationRequestHandler.removeCallbacksAndMessages(null)
        }

        // Get locations manually, once, so we don't have to wait for the initial delay to elapse.
        requestMemberLocations()

        // What runs on each execution/loop
        memberLocationRequestRunner = Runnable {

            val interval = if (serviceRunningInForeground) {
                TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_BACKGROUND)
            } else {
                TimeUnit.SECONDS.toMillis(AppPreferences.apiRequestInterval)
            }

            // Get trip member locations from the API
            requestMemberLocations()

            // Schedule it again for the future
            memberLocationRequestHandler.postDelayed(memberLocationRequestRunner!!, interval)
        }

        // Schedule the first loop
        memberLocationRequestHandler.postDelayed(memberLocationRequestRunner!!, 0)
    }

    /**
     * Stops the runner and nulls it out.
     */
    private fun stopMyLocationUploadRunner() {
        if (myLocationUploadRunner != null) {
            try {
                myLocationUploadHandler.removeCallbacks(myLocationUploadRunner!!)
                myLocationUploadHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stops the runner and nulls it out.
     */
    private fun stopRequestingMemberLocations() {
        if (memberLocationRequestRunner != null) {
            memberLocationRequestHandler.removeCallbacks(memberLocationRequestRunner!!)
            memberLocationRequestHandler.removeCallbacksAndMessages(null)
            memberLocationRequestRunner = null
        }
    }

    /**
     * Using the fusedlocationprovider it calls on the OS to get the last known location and
     * uploads it one time.
     */
    private fun getAndUpdateOurLocationOneTime() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // No permissions
            return
        }

        // Get and upload an initial location from the fused location provider before we start the runner
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                // Update the lastKnownMyLocation live data for outside observers.
                location?.let {
                    var isBg = 0
                    if (serviceRunningInForeground) {
                        isBg = 1
                    }

                    lastKnownLocation = MyLocation(
                        rowid = 0,
                        createdon = System.currentTimeMillis(),
                        elevation = location.altitude,
                        lat = it.latitude,
                        lon = it.longitude,
                        speed = it.speed,
                        bearing = it.bearing,
                        accuracy = it.accuracy,
                        isBg = isBg
                    )
                    Log.d(TAG3, "-= UPLOAD MY LOCATION ${location.latitude}/${location.longitude} FROM RUNNER =-")
                    lastLocReceivedAt = System.currentTimeMillis()
                    uploadMyLocation()
                }
            }
    }

    /**
     * Makes the actual call to the API for member locations.  Once received the existing
     * member locations are cleared from the database (delete all) and the ones just received
     * are saved in their place.
     */
    private fun requestMemberLocations() = CoroutineScope(IO).launch {

        if (!okayToRequestLocations()) {
            return@launch
        }

        if (tripcode == null) {
            return@launch
        }

        if (!isWaitingOnMemberLocApi) {
            isWaitingOnMemberLocApi = true
            withContext(Main) {
                _serviceStatus.value?.isRequestingMemberLocs = true
                postServiceStatus( "Requesting member locations...")
            }
            try {
                getMemberLocsFromApiUseCase.execute(tripcode!!).collect { apiResponse ->
                    when (apiResponse) {
                        is Resource.Success -> {
                            lastRequestedLocations = System.currentTimeMillis()
                            isWaitingOnMemberLocApi = false
                            deleteAllMemberLocsFromDbUseCase.execute()
                            apiResponse.data?.locUpdates?.let {
                                saveMemberLocsToDbUseCase.executeMany(it)
                            }
                            withContext(Main) {
                                _serviceStatus.value?.isRequestingMemberLocs = false
                                postServiceStatus( "Received member locations!")
                            }
                        }
                        is Resource.Loading -> {
                            isWaitingOnMemberLocApi = true
                            withContext(Main) {
                                _serviceStatus.value?.isRequestingMemberLocs = true
                            }
                        }
                        is Resource.Error -> {
                            isWaitingOnMemberLocApi = false
                            lastRequestedLocations = System.currentTimeMillis()
                            withContext(Main) {
                                _serviceStatus.value?.isRequestingMemberLocs = false
                                postServiceStatus( "Failed to get member locations.")
                            }
                            // writeOnetimeMsg(getString(R.string.failed_to_get_member_locations))
                        }
                    }
                }
            } catch (exception: NullPointerException) { }
        }
    }

    /**
     * Uploads the last known location to the API.
     */
    private fun uploadMyLocation() = CoroutineScope(IO).launch {

        if (!okayToUploadLocation()) {
            return@launch
        }

        // If the tripcode is null here something has gone horribly wrong.
        if (AppPreferences.tripCode == null) {
            stopRequestingMemberLocations()
            stopServerUrlMonitorRunner()
            unsubscribeToLocationUpdates(false)
            return@launch
        }

        generateNotification()

        // If we have a pending API request we bail so it can finish.
        if (isWaitingOnMyLocApi) {
            return@launch
        }

        // Build a MyLocation object for the purposes of saving it to the local DB.
        lastKnownLocation?.let {
            // If the service is in the foreground it means the app is in the background.
            // Stipulating foreground/background before uploading will allow us to represent
            // to trip members whether a user has the app in the foreground or not.
            if (serviceRunningInForeground) {
                lastKnownLocation?.isBg = 1
            } else {
                lastKnownLocation?.isBg = 0
            }

            // Convert MyLocation object to LocUpdate for consumption by the API
            lastKnownLocation!!.toLocUpdate(tripcode!!).let { locUpdate ->
                withContext(Main) {
                    _serviceStatus.value?.isUploadingMyLoc = true
                    postServiceStatus("Uploading my location...")
                }

                // Tack on a waypoint if exists
                getWaypointPositionUseCase.execute()?.let {
                    locUpdate.waypoint = it.toJson()
                }

                // Upload my loc to API
                isWaitingOnMyLocApi = true
                Log.i(TAG3, "-=uploadMyLocation:UPLOADING MY LOCATION =-")
                uploadMyLocToApiUseCase.execute(locUpdate).collect { baseApiResponse ->
                    when (baseApiResponse) {
                        is Resource.Success -> {
                            isWaitingOnMyLocApi = false
                            if (baseApiResponse.data?.wasSuccessful == true) {
                                lastUploadedLocation = System.currentTimeMillis()
                                withContext(Main) {
                                    _serviceStatus.value?.isUploadingMyLoc = false
                                    postServiceStatus("Successfully uploaded my location")
                                }
                                // It's nice to see our location change on the map after
                                // uploading our loc so we request that now.
                                // requestMemberLocations()
                            } else {
                                Log.w(TAG, "performMyLocUpload: ${baseApiResponse.data?.genericValue}")
                            }
                        }
                        is Resource.Loading -> {
                            isWaitingOnMyLocApi = false
                            withContext(Main) {
                                _serviceStatus.value?.isUploadingMyLoc = true
                            }
                        }
                        is Resource.Error -> {
                            lastUploadedLocation = System.currentTimeMillis()
                            isWaitingOnMyLocApi = false
                            withContext(Main) {
                                _serviceStatus.value?.isUploadingMyLoc = false
                                postServiceStatus("Failed to upload my location")
                            }
                        }
                    } // API response code.
                } // collect API results
            } // LocUpdate is not null
        } // currentLocation?.let {
    }

    /**
     * Posts a lightweight status about the service to live data for any observers outside.
     */
    private fun postServiceStatus(msg: String? = null) {
        _serviceStatus.value?.log1 = msg
        _serviceStatus.postValue(_serviceStatus.value)
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: TripUsersLocationManagementService
            get() = this@TripUsersLocationManagementService
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

    companion object {
        private const val TAG = "FIMTOWN|ForegroundOnlyLocationService"
        private const val TAG2 = "SERVICESTATE"
        private const val TAG3 = "TAG3"
        private const val TAG4 = "TAG4"

        private const val LOCATION_REQUEST_INTERVAL_BACKGROUND = 5L
        private const val LOCATION_REQUEST_INTERVAL_DEFAULT = 1L
        private const val LOCATION_REQUEST_INTERVAL_RIGOROUS = 1L

        const val RIGOROUS_UPDATES_INTENT_EXTRA = "RIGOROUS_UPDATES_INTENT_EXTRA"

        private const val NOTIFICATION_ID = 12345678

        private const val BASE_URL_CHECK_INTERVAL = 5L

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"

        var isRunning = false

        private val _serviceStatus: MutableLiveData<ServiceStatus> = MutableLiveData(
            ServiceStatus())
        val serviceStatus: LiveData<ServiceStatus> = _serviceStatus

        private val _location: MutableLiveData<Location> = MutableLiveData()
        val location: LiveData<Location> = _location
    }

}

fun LatLng.toJson(): String? {
    return Gson().toJson(this)
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