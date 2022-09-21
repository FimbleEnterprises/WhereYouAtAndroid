package com.fimbleenterprises.whereyouat.service

import android.Manifest
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.cancellable
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
    private var myHandler: Handler = Handler(Looper.myLooper()!!)
    private var runner: Runnable? = null

    /**
     * Starts a runner that repeatedly checks for member locations.
     */
    private fun startMonitoring() {
        if (runner != null) {
            try {
                myHandler.removeCallbacks(runner!!)
                myHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        runner = Runnable {
            // What runs each time
            val seconds = (System.currentTimeMillis() - lastLocReceivedAt) / 1000
            Log.i(TAG3, "-=startMonitoring: Last loc was $seconds ago. =-")
            myHandler.postDelayed(runner!!, 1000)
        }

        // Starts it up initially
        myHandler.postDelayed(runner!!, 0)
    }

    private fun stopMonitoring() {
        if (runner != null) {
            try {
                myHandler.removeCallbacks(runner!!)
                myHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    // endregion

    init {
        Log.i(TAG, "Initialized:TripUsersLocationManagementService")
    } // init

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        startMonitoring()

        startObservingServiceStatus()

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics

        // Tripcode should never be null here.
        tripcode = AppPreferences.tripCode
        if (tripcode == null) {
            Log.w(TAG2, "onCreate: TRIPCODE SHOULD NOT BE NULL HERE AT SERVICE!  STOPPING!!")
            CoroutineScope(IO).launch {
                withContext(Main) {
                    stopSelf()
                }
            }
            return
        }

        // Clear all locations for all members
        CoroutineScope(IO).launch {
            deleteMyLocFromDbUseCase.execute()
            deleteAllMemberLocsFromDbUseCase.execute()
            Log.i(TAG, "-=ForegroundOnlyLocationService:onCreate Cleared all locations from db =-")
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        initLocationRequests()

    } // onCreate

    private fun startObservingServiceStatus() = CoroutineScope(IO).launch {
        // -----------------------------------------------------------
        //   Retrieve and continue to monitor the db service status.
        // -----------------------------------------------------------

        // Do testing with this commented out - I had a crash down below due to serviceState
        // being uninitialized and so explicitly set its value here.  I feel like setting the
        // value within the flow below should cover this but I need to test to confirm.
        // serviceState = serviceStateUseCases.getServiceState()
        // Log.i(TAG2, "-=onCreate:Retrieved service state from db (on main thread):$serviceState =-")

        Log.i(TAG2, "-=onCreate:Coroutine to capture service state flow started. =-")
        serviceStateUseCases.getServiceStateAsFlow().cancellable().collect {
            serviceState = it
            when (it.state) {
                ServiceState.SERVICE_STATE_STOPPING -> {
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:STOPPING =-")
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:KILLING SERVICE BECAUSE STOPPING STATE =-")
                    unsubscribeToLocationUpdates()
                    stopSelf()
                }
                ServiceState.SERVICE_STATE_STOPPED -> {
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:STOPPED =-")
                    it.state = ServiceState.SERVICE_STATE_STOPPED
                }
                ServiceState.SERVICE_STATE_RUNNING -> {
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:RUNNING =-")
                }
                ServiceState.SERVICE_STATE_STARTING -> {
                    Log.i(TAG2, "-= SERVICE FLOW OBSERVER:STARTING =-")
                }
            }
        } // Collector
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancellationRequest =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

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

                    postServiceStatus()
                }
            }
        }

        // Tells the system not to recreate the service after it's been killed.
        return super.onStartCommand(intent, START_FLAG_REDELIVERY, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopRequestingMemberLocations()
        stopMyLocationUploadRunner()
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
        stopMonitoring()
    }

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
        uploadMyLocation(true)
        startMyLocationUploadRunner()
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    // MapFragment (client) returns to the foreground and rebinds to service, so the service
    // can become a background services.
    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        Log.i(TAG3, "-=onRebind: UPLOADING MY LOC =-")
        uploadMyLocation(true)
        startMyLocationUploadRunner()
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
        Log.d(TAG, "Start foreground service")

        // TODO I DON'T FUCKING KNOW, BRO
        // I screwed up somewhere such that this call can happen when the user leaves a trip and
        // the notification will show up.  The service is not actually running but the notification
        // is persistent and... it's bad.
        //
        // This RUNNING check seems to resolve it but it's feels so
        // fucking hacky!!!!!  Maybe I didn't screw up and this is fine but it feels wrong.
        if (serviceState.state == ServiceState.SERVICE_STATE_RUNNING) {
            val notification = generateNotification(lastKnownLocation?.toLocation())
            try {
                unsubscribeToLocationUpdates(true)
                startForeground(NOTIFICATION_ID, notification)
                serviceRunningInForeground = true
            } catch (exception:RuntimeException) {
                Log.e(TAG, "onUnbind: ${exception.localizedMessage}\n"
                    , exception)
            }
            Log.i(TAG3, "-=onUnbind: UPLOADING MY LOC =-")
            uploadMyLocation(true)
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
                        generateNotification(lastKnownLocation?.toLocation()))
                }

                if (rigorousUpdates) {
                    //Do upload actions
                    uploadMyLocation()
                    requestMemberLocations()
                }

            } // on Loc received
        } // loc callback
    }

    private fun okayToUploadLocation(): Boolean {
        val diff = System.currentTimeMillis() - lastUploadedLocation
        val thresh = 500
        return (diff) > thresh
    }

    private fun okayToRequestLocations(): Boolean {
        val diff = System.currentTimeMillis() - lastRequestedLocations
        val thresh = 500
        return (diff) > thresh
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
                        } else {
                            // stopSelf()
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

        /*val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)*/

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
            /*.addAction(
                R.drawable.ic_cancel,
                getString(R.string.leave_trip_notification_button),
                servicePendingIntent
            )*/
            .build()
    }

    /**
     * Starts a runner that repeatedly uploads the last known location.
     */
    private fun startMyLocationUploadRunner() {
        if (myLocationUploadRunner != null) {
            try {
                myLocationUploadHandler.removeCallbacks(myLocationUploadRunner!!)
                myLocationUploadHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        myLocationUploadRunner = Runnable {

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                // No permissions
                return@Runnable
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

            // What runs each time
            Log.d(TAG, "uploadMyLocation: ${lastUploadedLocation()} seconds ago.")

            var interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_DEFAULT)
            if (serviceRunningInForeground) {
                interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_BACKGROUND)
            } else {
                interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_DEFAULT)
            }

            myLocationUploadHandler.postDelayed(myLocationUploadRunner!!, interval)
        }

        // Starts it up initially
        myLocationUploadHandler.postDelayed(myLocationUploadRunner!!, 0)
    }

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
     * Starts a runner that repeatedly requests member locations.
     */
    private fun startRequestingMemberLocations() {

        // In case we somehow have a runner that's already instantiated we stop that shit dead.
        if (memberLocationRequestRunner != null) {
            memberLocationRequestHandler.removeCallbacks(memberLocationRequestRunner!!)
            memberLocationRequestHandler.removeCallbacksAndMessages(null)
        }

        var interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_DEFAULT)
        if (serviceRunningInForeground) {
            interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_BACKGROUND)
        } else {
            interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL_DEFAULT)
        }

        // Get locations manually, once, so we don't have to wait for the initial delay to elapse.
        Log.i(TAG3, "-=startRequestingMemberLocations:INITIAL REQUEST BEFORE RUNNER HAS STARTED =-")
        requestMemberLocations()

        // What runs on each execution/loop
        memberLocationRequestRunner = Runnable {
            Log.d(TAG, "requestMemberLocations: ${lastRequestedLocations()} seconds ago.")

            // Get trip member locations from the API
            requestMemberLocations()
            // Schedule it again for the future
            memberLocationRequestHandler.postDelayed(memberLocationRequestRunner!!, interval)
        }

        // Schedule the first loop
        memberLocationRequestHandler.postDelayed(memberLocationRequestRunner!!, interval)
    }

    /**
     * Makes the actual call to the API for member locations.
     */
    private fun requestMemberLocations() = CoroutineScope(IO).launch {

        if (!okayToRequestLocations()) {
            return@launch
        }

        if (tripcode == null) {
            return@launch
        }

        if (!isWaitingOnMemberLocApi) {
            // writeOnetimeMsg(getString(R.string.requesting_member_locations_from_api))
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
                            withContext(Main) {
                                _serviceStatus.value?.isRequestingMemberLocs = false
                                postServiceStatus( "Received member locations!")
                            }
                            deleteAllMemberLocsFromDbUseCase.execute()
                            apiResponse.data?.locUpdates?.let {
                                saveMemberLocsToDbUseCase.executeMany(it)
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
     * Save my current location to the DB and then upload to the API (if okay to do so).
     */
    private fun uploadMyLocation(ignoreWait: Boolean = false) = CoroutineScope(IO).launch {

        if (!okayToUploadLocation()) {
            return@launch
        }

        // If the tripcode is null here something has gone horribly wrong.
        if (AppPreferences.tripCode == null) {
            stopRequestingMemberLocations()
            unsubscribeToLocationUpdates(false)
            return@launch
        }

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
                            Log.i(TAG,
                                "-=ForegroundOnlyLocationService:onLocationResult LOADING =-")
                            withContext(Main) {
                                _serviceStatus.value?.isUploadingMyLoc = true
                            }
                        }
                        is Resource.Error -> {
                            lastUploadedLocation = System.currentTimeMillis()
                            isWaitingOnMyLocApi = false
                            Log.w(TAG,
                                "-=ForegroundOnlyLocationService:onLocationResult ERROR =-")
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

    private fun lastRequestedLocations(): Int {
        return ((System.currentTimeMillis() - lastRequestedLocations) / 1000).toInt()
    }

    private fun lastUploadedLocation(): Int {
        return ((System.currentTimeMillis() - lastUploadedLocation) / 1000).toInt()
    }

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

        private const val LOCATION_REQUEST_INTERVAL_BACKGROUND = 45L
        private const val LOCATION_REQUEST_INTERVAL_DEFAULT = 3L
        private const val LOCATION_REQUEST_INTERVAL_RIGOROUS = 1L

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        const val RIGOROUS_UPDATES_INTENT_EXTRA = "RIGOROUS_UPDATES_INTENT_EXTRA"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"

        var isRunning = false

        private val _serviceStatus: MutableLiveData<ServiceStatus> = MutableLiveData(
            ServiceStatus())
        val serviceStatus: LiveData<ServiceStatus> = _serviceStatus

        private val _location: MutableLiveData<Location> = MutableLiveData()
        val location: LiveData<Location> = _location
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