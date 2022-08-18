package com.fimbleenterprises.whereyouat.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.gson.annotations.SerializedName

@Entity(tableName = "my_location")
data class MyLocation(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("_id")
    val rowid: Int,
    @SerializedName("Createdon")
    val createdon: Long,
    @SerializedName("Elevation")
    val elevation: Any,
    @SerializedName("Lat")
    val lat: Double,
    @SerializedName("Lon")
    val lon: Double,
) {
    fun toLocUpdate(): LocUpdate {
        return LocUpdate(
            WhereYouAt.AppPreferences.memberid,
            WhereYouAt.AppPreferences.membername,
            System.currentTimeMillis(),
            elevation,
            lat,
            lon,
            WhereYouAt.AppPreferences.tripcode
        )
    }
}
