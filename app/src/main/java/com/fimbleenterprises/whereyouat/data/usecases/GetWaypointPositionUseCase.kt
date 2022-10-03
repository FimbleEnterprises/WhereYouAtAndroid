package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.LatLng

class GetWaypointPositionUseCase() {

    fun execute(): LatLng? {
        return WhereYouAt.AppPreferences.waypointPosition
    }

}