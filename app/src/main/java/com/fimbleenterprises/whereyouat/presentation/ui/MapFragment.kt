package com.fimbleenterprises.whereyouat.presentation.ui

import android.content.*
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.model.MarkerPoly
import com.fimbleenterprises.whereyouat.model.MemberMarker
import com.fimbleenterprises.whereyouat.model.MemberMarkers
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.service.SharedPreferenceUtil
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.service.toText
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.model.*


class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap

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
        binding.btnPushUpdate.setOnClickListener {
            // viewmodel.getMemberLocationsFromApi()
        }
        binding.btnLeave.setOnClickListener {
            viewmodel.removeAllSavedLocs()
            Toast.makeText(context, "Cleared all entries.", Toast.LENGTH_SHORT).show()
        }
        binding.fabCenterTrip.setOnClickListener {
            moveCameraToShowMarkers()
        }
        tripUsersLocationManagementService?.subscribeToLocationUpdates()
        binding.fabCenterMe.setOnClickListener {
            centerCameraOnMe()
        }
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(requireContext(), TripUsersLocationManagementService::class.java)
        requireContext().bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
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
        viewmodel.memberLocations.observe(viewLifecycleOwner) { memberList ->

            memberList.forEach { member ->
                // Build a map marker for this member
                val position = LatLng(member.lat, member.lon)
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(member.memberid.toString())

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
                        // If the user has a poly line, we need to remove and redraw it
                        /*if (existingMember.polyline != null) {
                            existingMember.polyline?.remove()
                            existingMember.polyline = null
                            // Get our location from the viewmodel and draw a line to them.
                            viewmodel.myLocation.value?.let {
                                drawPolyFromTo(it.toLatLng(), existingMember.marker.position)
                            }
                        }*/
                    }
                }

                /*map.moveCamera(CameraUpdateFactory.newLatLng(marker))
                map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(LatLng(marker.latitude, marker.longitude), 16f)))*/
            }
        }
    }

    private fun startObservingMyLocation() {
        viewmodel.myLocation.observe(viewLifecycleOwner) {
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
                    myMapMarker = MemberMarker(
                        map.addMarker(markerOptions)!!,
                        it.toLocUpdate(),
                        null
                    )
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

    /** Moves the camera to a position such that both the start and end map markers are viewable on screen.  */
    private fun moveCameraToShowMarkers() {
        Log.d(TAG, "Moving the camera to get all the markers in view")
        val cu: CameraUpdate

        // Create a new LatLngBounds.Builder object
        val builder = LatLngBounds.Builder()
        viewmodel.memberLocations.value?.forEach {
            builder.include(it.toLatLng())
        }

        // TODO CRASH HERE IF YOU PRESS BUTTON EARLY.  MAP NOT READY?

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
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            Log.i(TAG, "-=MapFragment:onSharedPreferenceChanged  =-")
        }
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
                Log.i(TAG, "-=onReceive|Foreground location: ${location.toText()} =-")
            }
        }
    }

    init { Log.i(TAG, "Initialized:MapFragment") }
    companion object { private const val TAG = "FIMTOWN|MapFragment" }

}