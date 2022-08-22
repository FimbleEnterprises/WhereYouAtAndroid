package com.fimbleenterprises.whereyouat.model

import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

/**
 * Class to tightly correlate map markers to loc updates.
 */
data class MemberMarker(
    val marker: Marker,
    var locUpdate: LocUpdate,
    var polyline: Polyline?
) {
}

data class MarkerPoly(
    val targetMarker: MemberMarker,
    var polyline: Polyline
)

/**
 * Container for MemberMarker objects with utility functions.
 */
class MemberMarkers : ArrayList<MemberMarker>() {

    fun find(locUpdate: LocUpdate): MemberMarker? {
        this.forEach {
            if (locUpdate.memberid == it.locUpdate.memberid) {
                return it
            }
        }
        return null
    }

    fun find(marker: Marker): MemberMarker? {
        this.forEach {
            if (marker.id == it.marker.id) {
                return it
            }
        }
        return null
    }
}