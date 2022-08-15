package com.fimbleenterprises.whereyouat.presentation.ui

import android.content.ClipData
import android.content.SharedPreferences
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.DragStartHelper
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.databinding.FragmentMapBinding
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.service.ForegroundOnlyLocationService
import com.fimbleenterprises.whereyouat.service.SharedPreferenceUtil

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding
    private lateinit var viewmodel: MainViewModel
    private lateinit var map: GoogleMap
    private lateinit var foregroundOnlyLocationService: ForegroundOnlyLocationService
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        foregroundOnlyLocationService = (activity as MainActivity).foregroundOnlyLocationService!!
        sharedPreferences = (activity as MainActivity).sharedPreferences
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)
        viewmodel = (activity as MainActivity).mainViewModel
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.btnPushUpdate.setOnClickListener {
            viewmodel.uploadMyLocation()
            viewmodel.getMemberLocations()
        }

        binding.btnLeave.setOnClickListener {
            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                foregroundOnlyLocationService?.subscribeToLocationUpdates()
            // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
                /*if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
                } else {
                    requestForegroundPermissions()
                }*/
            }
        }
    }

    private fun startObservingMemberLocations() {
        viewmodel.memberLocationsApiResponse.observe(viewLifecycleOwner) {
            it.data?.locUpdates?.forEach { loc ->
                val marker = LatLng(loc.lat, loc.lon)
                map.addMarker(MarkerOptions().position(marker).title(loc.memberid.toString()))
                map.moveCamera(CameraUpdateFactory.newLatLng(marker))
                map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(LatLng(marker.latitude, marker.longitude), 16f)))
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
    }

    init { Log.i(TAG, "Initialized:MapFragment") }
    companion object { private const val TAG = "FIMTOWN|MapFragment" }
}