package com.fimbleenterprises.whereyouat.presentation.ui

import android.content.*
import android.location.Location
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
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.model.MemberMarker
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.service.TripUsersLocationManagementService
import com.fimbleenterprises.whereyouat.service.SharedPreferenceUtil
import com.fimbleenterprises.whereyouat.service.toText
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.lang.reflect.Member

class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap

    private lateinit var myMapMarker: Marker
    private val memberMarkers = ArrayList<MemberMarker>()

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
        tripUsersLocationManagementService?.subscribeToLocationUpdates()
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
        viewmodel.memberLocations.observe(viewLifecycleOwner) {
            // map.clear()
            memberMarkers.forEach { memberMarker ->
                memberMarker.marker.remove()
            }
            memberMarkers.clear()

            it.forEach { loc ->
                val position = LatLng(loc.lat, loc.lon)
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(loc.memberid.toString())
                memberMarkers.add(
                    MemberMarker(
                        map.addMarker(markerOptions)!!,
                        loc
                    )
                )
                /*map.moveCamera(CameraUpdateFactory.newLatLng(marker))
                map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(LatLng(marker.latitude, marker.longitude), 16f)))*/
            }
        }
    }

    private fun startObservingMyLocation() {
        viewmodel.myLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                val position = LatLng(it.lat, it.lon)
                if (!this::myMapMarker.isInitialized) {
                    Log.i(TAG, "-=MapFragment:startObservingMyLocation  =-")
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(
                                LatLng(it.lat, it.lon), 16f
                            )
                        )
                    )
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title("Me")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_map_icon))
                    myMapMarker = map.addMarker(markerOptions)!!
                }
                myMapMarker.position = position
            }
        }
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
                Log.i(TAG, "-=ForegroundOnlyBroadcastReceiver:onReceive|Foreground location: ${location.toText()} =-")
            }
        }
    }

    init { Log.i(TAG, "Initialized:MapFragment") }
    companion object { private const val TAG = "FIMTOWN|MapFragment" }

}