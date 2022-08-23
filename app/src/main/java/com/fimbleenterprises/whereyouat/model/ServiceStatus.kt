package com.fimbleenterprises.whereyouat.model


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
    val isRunning: Boolean,
    @SerializedName("startedAt")
    val startedAt: Long = System.currentTimeMillis(),
    @SerializedName("isStarting")
    val isStarting: Boolean,
    @SerializedName("tripcode")
    val tripcode: String?
) {
    constructor(isStarting: Boolean, isRunning: Boolean, tripcode: String?) : this(1, isRunning, System.currentTimeMillis(), isStarting, tripcode)
}