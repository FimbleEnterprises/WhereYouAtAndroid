package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.model.Waypoints
import com.fimbleenterprises.whereyouat.service.toJson

class SaveWaypointPositionUseCase() {

    fun execute(waypoint: Waypoints.Waypoint) {
        WhereYouAt.AppPreferences.waypointPosition = waypoint.marker.position
    }

}