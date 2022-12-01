package com.fimbleenterprises.whereyouat.model

import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

class Waypoints: ArrayList<Waypoints.Waypoint>() {

    /**
     * Adds the supplied waypoint as a new item in the array or updates
     * the existing waypoint in the array with the same creator value.
     */
    fun addOrUpdate(waypoint: Waypoint) {
        var existing = find(waypoint.owner)
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
            if (it.owner == creator) { waypoint = it }
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
        var owner: Long,
        var polyline: Polyline? = null
    ) {

        fun setPosition(position: LatLng) {
            this.marker.position = position
        }

        fun remove() {
            marker.remove()
        }

        fun isMine(): Boolean {
            return this.owner == WhereYouAt.AppPreferences.memberid
        }

        /**
         * Calls the Polyline's remove() method to remove it from the map and nulls
         * out the polyline property of this class.
         */
        fun removePolyline() {
            this.polyline?.remove()
            this.polyline = null
        }

        fun toMapMarker(): MapMarkers.MapMarker {

            val locUpdate = LocUpdate(
                memberid = owner,
                speed = 0f,
                bearing = 0f,
                accuracy = 0f,
                elevation = 0.0,
                lat = marker.position.latitude,
                lon = marker.position.longitude,
                memberName = null,
                createdon = System.currentTimeMillis(),
                tripcode = WhereYouAt.AppPreferences.tripCode!!,
                displayName = null,
                googleid = null,
                avatarUrl = null,
                email = null,
                isBg = 0,
                waypoint = null
            )

            return MapMarkers.MapMarker(
                marker = this.marker,
                locUpdate = locUpdate,
                polyline = polyline,
                circle = null,
                isSelected = false,
                avatar = null
            )
        }

    }
}
