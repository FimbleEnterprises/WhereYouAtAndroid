package com.fimbleenterprises.whereyouat.model

import android.util.Log
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.*

/**
 * Container for MemberMarker objects with utility functions.
 */
class MapMarkers : ArrayList<MapMarkers.MapMarker>() {

    init { Log.i(TAG, "Initialized:MemberMarkers") }
    companion object { private const val TAG = "FIMTOWN|MemberMarkers" }

    /**
     * Iterates over this array returning the matching marker.  Returns null if not found.
     */
    fun findMarker(locUpdate: LocUpdate): MapMarker? {
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
    fun findMarker(marker: Marker): MapMarker? {
        this.forEach {
            if (marker.id == it.marker.id) {
                return it
            }
        }
        return null
    }

    /**
     * Iterates over this array returning the matching marker.  Returns null if not found.
     */
    fun findMarker(memberid: Long): MapMarker? {
        this.forEach {
            if (memberid == it.locUpdate.memberid) {
                return it
            }
        }
        return null
    }

    /**
     * Returns the index of the selected member marker.  If no marker is selected
     * will return -1
     */
    fun findSelectedIndex() : Int {
        for (i in 0 until this.size) {
            if (this[i].isSelected) {
                return i
            }
        }
        return -1
    }

    /**
     * Calls unselectAll() then sets this marker's isSelected property to true,
     * finally it shows its info window on the map.
     */
    fun selectMember(mapMarker: MapMarker) {
        unselectAll()
        this.forEach {
            if (it == mapMarker) {
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
    fun removeMarker(marker: MapMarker) {
        marker.marker.remove()
        this.remove(marker)
    }

    /**
     * Class to tightly correlate map markers to loc updates.
     */
    data class MapMarker(
        var marker: Marker,
        var locUpdate: LocUpdate,
        var polyline: Polyline?,
        var circle: Circle?,
        var isSelected: Boolean = false,
        var avatar: BitmapDescriptor? = null,
    ) {

        var isMe: Boolean
            get() {
                return locUpdate.googleid == WhereYouAt.AppPreferences.googleid
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