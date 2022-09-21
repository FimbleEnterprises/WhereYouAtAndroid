package com.fimbleenterprises.whereyouat.presentation.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.fimbleenterprises.whereyouat.BuildConfig
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberMarkers
import com.fimbleenterprises.whereyouat.model.MemberMarkers.MemberMarker
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.fimbleenterprises.whereyouat.model.containsMember
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.service.SharedPreferenceUtil
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.service.toText
import com.fimbleenterprises.whereyouat.utils.Helpers
import com.fimbleenterprises.whereyouat.utils.MyGeoUtil
import com.fimbleenterprises.whereyouat.utils.SphericalUtil
import com.fimbleenterprises.whereyouat.utils.Utils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap
    private lateinit var mLifecycleOwner: LifecycleOwner
    private val selectedMember: MutableLiveData<MemberMarker?> = MutableLiveData()
    private var cameraLockedOnMe = true
    private var cameraLockedOnParty = false
    private var cameraLockedOnMember = false
    private var cameraLocked = true
    private lateinit var mAdView : AdView
    private var iAmAlone = true

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

    // Handler and runner for clearing the info textview.
    private var myLogMsgHandler1: Handler = Handler(Looper.myLooper()!!)
    private var logMessageRunner1: Runnable? = null

    private var myCameraLockedHandler: Handler = Handler(Looper.myLooper()!!)
    private var myCameraLockedRunner: Runnable? = null

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
            if (viewmodel.serviceState.value?.state == ServiceState.SERVICE_STATE_STOPPED) {
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



        binding.memberInfoContainer.slideVisibility(false)

        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        mAdView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                Log.i(TAG, "-=onAdClicked: =-")
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                Log.i(TAG, "-=onAdClosed: =-")
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
                Log.i(TAG, "-=onAdFailedToLoad: =-")
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
                Log.i(TAG, "-=onAdImpression: =-")
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i(TAG, "-=onAdLoaded: =-")
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.i(TAG, "-=onAdOpened: =-")
            }
        }

    }

    private fun isProcessing(isBusy: Boolean) {

        binding.btnLeave.isEnabled = !isBusy
        binding.fabCenterMe.isEnabled = !isBusy
        binding.fabCenterTrip.isEnabled = !isBusy
        binding.fabShareCode.isEnabled = !isBusy

        when(isBusy) {
            true -> {
                binding.progressBar.visibility = View.VISIBLE
                // binding.mapContainer.visibility = View.INVISIBLE
                Utils.crossFadeAnimation(binding.progressBar, binding.mapContainer, 300)
            }
            else -> {
                binding.progressBar.visibility = View.GONE
                // binding.mapContainer.visibility = View.VISIBLE

                Utils.crossFadeAnimation(binding.mapContainer, binding.progressBar, 300)
            }
        }
    }

    override fun onStart() {

        binding.btnLeave.setOnClickListener {

            isProcessing(true)

            // Just an arbitrary delay to prevent spamming leave/resume/create trip.
            Handler(Looper.getMainLooper()).postDelayed({
                viewmodel.requestServiceStop()
            }, 250)
        }

        binding.fabCenterTrip.setOnClickListener {

            // If there is a member selected this will unselect them.  Method handles nulls so
            // feel free to call on a whim.
            unselectMember()

            if (viewmodel.memberLocations.value != null && viewmodel.memberLocations.value?.size!! > 0) {
                moveCameraToShowParty()
            }
            cameraLockedOnParty = true
            cameraLockedOnMe = false
            cameraLockedOnMember = false
        }

        binding.fabCenterMe.setOnClickListener {
            moveCameraToShowMe()
            cameraLockedOnParty = false
            cameraLockedOnMember = false
            cameraLockedOnMe = true
        }

        binding.fabShareCode.setOnClickListener {
            if (WhereYouAt.AppPreferences.tripCode != null) {
                viewmodel.shareTripcode(WhereYouAt.AppPreferences.tripCode!!)
            }
        }

        // Fire up the service
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val serviceIntent = Intent(requireContext(), TripUsersLocationManagementService::class.java)
        unselectMember()
        requireContext().bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        super.onStart()

        // Check permissions and leave trip if they're suddenly gone!
        if (!foregroundPermissionApproved()) {
            viewmodel.requestServiceStop()
        }

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

    private fun startObservingMyLocation() {

        // The tripcode will be null if the user clicks the leave trip button so we should
        // bail if that is the case.
        if (WhereYouAt.AppPreferences.tripCode == null) {
            return
        }

        viewmodel.myLocation.observe(this@MapFragment) { myLocation ->

            // If we no longer know who this user is we leave the trip.
            if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {
                viewmodel.requestServiceStop()
                Toast.makeText(context,
                    "Please sign in!",
                    Toast.LENGTH_SHORT).show()
                viewmodel.myLocation.removeObservers(viewLifecycleOwner)
                return@observe
            }

            if (myLocation != null) {
                val position = LatLng(myLocation.lat, myLocation.lon)

                // If this is the first time we have appeared on the map we move the camera.
                if (!this::myMapMarker.isInitialized) {
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(
                                LatLng(myLocation.lat, myLocation.lon), 16f
                            )
                        )
                    )

                    // Create our marker
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title("Me")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_map_icon_mini))

                    // Create a MemberMarker object to hold our location
                    myMapMarker = MemberMarker(
                        map.addMarker(markerOptions)!!,
                        myLocation.toLocUpdate(WhereYouAt.AppPreferences.tripCode!!),
                        null,
                        null
                    )
                } // !this::myMapMarker.isInitialized

                // Update our marker's position on the map.
                myMapMarker.marker.position = position

                // Update all polylines
                memberMarkers.forEach { memberMarker ->
                    if (memberMarker.polyline != null) {
                        memberMarker.polyline?.remove()
                        memberMarker.polyline = drawPolyFromTo(myLocation.toLatLng(), memberMarker.locUpdate.toLatLng())
                    }
                }

                // Do maths on selected member if selected
                if (selectedMember.value != null) {
                    displayDistanceAndDirectionToSelectedMember()
                }

                if (viewmodel.memberLocations.value?.size == 1) {
                    iAmAlone = true
                    cameraLocked = true
                    cameraLockedOnMe = true
                    cameraLockedOnParty = false
                    cameraLockedOnMember = false
                } else if (viewmodel.memberLocations.value?.size != 0 && iAmAlone) {
                    iAmAlone = false
                    cameraLocked = true
                    cameraLockedOnParty = true
                    cameraLockedOnMe = false
                    cameraLockedOnMember = false
                }

            }

            // Move camera
            moveCameraContextually()
        }
    }

    private fun startObservingMemberLocations() {

        viewmodel.memberLocations.observe(this@MapFragment) { memberList ->

            Log.i("TAG4", "-=startObservingMemberLocations: MAP FRAG OBSERVES ${memberList.size} members. =-")

            // If we no longer know who this user is we leave the trip.
            if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {
                viewmodel.requestServiceStop()
                Toast.makeText(context,
                    getString(R.string.please_sign_in),
                    Toast.LENGTH_SHORT).show()
                viewmodel.myLocation.removeObservers(viewLifecycleOwner)
                return@observe
            }

            if (memberList.isEmpty()) {
                return@observe
            }

            drawMembersOnMap(memberList)

            // Calculate metrics for selected member if applicable
            if (selectedMember.value != null) {
                // It is possible that the server has removed this user due to inactivity.
                if (memberList.size == 1) {
                    iAmAlone = true
                    cameraLocked = true
                    cameraLockedOnMe = true
                    cameraLockedOnParty = false
                    cameraLockedOnMember = false
                    unselectMember()
                } else {
                    displayDistanceAndDirectionToSelectedMember()
                }
            } else {
                if (memberList.size == 1) {
                    iAmAlone = true
                    cameraLocked = true
                    cameraLockedOnMe = true
                    cameraLockedOnParty = false
                    cameraLockedOnMember = false
                } else if (memberList.size > 1 && iAmAlone) {
                    iAmAlone = false
                    cameraLocked = true
                    cameraLockedOnParty = true
                    cameraLockedOnMe = false
                    cameraLockedOnMember = false
                }
            }

            // Move camera
            moveCameraContextually()

        }  // observe

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            Log.i(TAG, "-=MapFragment:onSharedPreferenceChanged  =-")
        }
    }

    private fun startObservingServiceStatus() {
        TripUsersLocationManagementService.serviceStatus.observe(viewLifecycleOwner) {
            binding.textView3.text = it.log1
            startDelayWipeOfLogMsg1()
        }
    }

    private fun startObservingServiceState() {
        viewmodel.serviceState.observe(viewLifecycleOwner) {

            when (it.state) {
                ServiceState.SERVICE_STATE_STOPPING -> {
                    // I want to wrap this in a if(foregroundOnlyLocationServiceBound){ block like I
                    // do in onStop() but it shouldn't ever false...
                    if(foregroundOnlyLocationServiceBound) {
                        requireContext().unbindService(foregroundOnlyServiceConnection)
                        foregroundOnlyLocationServiceBound = false
                    }
                }
                ServiceState.SERVICE_STATE_STOPPED -> {
                    isProcessing(false)
                    Toast.makeText(context, "Left trip.", Toast.LENGTH_SHORT).show()
                    // Navigate to start frag
                    findNavController().popBackStack()
                    val navBuilder = NavOptions.Builder()
                    navBuilder.setEnterAnim(R.anim.slide_in_right).setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(android.R.anim.fade_in).setPopExitAnim(android.R.anim.fade_out)
                    findNavController().navigate(
                        R.id.startFragment, null, navBuilder.build()
                    )
                }
                else -> {
                    binding.txtTripCode.text = WhereYouAt.AppPreferences.tripCode
                    startDelayWipeOfLogMsg1()
                }
            }
        }
    }

    private fun startObservingSelectedMember() {
        selectedMember.observe(viewLifecycleOwner) { selectedMember ->
            if (selectedMember != null) {
                displayDistanceAndDirectionToSelectedMember()
                binding.memberInfoContainer.visibility = View.VISIBLE
                binding.memberInfoContainer.slideVisibility(true, 600)
            } else {
                binding.memberInfoContainer.visibility = View.GONE
                binding.memberInfoContainer.slideVisibility(false, 600)
                Log.i(TAG, "-=startObservingSelectedMember: =-")
            }
            moveCameraContextually()
        }
    }

    private fun drawMembersOnMap(memberList: List<LocUpdate>) {

        // Remove any existing circles before we start looping through the members and recreating them.
        memberMarkers.removeAllCircles()

        // Loop through all member locations.
        memberList.forEach { memberLoc ->

            // Build a LatLng for convenience
            val position = LatLng(memberLoc.lat, memberLoc.lon)

            // Get a new circle started
            val cOptions = CircleOptions()
            memberLoc.accuracy?.let {
                cOptions.fillColor(getCircleColor(it.toDouble()))
                .strokeColor(getCircleColor(it.toDouble()))
                .center(position)
                .radius(it.toDouble())
            }


            val markerOptions: MarkerOptions =
                MarkerOptions().position(position)

            if (memberLoc.memberid == WhereYouAt.AppPreferences.memberid) {
                markerOptions.title(getString(R.string.me))
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_maroon))
            } else {
                markerOptions.title(memberLoc.displayName)
                if (memberLoc.isBg == 1) {
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_gray))
                }
            }

            // Create a bucket to hold any zombie markers from users that have left the party
            val toBeRemoved = ArrayList<MemberMarker>() // A list of the condemned
            // See if a marker lacks an actual active member
            memberMarkers.forEach {
                // Remove all circles no matter what or we can get zombies
                if (!memberList.containsMember(it.locUpdate)) {
                    toBeRemoved.add(it)
                }
            }
            // Loop through our list of markers to be removed and remove them from
            // memberMarkers array (to avoid concurrency errors)
            toBeRemoved.forEach { memberMarkers.removeMarker(it) }

            // See if we already have a marker for this member
            val existingMember = memberMarkers.findMember(memberLoc)
            when (existingMember) {

                // Member does not have a marker yet. Add them to the list and put them on the map.
                null -> {
                    // Add the marker to the map so we can get a reference to it
                    val actualMapMarker = map.addMarker(markerOptions)!!

                    // Start working on getting the user's avatar
                    memberLoc.avatarUrl?.let { setMarkerUsingGlide(it, actualMapMarker) }

                    // Build an accuracy circle if the member loc has an accuracy value.
                    var circle: Circle? = null
                    memberLoc.accuracy?.let {
                        circle = map.addCircle(cOptions)
                    }

                    // Build and add this marker to our member markers array
                    memberMarkers.add(
                        MemberMarker(
                            actualMapMarker,
                            locUpdate = memberLoc,
                            polyline = null,
                            circle
                        )
                    )
                } // Member has a marker; update its location to move the existing map marker.
                else -> {
                    existingMember.locUpdate = memberLoc
                    if (memberLoc.isBg == 1) {
                        // Member is in background so show a boring, gray marker.
                        existingMember.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_gray))
                    } else {
                        // Member is in foreground, use Glide to build a marker using their profile's avatar url.
                        existingMember.locUpdate.avatarUrl?.let { setMarkerUsingGlide(it, existingMember.marker) }
                    }
                    existingMember.marker.position = position
                    memberLoc.accuracy?.let {
                        existingMember.circle = map.addCircle(cOptions)
                    }
                }
            }
        } // for each member
    }

    /**
     * Uses Glide to make a network call obtaining the user's Google profile avatar and applies it
     * applies it to the supplied marker.  Not doing any of our own caching, trusting Glide to
     * handle it instead.
     */
    private fun setMarkerUsingGlide(url: String, marker: Marker) {
        Glide.with(this).load(url).listener(object : RequestListener<Drawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>?,
                isFirstResource: Boolean,
            ): Boolean {
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_maroon))
                return true
            }
            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable?>?,
                dataSource: DataSource?,
                isFirstResource: Boolean,
            ): Boolean {
                marker.setIcon(Helpers.Bitmaps.getBitmapDescriptorFromDrawable(resource))
                return true
            }
        }).submit()
    }

    private fun View.slideVisibility(visibility: Boolean, durationTime: Long = 300) {
        val transition = Slide(Gravity.BOTTOM)
        transition.apply {
            duration = durationTime
            addTarget(this@slideVisibility)
        }
        TransitionManager.beginDelayedTransition(this.parent as ViewGroup, transition)
        this.isVisible = visibility
    }

    /**
     * Shows the selected member's name, distance and cardinal direction.
     */
    private fun displayDistanceAndDirectionToSelectedMember() {

        selectedMember.value?.let { selectedMember ->

            // My location scope
            viewmodel.myLocation.value?.let { myLocation ->
                selectedMember.removePolyline()
                selectedMember.polyline = drawPolyFromTo(
                    myLocation.toLatLng(),
                    selectedMember.locUpdate.toLatLng()
                )

                val metersAway = SphericalUtil.computeDistanceBetween(
                    myLocation.toLatLng(),
                    selectedMember.locUpdate.toLatLng()
                )
                val milesAway = MyGeoUtil.convertMetersToMiles(metersAway, 2)
                val radian = (myLocation.bearingTo(selectedMember.locUpdate))

                // Display distance and direction to the selected user.
                binding.txtMemberInfo3.text = getString(
                    R.string.units_away,
                    milesAway.toString(),
                    MyGeoUtil.calculateBearingFromRadian(radian)
                )
            } // my location scope

            binding.txtMemberInfo1.text = selectedMember.locUpdate.displayName

            // Display age of member's location
            val secondsOld = (System.currentTimeMillis() - selectedMember.locUpdate.createdon) / 1000
            val prettySeconds = Helpers.Numbers.secondsAgo(secondsOld.toInt())
            binding.txtMemberInfo4.text = prettySeconds
        } // selectedMember scope

        // Reset the camera if member is no longer selected.
        if (selectedMember.value == null) {
            cameraLockedOnMember = false
            cameraLockedOnParty = true
            cameraLockedOnMe = false
        } else {
            cameraLockedOnMember = true
            cameraLockedOnParty = false
            cameraLockedOnMe = false
        }

        // Move the camera
        moveCameraContextually()
    }

    /** Review Permissions: Method checks if permissions approved. */
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

    /** Review Permissions: Method checks if permissions approved. */
    private fun foregroundPermissionApproved(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
    }

    /** Review Permissions: Method requests permissions. */
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
            showTwoButtonSnackbar()

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

    /**
     * Shows a fragment explaining why we need and how we allow background loc permission.
     */
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

    /**
     * Starts a runner that repeatedly checks for member locations.
     */
    private fun startDelayWipeOfLogMsg1() {
        if (logMessageRunner1 != null) {
            myLogMsgHandler1.removeCallbacks(logMessageRunner1!!)
            myLogMsgHandler1.removeCallbacksAndMessages(null)
        }

        logMessageRunner1 = Runnable {
            // What runs each time

            myLogMsgHandler1.postDelayed(logMessageRunner1!!, 3000)
        }

        // Starts it up initially
        myLogMsgHandler1.postDelayed(logMessageRunner1!!, 750)
    }

    /**
     * The camera gets unlocked when the user interacts with it.  When the camera is unlocked it
     * will ignore all normal camera updates as called from moveCameraContextually().   After an
     * arbitrary delay it will be re-locked.
     */
    private fun startDelayToReLockCamera() {

        // If it's already up with messages pending we cancel those
        if (myCameraLockedRunner != null) {
            myCameraLockedHandler.removeCallbacks(myCameraLockedRunner!!)
            myCameraLockedHandler.removeCallbacksAndMessages(null)
        }

        // What will get done when the time comes.
        myCameraLockedRunner = Runnable {
            cameraLocked = true
            moveCameraContextually()
        }

        // Start the delay
        myCameraLockedHandler.postDelayed(myCameraLockedRunner!!, 6000)
    }

    /**
     * Evaluates context and moves camera accordingly.
     */
    private fun moveCameraContextually() {

        if (!cameraLocked) {
            Log.i(TAG, "-=moveCameraContextually:Camera is currently free, cannot move it=-")
            return
        }

        // Move camera based on context
        if (cameraLockedOnMember) {
            moveCameraToShowSelectedMemberAndMe()
        } else if (cameraLockedOnParty) {
            moveCameraToShowParty()
        } else if (cameraLockedOnMe) {
            moveCameraToShowMe()
        }

    }

    /** Moves the camera to a position such that both the start and end map markers are viewable on screen.  */
    private fun moveCameraToShowParty() {

        if (viewmodel.memberLocations.value.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "Moving the camera to get all the markers in view")
        val cu: CameraUpdate

        // Create a new LatLngBounds.Builder object
        val builder = LatLngBounds.Builder()
        viewmodel.memberLocations.value?.forEach {
            builder.include(it.toLatLng())
        }

        // Include our real-time location as well as all of the API supplied locations
        // (including us again).  Otherwise when moving at speed the blue marker will
        // quickly travel off-screen.
        viewmodel.myLocation.value?.let {
            builder.include(it.toLatLng())
        }

        cu = CameraUpdateFactory.newLatLngBounds(builder.build(),250)

        map.animateCamera(cu, 750, null)
    }

    /** Moves the camera to a position such that both the start and end map markers are viewable on screen.  */
    private fun moveCameraToShowSelectedMemberAndMe() {

        if (selectedMember.value == null) {
            return
        }

        // The member locations array is emptied and refilled and we can find ourselves here
        // during the empty so we bail if it happens.
        if (viewmodel.memberLocations.value.isNullOrEmpty()) {
            return
        }

        val selMember = selectedMember.value?.locUpdate!!
        val me = myMapMarker.locUpdate

        // Create a new LatLngBounds.Builder object
        val builder = LatLngBounds.Builder()

        val temp = ArrayList<LocUpdate>()
        viewmodel.memberLocations.value?.forEach {
            if ((it.memberid == me.memberid) || (it.memberid == selMember.memberid)) {
                temp.add(it)
            }
        }

        temp.forEach {
            builder.include(it.toLatLng())
        }

        // Include our real-time location as well as our last known (by the API) location.
        // Otherwise when moving at speed the blue marker will quickly travel off-screen.
        viewmodel.myLocation.value?.let {
            builder.include(it.toLatLng())
        }

        try {
            /*cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 350)
            map.animateCamera(cu,300, null)*/
            TripUsersLocationManagementService.location.value?.bearing?.let { pointCameraAt(it) }
        } catch (exception:IllegalStateException) {
            Log.e(TAG, "Failed to create camera update for selected user and me: ${exception.localizedMessage}"
                , exception)

        }
    }

    /**
     * Attempts to point the camera such that the bearing supplied is up.
     *
     * BELOW NOT IMPLEMENTED
     * Also, tries to move the
     * user's marker toward the bottom of the screen.
     * https://stackoverflow.com/a/16764140/2097893
     */
    private fun pointCameraAt(bearing: Float) {

        viewmodel.myLocation.value?.let {
            try {
                var zoom = 18f
                if (it.speed != null) {
                   zoom = calculateZoomAccordingToSpeed(it.speed)
                }

                // This is buggy so I'm removing it for now.
                /*val projection: Projection = map.projection
                val markerPosition: LatLng = it.toLatLng()
                val markerPoint: Point = projection.toScreenLocation(markerPosition)
                val targetPoint = Point(markerPoint.x, markerPoint.y - requireView().height / 10)
                val targetPosition = projection.fromScreenLocation(targetPoint)*/

                val cameraPosition = CameraPosition.Builder()
                    .target(it.toLatLng())
                    .bearing(bearing)
                    .tilt(90f)
                    .zoom(zoom)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            } catch (exception:NullPointerException) {
                Log.e(TAG, "pointCameraAt: ${exception.localizedMessage}"
                    , exception)

            }

        }
    }

    /**
     * Calculates a zoom based on user's velocity.  Faster is zoomed out further.
     */
    private fun calculateZoomAccordingToSpeed(speedTravelling: Float): Float {
        var setZoom = 16f
        Log.d(TAG, "Speed supplied was: $speedTravelling")
        if (speedTravelling < 3) {
            setZoom = 20f
        }
        if (speedTravelling > 5 && speedTravelling < 8) {
            setZoom = 19f
        }
        if (speedTravelling > 11 && speedTravelling < 14) {
            setZoom = 18f
        }
        if (speedTravelling > 16 && speedTravelling < 19) {
            setZoom = 17f
        }
        if (speedTravelling > 21) {
            setZoom = 16f
        }
        return setZoom
    }

    /**
     * Centers the camera on me.  Camera will face up indicating that is the bearing you are pointing.
     */
    private fun moveCameraToShowMe() {
        viewmodel.myLocation.value?.let { myLoc ->
            myLoc.bearing?.let { bearing ->
                pointCameraAt(bearing)
            }
        }
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

    /**
     * Fires when the Google map is ready and rendered.
     */
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
        startObservingServiceState()
        startObservingServiceStatus()

        map.setOnMarkerClickListener { marker ->

            // Try to find the member by their map marker.
            val memberMarker = memberMarkers.findMember(marker)
            memberMarker?.let { clickedMemberMarker ->
                if (clickedMemberMarker.isSelected) {
                    unselectMember()
                } else {
                    clickedMemberMarker.marker = marker
                    if (!clickedMemberMarker.isMe) {
                        memberMarkers.selectMember(clickedMemberMarker)
                        selectedMember.value = clickedMemberMarker
                        // Tell the service to begin rigorous updates.
                        viewmodel.requestVigorousUpdates(true)
                    } else {
                        Toast.makeText(context, getString(R.string.clicked_self_in_map), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Consume the click so the map doesn't show the info window
            true
        }

        map.setOnCameraMoveStartedListener {
            when (it) {
                REASON_GESTURE -> {
                    Log.i(TAG, "-=REASON_GESTURE: =-")
                    cameraLocked = false
                    startDelayToReLockCamera()
                }
                REASON_API_ANIMATION -> {
                    Log.i(TAG, "-=REASON_API_ANIMATION: =-")
                }
                REASON_DEVELOPER_ANIMATION -> {
                    Log.i(TAG, "-=REASON_DEVELOPER_ANIMATION: =-")
                }
            }
        }

        map.setOnCameraMoveListener {
            Log.v(TAG, "-=onCameraMove: =-")
        }

        map.setOnCameraMoveCanceledListener {
            Log.w(TAG, "-=onCameraMoveCancelled: =-")
        }

        map.setOnCameraIdleListener {
            Log.w(TAG, "-=onCameraIdle: =-")
        }

        if (!backgroundPermissionApproved()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundPermissions()
            }
        }

        startObservingSelectedMember()

        // If we rely solely on the livedata we can end up in a weird position from time to time.
        moveCameraContextually()

    }

    private fun unselectMember() {

        binding.txtMemberInfo1.text = null
        // binding.txtMemberInfo2.text = null
        binding.txtMemberInfo3.text = null
        binding.txtMemberInfo4.text = null

        if (selectedMember.value == null) {
            return
        }

        val clickedMemberMarker = memberMarkers.findMember(selectedMember.value!!.locUpdate)
        clickedMemberMarker?.removePolyline()
        memberMarkers.unselectAll()
        selectedMember.value = null

        // Tell the service to cease rigorous updates.
        viewmodel.requestVigorousUpdates(false)
    }

    private fun getCircleColor(accuracy: Double): Int {
        if (accuracy > 20.0) {
            return Color.parseColor("#25C10000")
        } else if (accuracy > 15.0) {
            return Color.parseColor("#25FFFF00")
        }  else {
            return Color.parseColor("#330022E2")
        }
    }

/*    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            Log.i(TAG, "-=MapFragment:onSharedPreferenceChanged  =-")
        }
    }*/

    private fun showTwoButtonSnackbar() {

        // Create the Snackbar
        val objLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        val snackbar =
            Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)

        // Get the Snackbar layout view
        val layout = snackbar.view as Snackbar.SnackbarLayout

        // Set snackbar layout params
        val navbarHeight: Int = getNavBarHeight(requireContext())
        val parentParams = layout.layoutParams as FrameLayout.LayoutParams
        parentParams.setMargins(0, 0, 0, 0 - navbarHeight + 50)


        // Inflate our custom view
        val snackView: View = layoutInflater.inflate(R.layout.two_button_snackbar, binding.root,false)

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
        textViewTwo.text = getString(R.string.dismiss)
        textViewTwo.setOnClickListener {
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


