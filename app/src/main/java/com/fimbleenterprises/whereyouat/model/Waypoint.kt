package com.fimbleenterprises.whereyouat.model

import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

class Waypoints(): ArrayList<Waypoints.Waypoint>() {

    // Removes the marker from the map and array.
    fun removeAll() {
        this.forEach {
            it.marker.remove()
            it.remove()
        }
    }

    /**
     * Adds the supplied waypoint as a new item in the array or updates
     * the existing waypoint in the array with the same creator value.
     */
    fun addOrUpdate(waypoint: Waypoint) {
        var existing = find(waypoint.locUpdate.memberid)
        if (existing != null) {
            existing = waypoint
        } else {
            this.add(waypoint)
        }
    }

    /**
     * Removes all markers from the map (does not remove from array)
     */
    fun clearAll() {
        this.forEach { it.marker.remove() }
    }

    fun clearAllPolylines() {
        this.forEach {
            it.polyline?.remove()
        }
    }

    /**
     * Finds a waypoint in the array by comparing memberid
     */
    fun find(creator: Long): Waypoint? {
        var waypoint: Waypoint? = null
        this.forEach {
            if (it.locUpdate.memberid == creator) { waypoint = it }
        }
        return waypoint
    }

    /**
     * Finds a waypoint in the array by comparing memberid
     */
    fun find(marker: Marker): Waypoint? {
        var waypoint: Waypoint? = null
        this.forEach {
            if (it.marker == marker) { waypoint = it }
        }
        return waypoint
    }

    data class Waypoint(
        var marker: Marker,
        var locUpdate: LocUpdate,
        var polyline: Polyline? = null
    ) {

        fun setPosition(position: LatLng) {
            this.marker.position = position
        }

        fun remove() {
            marker.remove()
        }

        fun isMine(): Boolean {
            return this.locUpdate.memberid == WhereYouAt.AppPreferences.memberid
        }

        /**
         * Calls the Polyline's remove() method to remove it from the map and nulls
         * out the polyline property of this class.
         */
        fun removePolyline() {
            this.polyline?.remove()
            this.polyline = null
        }

    }
}
