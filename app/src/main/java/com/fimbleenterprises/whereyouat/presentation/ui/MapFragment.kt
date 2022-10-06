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
import com.fimbleenterprises.whereyouat.R.drawable.*
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.model.MapMarkers.MapMarker
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
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var cameraIsMoving = false
    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap
    private lateinit var mLifecycleOwner: LifecycleOwner
    private val selectedMember: MutableLiveData<MapMarker?> = MutableLiveData()
    private var cameraLockedOnMe = true
    private var cameraLockedOnParty = false
    private var cameraIncludeWaypoints = true
    private var cameraLockedOnMember = false
    private var cameraLocked = true
    private lateinit var mAdView : AdView
    private var iAmAlone = true
    // Handler and runner for clearing direction textview if not updated recently
    private var myDirectionWipeHandler: Handler = Handler(Looper.myLooper()!!)
    private var myDirectionWipeRunner: Runnable? = null
    // Handler and runner for clearing velocity textview if not updated recently
    private var myVelocityWipeHandler: Handler = Handler(Looper.myLooper()!!)
    private var myVelocityWipeRunner: Runnable? = null

    /**
     * A container to represent us on the map
     */
    private lateinit var myMapMarker: MapMarker
    /**
     * A container to represent our fellows on the map.
     */
    private val mapMarkers = MapMarkers()
    private val waypoints = Waypoints()
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
            tripUsersLocationManagementService!!.startWhereYouAtService()
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
        mapFragment?.getMapAsync(onMapReadyCallback)

        binding.memberInfoContainer.slideVisibility(false, 0)

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
        mAdView.visibility = View.GONE
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
            cameraLocked = true
            cameraLockedOnParty = true
            cameraLockedOnMe = false
            cameraLockedOnMember = false
            cameraIncludeWaypoints = !cameraIncludeWaypoints
            Log.d(TAG, "onStart: Include waypoints in camera: $cameraIncludeWaypoints")
            moveCameraContextually()
        }

        binding.fabCycleMembers.setOnClickListener {

            // Cycles through members and selects them assuming they are not
            // already selected or are me (ngl, I think this was pretty clever).
            var nextMember: MapMarker? = null
            mapMarkers.forEach {
                if (!it.isMe && !it.isSelected && nextMember == null) {
                    nextMember = it
                }
            }

            nextMember?.let {
                mapMarkers.selectMember(it)
                selectedMember.value = it
            }
        }

        binding.fabCenterMe.setOnClickListener {
            unselectMember()
            cameraLocked = true
            cameraLockedOnParty = false
            cameraLockedOnMember = false
            cameraLockedOnMe = true
            moveCameraContextually()
        }

        binding.fabShareCode.setOnClickListener {
            if (AppPreferences.tripCode != null) {
                viewmodel.shareTripcode(AppPreferences.tripCode!!)
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

        // Just a due-dilligence request for server-side guidance on update rate and base url.
        viewmodel.requestUpdateIntervalsFromApi()
        viewmodel.requestApiBaseUrlFromApi()

        // The camera can stop following if the device has been backgrounded for a time.
        // This WILL get called before the map is initialized so indeed a check must be
        // performed in there as well as adding a hacky delayed execution here.
        Handler(Looper.getMainLooper()).postDelayed({
            moveCameraContextually()
        }, 500)

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

    /**
     * Fires when the Google map is ready and rendered.
     */
    private val onMapReadyCallback = OnMapReadyCallback { googleMap ->
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
        startObservingSelectedMember()

        map.uiSettings.isMapToolbarEnabled = true

        map.setOnMarkerClickListener { marker ->

            // Try to find the member by their map marker.
            val memberMarker = mapMarkers.findMarker(marker)
            memberMarker?.let { clickedMemberMarker ->

                if (clickedMemberMarker.isSelected) {
                    unselectMember()
                } else {
                    clickedMemberMarker.marker = marker
                    if (!clickedMemberMarker.isMe) {
                        mapMarkers.selectMember(clickedMemberMarker)
                        selectedMember.value = clickedMemberMarker
                        // Tell the service to begin rigorous updates.
                        viewmodel.requestVigorousUpdates(true)
                    } else {
                        Toast.makeText(context, getString(R.string.clicked_self_in_map), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            marker.showInfoWindow()

            // Consume the click so the map doesn't show the info window
            true
        }

        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {

            override fun onMarkerDrag(marker: Marker) {
                // Super expensive from a CPU point of view but it prevents the marker from
                // briefly flashing back to the pre-dragged position if the user is taking a
                // long time to settle on where to drop.
                waypoints.find(AppPreferences.memberid)?.let {
                    it.marker.position = marker.position
                    viewmodel.saveWaypoint(it)
                }
            }

            override fun onMarkerDragEnd(marker: Marker) {
                waypoints.find(AppPreferences.memberid)?.let {
                    it.marker.position = marker.position
                    viewmodel.saveWaypoint(it)
                }
            }

            override fun onMarkerDragStart(marker: Marker) {
                waypoints.find(AppPreferences.memberid)?.let {
                    if (!it.isMine()) {
                        marker.isDraggable = false
                    }
                }
            }
        })

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
            cameraIsMoving = true
        }

        map.setOnCameraMoveCanceledListener {
            Log.w(TAG, "-=onCameraMoveCancelled: =-")
            cameraIsMoving = false
        }

        map.setOnCameraIdleListener {
            Log.w(TAG, "-=onCameraIdle: =-")
            cameraIsMoving = false
        }

        map.setOnMapLongClickListener { position ->
            Log.d(TAG, "-=:${AppPreferences.googleid} =-")
            val waypoint = waypoints.find(AppPreferences.memberid)
            if (waypoint != null) {
                waypoint.marker.position = position
                viewmodel.saveWaypoint(waypoint)
            } else {
                myMapMarker.locUpdate.displayName?.let {name ->
                    putWaypointOnMap(position, name, isMe = true)?.let { marker ->
                        val newWaypoint = Waypoints.Waypoint(marker, myMapMarker.locUpdate)
                        waypoints.addOrUpdate(newWaypoint)
                        viewmodel.saveWaypoint(newWaypoint)
                    }
                }
            }
        }

        if (!backgroundPermissionApproved()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundPermissions()
            }
        }

        // If we rely solely on the livedata we can end up in a weird position from time to time.
        moveCameraContextually()

    }

    private fun putWaypointOnMap(position: LatLng, name: String, isMe: Boolean) : Marker? {
        val bitmap = if (isMe) {
            Helpers.Bitmaps.getBitmapFromResource(context, waypoint_checkmark_red)
        } else {
            Helpers.Bitmaps.getBitmapFromResource(context, waypoint_checkmark_blue)
        }
        val markerOptions = MarkerOptions()
            .title("Waypoint by $name")
            .position(position)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            .draggable(isMe)

        return map.addMarker(markerOptions)
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

    private fun startObservingMyLocation() {

        // The tripcode will be null if the user clicks the leave trip button so we should
        // bail if that is the case.
        if (AppPreferences.tripCode == null) {
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

                myLocation.bearing?.let {
                    binding.txtDirection.text = MyGeoUtil.calculateCardinalDirection(it)
                    startDelayedDirectionWipe()
                }
                myLocation.speed?.let {
                    binding.txtSpeed.text = getString(
                        R.string.velocity_textview,
                        Helpers.Numbers.formatAsOneDecimalPointNumber(it.toDouble()).toString())
                    startDelayedVelocityWipe()
                }

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
                        .icon(BitmapDescriptorFactory.fromResource(my_map_icon_mini))

                    // Create a MemberMarker object to hold our location
                    myMapMarker = MapMarker(
                        map.addMarker(markerOptions)!!,
                        myLocation.toLocUpdate(AppPreferences.tripCode!!),
                        null,
                        null
                    )
                } // !this::myMapMarker.isInitialized

                // Update our marker's position on the map.
                myMapMarker.marker.position = position

                // Update all polylines
                mapMarkers.forEach { memberMarker ->
                    if (memberMarker.polyline != null) {
                        memberMarker.polyline?.remove()
                        memberMarker.polyline = drawPolyFromTo(myLocation.toLatLng(), memberMarker.locUpdate.toLatLng())
                    }
                }

                // Do maths on selected member if selected
                if (selectedMember.value != null) {
                    displayDistanceAndDirectionToSelectedMember()
                }

                var memberCount = 0
                viewmodel.memberLocations.value?.size?.let { memberCount = it }

                if (memberCount == 1 ) {
                    iAmAlone = true
                    cameraLockedOnMe = true
                    cameraLockedOnParty = false
                    cameraLockedOnMember = false
                } else if (memberCount > 1 && iAmAlone) {
                    iAmAlone = false
                    cameraLockedOnParty = true
                    cameraLockedOnMe = false
                    cameraLockedOnMember = false
                }
            } // MyLoc is not null

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

            // It's possible to to get member locs before the map or my marker has been initialized.
            // This is only really possible when joining an existing trip.  Shouldn't happen on new.
            if (this::myMapMarker.isInitialized) {
                drawMembersOnMap(memberList)
            }

            // Calculate metrics for selected member if applicable
            if (selectedMember.value != null) {
                // It is possible that the server has removed this user due to inactivity.
                if (memberList.size == 1 ) {
                    Log.i(TAG, "-=startObservingMemberLocations:CamLocked: $cameraLocked =-")
                    iAmAlone = true
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
                    cameraLockedOnMe = true
                    cameraLockedOnParty = false
                    cameraLockedOnMember = false
                } else if (memberList.size > 1 && iAmAlone) {
                    iAmAlone = false
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
                    binding.txtTripCode.text = AppPreferences.tripCode
                    startDelayWipeOfLogMsg1()
                }
            }
        }
    }

    private fun startObservingSelectedMember() {
        selectedMember.observe(viewLifecycleOwner) { selectedMember ->
            if (selectedMember != null) {
                displayDistanceAndDirectionToSelectedMember()
                binding.memberInfoContainer.slideVisibility(true, 750)
            } else {
                binding.memberInfoContainer.slideVisibility(false, 750)
            }
            moveCameraContextually()
        }
    }

    private fun drawMembersOnMap(memberList: List<LocUpdate>) {

        // Remove existing accuracy circles - they'll be recreated below.
        mapMarkers.removeAllCircles()

        // Loop through all member locations.
        memberList.forEach { memberLoc ->

            // Remove members that have left the trip but still have a marker
            removeZombieMarkers(memberList)

            // See if we already have a marker for this member
            when (val existingMarker = mapMarkers.findMarker(memberLoc)) {
                null -> { // Member does not have a marker yet.

                    // Prepare an accuracy circle if the member loc has an accuracy value.
                    var circle: Circle? = null
                    memberLoc.accuracy?.let {
                        circle = map.addCircle(buildCircle(it, memberLoc.toLatLng()))
                    }

                    // Set an initial map marker - gray for background, maroon for foreground.  This
                    // will be changed to their Google avatar later using Glide.
                    val bmap: BitmapDescriptor = if (memberLoc.isBg == 1) {
                        BitmapDescriptorFactory.fromResource(marker_gray)
                    } else {
                        BitmapDescriptorFactory.fromResource(marker_maroon)
                    }

                    // Add the marker to the map so we can get a reference to it
                    val actualMapMarker = map.addMarker(MarkerOptions().position(memberLoc.toLatLng()))

                    // It is possible to get a null marker when calling .addMarker so we check
                    actualMapMarker?.let {
                        // Build and add this marker to our member markers array
                        val marker = MapMarker(
                            it,
                            locUpdate = memberLoc,
                            polyline = null,
                            circle = circle,
                            isSelected = false,
                            avatar =  bmap
                        )
                        mapMarkers.add(marker)

                        // Asyncronously get and set the membermarker's avatar
                        setAvatarUsingGlide(marker)
                    }

                } // Member has a marker; update its location to move the existing map marker.
                else -> {
                    existingMarker.locUpdate = memberLoc
                    if (memberLoc.isBg == 1) {
                        // Member is in background so show a boring, gray marker.
                        existingMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(
                            marker_gray))
                    } else {
                        // Member is in foreground, use Glide to build a marker using their profile's avatar url.
                        if (existingMarker.avatar == null) {
                            existingMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(
                                marker_maroon))
                            // Asyncronously retrieve the member's avatar and set it in their member marker.
                            // It should then be available next time we get here.
                            setAvatarUsingGlide(existingMarker)
                        } else {
                            existingMarker.marker.setIcon(existingMarker.avatar)
                        }
                    }
                    existingMarker.marker.position = memberLoc.toLatLng()
                    memberLoc.accuracy?.let {
                        existingMarker.circle = map.addCircle(buildCircle(it, memberLoc.toLatLng()))
                    }
                }
            }

            // If the member has a waypoint we add it to or move it on the map.
            if (memberLoc.waypoint != null) {
                val waypointPosition = LatLng(memberLoc.waypoint!!)
                val existingWaypoint = waypoints.find(memberLoc.memberid)
                if (existingWaypoint == null) { // This is the first time we are seeing this waypoint.
                    memberLoc.displayName?.let { name ->
                        putWaypointOnMap(memberLoc.toLatLng(), name, isMe = memberLoc.isMe())?.let { marker ->
                            val waypoint = Waypoints.Waypoint(marker, memberLoc)
                            waypoints.addOrUpdate(waypoint)
                        }
                    }
                } else { // Waypoint is already on the map - just move it.

                    // If the waypoint is our waypoint we use the waypoint position stored in
                    // shared prefs instead of the server's position.  If the user has just moved
                    // the marker this will prevent the marker from bouncing between the old and
                    // new position.
                    if (memberLoc.isMe() && viewmodel.getWaypoint() != null) {
                        existingWaypoint.marker.position = viewmodel.getWaypoint()!!
                        existingWaypoint.marker.isDraggable = true
                    } else { // Not our waypoint - use the server's position.
                        existingWaypoint.marker.position = waypointPosition
                        existingWaypoint.marker.isDraggable = false
                    }
                } // waypoint exists already
            } // Server's locUpdate has a waypoint

            // Loop through the waypoints array and match waypoints to existing members.
            // If a waypoint is an orphan, remove it from the waypoints array.
            val toBeRemoved = ArrayList<Waypoints.Waypoint>()
            waypoints.forEach { waypoint ->
                val existingMember = memberList.findMarker(waypoint.locUpdate.memberid)
                if (existingMember == null) {
                    toBeRemoved.add(waypoint)
                }
            }
            // If there are waypoints to be removed, remove them.
            toBeRemoved.forEach {
                waypoints.remove(it)
            }

        } // for each member

    }

    /**
     * Removes markers of members that are no longer in the trip.
     */
    private fun removeZombieMarkers(memberList: List<LocUpdate>) {
        // Create a bucket to hold any zombie markers from users that have left the party
        val toBeRemoved = ArrayList<MapMarker>() // A list of the condemned
        // See if a marker lacks an actual active member
        mapMarkers.forEach {
            if (!memberList.containsMember(it.locUpdate.memberid)) {
                toBeRemoved.add(it)
            }
        }
        // Remove them from memberMarkers array (to avoid concurrency errors)
        toBeRemoved.forEach { mapMarkers.removeMarker(it) }
    }

    /**
     * Constructs a CircleOptions object to be used when adding a circle to the map.
     */
    private fun buildCircle(accuracy: Float, target: LatLng): CircleOptions {
        // Get a new circle started
        return CircleOptions()
            .fillColor(getCircleColor(accuracy.toDouble()))
            .strokeColor(getCircleColor(accuracy.toDouble()))
            .center(target)
            .radius(accuracy.toDouble())
    }

    private fun getCircleColor(accuracy: Double): Int {
        return if (accuracy > 20.0) {
            Color.parseColor("#25C10000")
        } else if (accuracy > 15.0) {
            Color.parseColor("#25FFFF00")
        }  else {
            Color.parseColor("#330022E2")
        }
    }

    /**
     * Uses Glide to make a network call obtaining the user's Google profile avatar and applies it
     * applies it to the supplied marker.  Not doing any of our own caching, trusting Glide to
     * handle it instead.
     */
    private fun setAvatarUsingGlide(marker: MapMarker) {

        Glide.with(this).load(marker.locUpdate.avatarUrl).listener(object : RequestListener<Drawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>?,
                isFirstResource: Boolean,
            ): Boolean {
                marker.avatar = BitmapDescriptorFactory.fromResource(marker_maroon)
                return false
            }
            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable?>?,
                dataSource: DataSource?,
                isFirstResource: Boolean,
            ): Boolean {
                // The raw bitmap from Glide
                val bitmap = Helpers.Bitmaps.getBitmapFromResource(resource)
                // Crop it to a circle
                val croppedBitmap = Helpers.Bitmaps.getCroppedBitmap(bitmap)
                // Convert it to a BitmapDescriptor for consumption by the Marker
                val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(croppedBitmap)
                marker.avatar = bitmapDescriptor
                return false
            }
        }).submit()
    }

    /**
     * Extension function to expand/contract a view (designed with layouts in mind)
     */
    private fun View.slideVisibility(visibility: Boolean, durationTime: Long) {
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
                    MyGeoUtil.calculateCardinalDirectionFromRadian(radian)
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
                    goToAppSettings()
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

    private fun goToAppSettings() {
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
     * Starts a runner that will clear the direction textview
     */
    private fun startDelayedDirectionWipe() {
        if (myDirectionWipeRunner != null) {
            try {
                myDirectionWipeHandler.removeCallbacks(myDirectionWipeRunner!!)
                myDirectionWipeHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        myDirectionWipeRunner = Runnable {
            // What runs each time
            binding.txtDirection.text = ""
            myDirectionWipeHandler.postDelayed(myDirectionWipeRunner!!, 3000)
        }

        // Starts it up initially
        myDirectionWipeHandler.postDelayed(myDirectionWipeRunner!!, 3000)
    }

    /**
     * Starts a runner that will clear the velocity textview
     */
    private fun startDelayedVelocityWipe() {
        if (myDirectionWipeRunner != null) {
            try {
                myVelocityWipeHandler.removeCallbacks(myVelocityWipeRunner!!)
                myVelocityWipeHandler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        myVelocityWipeRunner = Runnable {
            // What runs each time
            binding.txtSpeed.text = ""
            myVelocityWipeHandler.postDelayed(myVelocityWipeRunner!!, 3000)
        }

        // Starts it up initially
        myVelocityWipeHandler.postDelayed(myVelocityWipeRunner!!, 3000)
    }

    /**
     * Evaluates context and moves camera accordingly.
     */
    private fun moveCameraContextually() {

        if (!cameraLocked) {
            Log.i(TAG, "-=moveCameraContextually:Camera is currently free, cannot move it=-")
            return
        }

        if (!this::map.isInitialized) {
            Log.w(TAG, "moveCameraContextually: Map not ready!")
            return
        }

        map.stopAnimation()

        // Move camera based on context
        if (cameraLockedOnParty) {
            moveCameraToShowPartyAndWaypoints()
        } else {
            moveCameraToShowMe()
        }

    }

    /** Moves the camera to a position such that both the start and end map markers are viewable on screen.  */
    private fun moveCameraToShowPartyAndWaypoints() {

        if (viewmodel.memberLocations.value.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "Moving the camera to get all the markers in view")
        val cu: CameraUpdate

        // Create a new LatLngBounds.Builder object
        val builder = LatLngBounds.Builder()
        viewmodel.memberLocations.value?.forEach {
            builder.include(it.toLatLng())
            // Include waypoint if present if camera settings allow it
            if (cameraIncludeWaypoints) {
                it.waypoint?.let { waypoint ->
                    builder.include(LatLng(waypoint))
                }
            }
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
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 750,null)
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
        if (speedTravelling >= 3 && speedTravelling < 9) {
            setZoom = 19f
        }
        if (speedTravelling >= 9 && speedTravelling < 14) {
            setZoom = 18f
        }
        if (speedTravelling >= 14 && speedTravelling < 19) {
            setZoom = 17f
        }
        if (speedTravelling > 19) {
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

    private fun unselectMember() {

        binding.txtMemberInfo1.text = null
        // binding.txtMemberInfo2.text = null
        binding.txtMemberInfo3.text = null
        binding.txtMemberInfo4.text = null

        if (selectedMember.value == null) {
            return
        }

        val clickedMemberMarker = mapMarkers.findMarker(selectedMember.value!!.locUpdate)
        clickedMemberMarker?.removePolyline()
        mapMarkers.unselectAll()
        selectedMember.value = null

        // Tell the service to cease rigorous updates.
        viewmodel.requestVigorousUpdates(false)
    }

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

    /**
     * Extension function for creating a LatLng from a serialized LatLng.
     */
    fun LatLng(s: String): LatLng {
        return Gson().fromJson(s, LatLng::class.java)
    }


}




