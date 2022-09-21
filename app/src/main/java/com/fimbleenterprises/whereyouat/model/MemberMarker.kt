package com.fimbleenterprises.whereyouat.model

import android.util.Log
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

/**
 * Container for MemberMarker objects with utility functions.
 */
class MemberMarkers : ArrayList<MemberMarkers.MemberMarker>() {

    init { Log.i(TAG, "Initialized:MemberMarkers") }
    companion object { private const val TAG = "FIMTOWN|MemberMarkers" }

    /**
     * Iterates over this array returning the matching marker.  Returns null if not found.
     */
    fun findMember(locUpdate: LocUpdate): MemberMarker? {
        this.forEach {
            if (locUpdate.memberid == it.locUpdate.memberid) {
                return it
            }
        }
        return null
    }

    /**
     * Iterates over this array returning the matching marker.  Returns null if not found.
     */
    fun findMember(marker: Marker): MemberMarker? {
        this.forEach {
            if (marker.id == it.marker.id) {
                return it
            }
        }
        return null
    }

    /**
     * Calls unselectAll() then sets this marker's isSelected property to true and
     * finally it shows its info window on the map.
     */
    fun selectMember(memberMarker: MemberMarker) {
        unselectAll()
        this.forEach {
            if (it == memberMarker) {
                it.isSelected = true
                it.marker.showInfoWindow()
            } else {
                Log.i(TAG, "-=selectMember: =-")
            }
        }
    }

    /**
     * This calls removeCircle() on every member marker in the array.
     */
    fun removeAllCircles() {
        this.forEach {
            it.removeCircle()
        }
    }

    /**
     * Sets isSelected property to false, removes the polyline
     * and hides any info window for all items in the list.
     */
    fun unselectAll() {
        this.forEach {
            it.isSelected = false
            it.marker.hideInfoWindow()
            it.removePolyline()
        }
    }

    /**
     * Calls the Marker class' remove() method to remove it from the map, then calls the
     * ArrayList's remove() method.
     */
    fun removeMarker(marker: MemberMarker) {
        marker.marker.remove()
        this.remove(marker)
    }

    /**
     * Class to tightly correlate map markers to loc updates.
     */
    data class MemberMarker(
        var marker: Marker,
        var locUpdate: LocUpdate,
        var polyline: Polyline?,
        var circle: Circle?,
        var isSelected: Boolean = false
    ) {

        var isMe: Boolean
            get() {
                return locUpdate.memberid == WhereYouAt.AppPreferences.memberid
            }
            set(value) {}

        /**
         * Calls the Polyline's remove() method to remove it from the map and nulls
         * out the polyline property of this class.
         */
        fun removePolyline() {
            this.polyline?.remove()
            this.polyline = null
        }
        /**
         * Calls the Circle's remove() method to remove it from the map and nulls
         * out the circle property of this class.
         */
        fun removeCircle() {
            this.circle?.remove()
            this.circle = null
        }

    }

}