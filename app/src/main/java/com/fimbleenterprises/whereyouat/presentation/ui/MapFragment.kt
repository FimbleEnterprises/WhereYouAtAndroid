package com.fimbleenterprises.whereyouat.presentation.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.fimbleenterprises.whereyouat.BuildConfig
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.model.MemberMarker
import com.fimbleenterprises.whereyouat.model.MemberMarkers
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.service.SharedPreferenceUtil
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.service.toText
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap
    private lateinit var mLifecycleOwner: LifecycleOwner

    /**
     * A container to represent us on the map
     */
    private lateinit var myMapMarker: MemberMarker
    /**
     * A container to represent our fellows on the map.
     */
    private val memberMarkers = MemberMarkers()

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences

    var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    var tripUsersLocationManagementService: TripUsersLocationManagementService? = null

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {

            // It is very possible to get here if the user has killed the trip using
            // the notification then multi-tasked back to the map frag which causes
            // the frag to rebind but the service isn't actually running and shit
            // will get fucking weird, fast.  We check for that before proceeding.
            if (viewmodel.serviceStatus.value?.isRunning == false &&
                    viewmodel.serviceStatus.value?.isStarting == false) {
                // Pretty sure we have to call unbind here.  onDestroy() isn't called
                // unless all bound members are gone.  I'm not 100% sure...
                requireContext().unbindService(this)
                return
            }
            val binder = service as TripUsersLocationManagementService.LocalBinder
            tripUsersLocationManagementService = binder.service
            foregroundOnlyLocationServiceBound = true
            tripUsersLocationManagementService!!.subscribeToLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            tripUsersLocationManagementService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        mLifecycleOwner = viewLifecycleOwner
        return inflater.inflate(R.layout.fragment_map, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()
        sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        binding = FragmentMapBinding.bind(view)
        viewmodel = (activity as MainActivity).mainViewModel

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

    }

    private fun isProcessing(isBusy: Boolean) {

        binding.btnLeave.isEnabled = !isBusy
        binding.btnPushUpdate.isEnabled = !isBusy
        binding.fabCenterMe.isEnabled = !isBusy
        binding.fabCenterTrip.isEnabled = !isBusy

        when(isBusy) {
            true -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.mapContainer.visibility = View.INVISIBLE
            }
            else -> {
                binding.progressBar.visibility = View.GONE
                binding.mapContainer.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {

        binding.btnPushUpdate.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            viewmodel.requestForcePush()
            viewmodel.oneTimeServiceStatusMsg.observe(viewLifecycleOwner) {
                binding.txtInfo1.text = it
            }
        }
        binding.btnLeave.setOnClickListener {

            isProcessing(true)

            // Just an arbitrary delay to prevent spamming leave/resume/create trip.
            Handler(Looper.getMainLooper()).postDelayed({
                isProcessing(false)
                viewmodel.stopService()
            }, 1500)
        }
        binding.fabCenterTrip.setOnClickListener {
            if (viewmodel.memberLocations.value != null && viewmodel.memberLocations.value?.size!! > 0) {
                moveCameraToShowMarkers()
            }
        }
        binding.fabCenterMe.setOnClickListener {
            centerCameraOnMe()
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val serviceIntent = Intent(requireContext(), TripUsersLocationManagementService::class.java)
        requireContext().bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        super.onStart()

    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                TripUsersLocationManagementService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            context?.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    private fun startObservingMemberLocations() {

        viewmodel.memberLocations.observe(this@MapFragment) { memberList ->
            memberList.forEach { member ->

                // Build a map marker for this member
                val position = LatLng(member.lat, member.lon)

                val markerOptions: MarkerOptions = if (member.memberid == WhereYouAt.AppPreferences.memberid) {
                        MarkerOptions()
                            .position(position)
                            .title(member.memberid.toString())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon32x32))
                    } else {
                        MarkerOptions()
                            .position(position)
                            .title(member.memberid.toString())
                    }

                // See if we already have a marker for this member
                when (val existingMember = memberMarkers.find(member)) {
                    // Add this member to the list and put them on the map.
                    null -> {
                        memberMarkers.add(
                            MemberMarker(
                                map.addMarker(markerOptions)!!,
                                member,
                                null
                            )
                        )
                    } // Otherwise just update their location property and move their existing map marker.
                    else -> {
                        existingMember.locUpdate = member
                        existingMember.marker.position = position
                    }
                }
            }
        }
    }

    private fun startObservingMyLocation() {

        // The tripcode will be null if the user clicks the leave trip button so we should
        // bail if that is the case.
        if (WhereYouAt.AppPreferences.tripCode == null) {
            return
        }

        viewmodel.myLocation.observe(this@MapFragment) {
            if (it != null) {
                val position = LatLng(it.lat, it.lon)

                // If this is the first time we have appeared on the map we move the camera.
                if (!this::myMapMarker.isInitialized) {
                    Log.i(TAG, "-=MapFragment:startObservingMyLocation  =-")
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(
                                LatLng(it.lat, it.lon), 16f
                            )
                        )
                    )

                    // Create our marker
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title("Me")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_map_icon))

                    // Create a MemberMarker object to hold our location
                    myMapMarker = it.toLocUpdate(WhereYouAt.AppPreferences.tripCode!!)?.let { locUpdate ->
                        MemberMarker(
                            map.addMarker(markerOptions)!!,
                            locUpdate,
                            null
                        )
                    }!!
                } // !this::myMapMarker.isInitialized

                // Update our marker's position on the map.
                myMapMarker.marker.position = position

                // Update all polylines
                memberMarkers.forEach { memberMarker ->
                    if (memberMarker.polyline != null) {
                        memberMarker.polyline?.remove()
                        memberMarker.polyline = drawPolyFromTo(it.toLatLng(), memberMarker.locUpdate.toLatLng())
                    }
                }
            }
        }
    }

    private fun startObservingServiceStatus() {
        viewmodel.serviceStatus.observe(viewLifecycleOwner) {

            if (!it.isStarting && !it.isRunning) {
                Toast.makeText(context, "Left trip.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                findNavController().navigate(
                    R.id.startFragment
                )
            } else {
                binding.txtTripCode.text = WhereYouAt.AppPreferences.tripCode
                binding.txtInfo1.text = it.oneTimeMessage

                startDelayWipeOfLogMsg()
            }
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun backgroundPermissionApproved(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundPermissions() {
        val provideRationale = backgroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            // Request permission
            ActivityCompat.requestPermissions(
                (activity as MainActivity),
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        } else {
            if (WhereYouAt.AppPreferences.nagUserAboutBgPermission) {
                showTwoButtonSnackbar()
            }

            /*Snackbar.make(
                binding.root,
                R.string.permission_rationale_background,
                60000
            )
                .setAction(R.string.okay) {
                    showBackgroundPermRationale()
                }
                .show()*/
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Now we ask for background permissions
                    if (!backgroundPermissionApproved()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestBackgroundPermissions()
                        }
                    }
                }
                // Permission was granted.

                else -> {
                    Snackbar.make(
                        binding.root,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun showBackgroundPermRationale() {
        BgPermRationaleDialogFragment(object : BgPermRationaleDialogFragment.DecisionListener {
            override fun affirmative() {
                Log.d(TAG, "Request background only permission")

                val provideRationale = backgroundPermissionApproved()
                if (provideRationale) {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(
                            (activity as MainActivity),
                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                        )
                    }
                }
            }

            override fun negative() {
                Toast.makeText(context, "Paranoid much?", Toast.LENGTH_SHORT).show()
            }

        }).show(parentFragmentManager, "")
    }
    // Handler and runner for clearing the info textview.
    private var myHandler: Handler = Handler(Looper.myLooper()!!)
    private var runner: Runnable? = null

    /**
     * Starts a runner that repeatedly checks for member locations.
     */
    private fun startDelayWipeOfLogMsg() {
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
            binding.txtInfo1.text = ""
            myHandler.postDelayed(runner!!, 3000)
        }

        // Starts it up initially
        myHandler.postDelayed(runner!!, 3000)
    }

    /** Moves the camera to a position such that both the start and end map markers are viewable on screen.  */
    private fun moveCameraToShowMarkers() {

        Log.d(TAG, "Moving the camera to get all the markers in view")
        val cu: CameraUpdate

        // Create a new LatLngBounds.Builder object
        val builder = LatLngBounds.Builder()
        viewmodel.memberLocations.value?.forEach {
            builder.include(it.toLatLng())
        }

        cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
        map.animateCamera(cu, 300, null)
    }

    private fun centerCameraOnMe() {
        val location = viewmodel.myLocation.value?.toLocation() ?: return
        val currentPlace = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .bearing(0f).tilt(90f).zoom(12f).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace),
            1000,
            object : CancelableCallback {
                override fun onFinish() {
                    Log.i(TAG, "onFinish ")
                }

                override fun onCancel() {
                    Log.i(TAG, "onCancel ")
                }
            })
    }

    /** Launches an intent that lets the user select a navigation app which after selection gets handed the parking spot's lat/lng values.  */
    fun startNavigation(spot: LatLng) {
        Toast.makeText(context, "Starting navigation...", Toast.LENGTH_SHORT).show()
        val myLocation = viewmodel.myLocation.value
        val myLat = myLocation?.lat
        val myLng = myLocation?.lon
        val placeLat = spot.latitude
        val placeLng = spot.longitude
        val uri: Uri = Uri.parse(
            getString(
                R.string.nav_intent,
                myLat.toString(),
                myLng.toString(),
                placeLat.toString(),
                placeLng.toString()
            )
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    /** Using the list of TripEntryObjects extend a polyline through each position  */
    private fun drawPolyFromTo(myPosition: LatLng, theirPosition: LatLng): Polyline {
        // Start building our poly line
        val line = PolylineOptions()
        line.width(5f)
        line.color(Color.RED)
        line.add(LatLng(myPosition.latitude, myPosition.longitude))
        line.add(LatLng(theirPosition.latitude, theirPosition.longitude))

        // Now that the polyline is built we simply add it to the map
        return map.addPolyline(line)
    }

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        this.map = googleMap

        startObservingMemberLocations()
        startObservingMyLocation()
        startObservingServiceStatus()

        map.setOnMarkerClickListener {
            val myloc = viewmodel.myLocation.value
            // Try to find the member by their map marker.
            val memberMarker = memberMarkers.find(it)
            if (myloc != null) {
                if (memberMarker?.polyline != null) {
                    memberMarker.polyline?.remove()
                    memberMarker.polyline = null
                } else {
                    memberMarker?.polyline = drawPolyFromTo(myloc.toLatLng(),
                        memberMarker?.locUpdate!!.toLatLng())
                }
            }
            false
        }

        if (!backgroundPermissionApproved()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundPermissions()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            Log.i(TAG, "-=MapFragment:onSharedPreferenceChanged  =-")
        }
    }

    private fun showTwoButtonSnackbar() {

        // Create the Snackbar
        val objLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        val snackbar =
            Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)

        // Get the Snackbar layout view
        val layout = snackbar.getView() as Snackbar.SnackbarLayout

        // Set snackbar layout params
        val navbarHeight: Int = getNavBarHeight(requireContext())
        val parentParams = layout.layoutParams as FrameLayout.LayoutParams
        parentParams.setMargins(0, 0, 0, 0 - navbarHeight + 50)


        // Inflate our custom view
        val snackView: View = layoutInflater.inflate(R.layout.two_button_snackbar, null)

        // Configure our custom view
        val messageTextView = snackView.findViewById(R.id.message_text_view) as TextView
        messageTextView.setText(R.string.permission_rationale_background)

        val textViewOne = snackView.findViewById(R.id.first_text_view) as TextView
        textViewOne.text = getString(R.string.fix)
        textViewOne.setOnClickListener {
            showBackgroundPermRationale()
            snackbar.dismiss()
        }

        val textViewTwo = snackView.findViewById(R.id.second_text_view) as TextView
        textViewTwo.text = getString(R.string.dont_remind_me)
        textViewTwo.setOnClickListener {
            WhereYouAt.AppPreferences.nagUserAboutBgPermission = false
            Log.d("Deny", "showTwoButtonSnackbar() : deny forever clicked")
            snackbar.dismiss()
        }

        // Add our custom view to the Snackbar's layout
        layout.addView(snackView, objLayoutParams)

        // Show the Snackbar
        snackbar.show()
    }

    private fun getNavBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Receiver for location broadcasts from [TripUsersLocationManagementService].
     */
    inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                TripUsersLocationManagementService.EXTRA_LOCATION
            )
            if (location != null) {
                Log.d(TAG, "-=onReceive|Foreground location: ${location.toText()} =-")
            }
        }
    }

    init { Log.i(TAG, "Initialized:MapFragment") }
    companion object {

        private const val REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 33
        private const val TAG = "FIMTOWN|MapFragment"
    }

}