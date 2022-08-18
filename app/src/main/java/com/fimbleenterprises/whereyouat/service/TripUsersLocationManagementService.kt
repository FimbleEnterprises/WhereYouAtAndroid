package com.fimbleenterprises.whereyouat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.utils.Resource
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
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

    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    // TODO: Step 1.1, Review variables (no changes).
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    // Handler and runner for continuously requesting member locations.
    private var myHandler: Handler = Handler(Looper.myLooper()!!)
    private var runner: Runnable? = null

    // Set true when request is made and false when it returns.
    private var isWaitingOnApi = false

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

    private var lastUploadedLocation: Long = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        CoroutineScope(IO).launch {
            deleteMyLocFromDbUseCase.execute()
            deleteAllMemberLocsFromDbUseCase.execute()
            Log.i(TAG, "-=ForegroundOnlyLocationService:onCreate Cleared all locations from db =-")
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // TODO: Step 1.2, Review the FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO: Step 1.3, Create a LocationRequest.
        locationRequest = LocationRequest.create().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = TimeUnit.SECONDS.toMillis(LOCATION_REQUEST_INTERVAL)

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = TimeUnit.SECONDS.toMillis(FASTEST_LOCATION_REQUEST_INTERVAL)

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.SECONDS.toMillis(MAX_WAIT_LOCATION_REQUEST)

            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // TODO: Step 1.4, Initialize the LocationCallback.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

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

                // Updates notification content if this service is running as a foreground
                // service.
                if (serviceRunningInForeground) {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        generateNotification(currentLocation))
                }

                // Save and upload our location.
                CoroutineScope(IO).launch {
                    currentLocation?.let {

                        val myLocation = MyLocation(0,
                            System.currentTimeMillis(),
                            45,
                            currentLocation!!.latitude,
                            currentLocation!!.longitude
                        )
                        // Save to local storage, cause why not.
                        saveMyLocToDbUseCase.execute(myLocation)

                        if (okayToUploadLocation()) {
                            // Upload my loc to API
                            uploadMyLocToApiUseCase.execute(myLocation.toLocUpdate()).collect {
                                when (it) {
                                    is Resource.Success -> {
                                        Log.i(TAG,
                                            "-=ForegroundOnlyLocationService:onLocationResult SUCCESS =-")
                                        Log.i(TAG,
                                            "-=ForegroundOnlyLocationService:wasSuccessful:${it.data?.wasSuccessful} =-")
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
                        } // okayToUploadLocation
                    } // currentLocation?.let {
                } // bg coroutine
            } // on Loc received
        } // loc callback
    }// onCreate

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        // Start the runner to repeated request member locations.
        startRequestingMemberLocations()

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
        // return
        return super.onStartCommand(intent, START_FLAG_RETRY, startId)
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

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
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
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        stopRequestingMemberLocations()
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun okayToUploadLocation(): Boolean = (System.currentTimeMillis()
            - lastUploadedLocation) > TimeUnit.SECONDS.toMillis(MAX_WAIT_LOCATION_REQUEST)

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

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

    private fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
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
        val mainNotificationText = location?.toText() ?: getString(R.string.no_location_text)
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSilent(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_launch, getString(R.string.launch_activity),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_cancel,
                getString(R.string.stop_location_updates_button_text),
                servicePendingIntent
            )
            .build()
    }

    /**
     * Stops the runner and nulls it out.
     */
    private fun stopRequestingMemberLocations() {
        if (runner != null) {
            try {
                myHandler.removeCallbacks(runner!!)
                myHandler.removeCallbacksAndMessages(null)
                runner = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Starts a runner that repeatedly checks for member locations.
     */
    private fun startRequestingMemberLocations() {

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
            requestMemberLocations()
            myHandler.postDelayed(runner!!, 15000)
        }

        // Starts it up initially
        myHandler.postDelayed(runner!!, 15000)
    }

    private fun requestMemberLocations() = CoroutineScope(IO).launch {
        isWaitingOnApi = true
        getMemberLocsFromApiUseCase.execute(WhereYouAt.AppPreferences.tripcode).collect { apiResponse ->
            when (apiResponse) {
                is Resource.Success -> {
                    isWaitingOnApi = false
                    apiResponse.data?.locUpdates?.forEach {
                        try {
                            deleteAllMemberLocsFromDbUseCase.execute()
                            saveMemberLocsToDbUseCase.execute(it)
                        } catch (exception:Exception) {
                            Log.e(TAG, "requestMemberLocations: ${exception.localizedMessage}"
                                , exception)

                        }
                        Log.i(TAG, "-=ForegroundOnlyLocationService:requestMemberLocations Saved $it =-")
                    }
                }
                is Resource.Loading -> {
                    isWaitingOnApi = true
                }
                is Resource.Error -> {
                    isWaitingOnApi = false
                }
            }
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: TripUsersLocationManagementService
            get() = this@TripUsersLocationManagementService
    }

    companion object {
        private const val TAG = "ForegroundOnlyLocationService"

        private const val LOCATION_REQUEST_INTERVAL = 15L
        private const val FASTEST_LOCATION_REQUEST_INTERVAL = 1L
        private const val MAX_WAIT_LOCATION_REQUEST = 30L
        private const val UPLOAD_LOCATION_INTERVAL = 30
        private const val MINIMUM_DISTANCE_FOR_UPLOAD = 10

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
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