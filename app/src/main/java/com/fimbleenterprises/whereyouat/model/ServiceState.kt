package com.fimbleenterprises.whereyouat.model


import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "service_state"
)
/**
 * To prevent very difficult to debug [ServiceState.state] issues, these use cases shall ONLY be
 * called by either the viewmodel or the service.  Do not call from fragments!
 * Moreover, the service shall be solely responsible for setting some states and viewmodel solely
 * responsible for setting others.
 *
 * &nbsp;
 *
 * [TripUsersLocationManagementService] shall be solely responsible for setting [ServiceState.state]
 * to the following values:
 *
 * &nbsp;
 *
 * [ServiceState.SERVICE_STATE_STOPPED]
 *
 * [ServiceState.SERVICE_STATE_RUNNING]
 *
 * &nbsp;
 *
 * These values should never be set by viewmodel.
 *
 * &nbsp;
 *
 * [MainViewModel] is solely responsible for setting [ServiceState.state]
 * to the following values:
 *
 * &nbsp;
 *
 * [ServiceState.SERVICE_STATE_STARTING]
 *
 * [ServiceState.SERVICE_STATE_STOPPING]
 *
 * &nbsp;
 *
 * These values should never be set by the service.
 *
 * &nbsp;
 *
 */
data class ServiceState(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("rowid")
    val rowid: Long = 1,
    @SerializedName("state")
    var state: Int = SERVICE_STATE_IDLE
) {
    constructor(serviceState: Int): this(state = serviceState)

    private fun stateToString(): String {
        when (this.state) {
            SERVICE_STATE_RUNNING -> { return "RUNNING" }
            SERVICE_STATE_STARTING -> { return "STARTING" }
            SERVICE_STATE_STOPPING -> { return "STOPPING" }
            SERVICE_STATE_STOPPED -> { return "STOPPED" }
            SERVICE_STATE_RESTART -> { return "RESTART" }
            SERVICE_STATE_IDLE -> { return "UNKNOWN" }
        }
        return ""
    }

    override fun toString(): String {
        return "Service state: ${stateToString()}"
    }

    init { Log.i(TAG, "Initialized:ServiceStatus") }
    companion object {
        private const val TAG = "FIMTOWN|ServiceStatus"
        const val SERVICE_STATE_STARTING = 1
        const val SERVICE_STATE_STOPPING = 2
        const val SERVICE_STATE_RESTART = 3
        const val SERVICE_STATE_RUNNING = 0
        const val SERVICE_STATE_STOPPED = 4
        const val SERVICE_STATE_IDLE = 5
    }
}