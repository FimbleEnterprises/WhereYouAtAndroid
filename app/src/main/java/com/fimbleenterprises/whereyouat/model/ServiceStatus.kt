package com.fimbleenterprises.whereyouat.model


import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "service_status"
)
data class ServiceStatus(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("rowid")
    val rowid: Long = 1,
    @SerializedName("isRunning")
    var isRunning: Boolean = false,
    @SerializedName("startedAt")
    val startedAt: Long = System.currentTimeMillis(),
    @SerializedName("isStarting")
    var isStarting: Boolean = false,
    @SerializedName("tripcode")
    var tripcode: String? = null,
    @SerializedName("forceupdate")
    val forceUpdate: Boolean = false,
    @SerializedName("onetimemessage")
    val oneTimeMessage: String? = null,
    @SerializedName("isStopping")
    var isStopping: Boolean = false,
    @SerializedName("locationrequeststate")
    var locationRequestState: Int = LOCATION_STATE_RUNNING
) {
    constructor(locationRequest: Int) : this(locationRequestState = locationRequest)
    constructor(isStarting: Boolean, isRunning: Boolean) : this(1, isRunning, System.currentTimeMillis(), isStarting)
    constructor(isStarting: Boolean, isRunning: Boolean, tripcode: String?) : this(1, isRunning, System.currentTimeMillis(), isStarting, tripcode)
/*    constructor(isStarting: Boolean, isRunning: Boolean, forceUpdate: Boolean) : this(1, isRunning, System.currentTimeMillis(), isStarting, forceUpdate)
    constructor(isStarting: Boolean, isRunning: Boolean, forceUpdate: Boolean, oneTimeMessage: String?) : this(1, isRunning, System.currentTimeMillis(), isStarting, forceUpdate, oneTimeMessage)*/

    override fun toString(): String {
        return "isRunning:$isRunning, isStarting:$isStarting, forceupdate:$forceUpdate"
    }

    init { Log.i(TAG, "Initialized:ServiceStatus") }
    companion object {
        private const val TAG = "FIMTOWN|ServiceStatus"
        const val LOCATION_STATE_RUNNING = 0
        const val LOCATION_STATE_STARTING = 1
        const val LOCATION_STATE_STOPPING = 2
        const val LOCATION_STATE_RESTART = 3

    }
}