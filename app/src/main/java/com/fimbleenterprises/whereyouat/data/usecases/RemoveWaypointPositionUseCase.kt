package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.WhereYouAt

class RemoveWaypointPositionUseCase {
    fun execute() {
        WhereYouAt.AppPreferences.waypointPosition = null
    }
}