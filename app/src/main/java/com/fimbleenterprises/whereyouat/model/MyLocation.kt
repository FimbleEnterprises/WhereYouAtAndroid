package com.fimbleenterprises.whereyouat.model

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.google.android.gms.maps.model.LatLng
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

    fun toLatLng() : LatLng {
        return LatLng(lat, lon)
    }

    fun toLocation(): Location {
        val location = Location("GPS")
        location.latitude = this.lat
        location.longitude = this.lon
        return location
    }
}
