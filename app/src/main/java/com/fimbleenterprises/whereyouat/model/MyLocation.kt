package com.fimbleenterprises.whereyouat.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.google.gson.annotations.SerializedName

/**
 * Class represents the user's location only.  It can (and often is) converted to it's sibling
 * object [LocUpdate] which is a class representing any member's location.  Both classes implement
 * the abstract class [LocationBasics] to provide unified shared properties (like lat, lon etc.)
 * as well as provide shared functionality (like [bearingTo], [toLocation] etc.)
 */
@Entity(tableName = "my_location")
data class MyLocation (
    @PrimaryKey(autoGenerate = false)
    @SerializedName("_id")
    val rowid: Int = 0,

    @SerializedName("Createdon")
    val createdon: Long,

    @SerializedName("Elevation")
    override val elevation: Double? = 0.0,

    @SerializedName("Lat")
    override val lat: Double,

    @SerializedName("Lon")
    override val lon: Double,

    @SerializedName("Speed")
    override val speed: Float? = null,

    @SerializedName("Bearing")
    override val bearing: Float? = null,

    @SerializedName("Accuracy")
    override val accuracy: Float? = null,

    @SerializedName("IsBg")
    var isBg: Int = 0
): LocationBasics() {
    fun toLocUpdate(tripcode: String): LocUpdate {
        return LocUpdate(
            memberid = AppPreferences.memberid,
            memberName = AppPreferences.membername,
            createdon = System.currentTimeMillis(),
            elevation = elevation,
            lat = lat,
            lon = lon,
            tripcode = tripcode,
            accuracy = accuracy,
            bearing = bearing,
            speed = speed,
            googleid = AppPreferences.googleid,
            displayName = AppPreferences.name,
            email = AppPreferences.email,
            avatarUrl = AppPreferences.avatarUrl,
            isBg = isBg
        )
    }
    fun toLocUpdate(): LocUpdate {
        return LocUpdate(
            memberid = AppPreferences.memberid,
            memberName = AppPreferences.membername,
            createdon = System.currentTimeMillis(),
            elevation = elevation,
            lat = lat,
            lon = lon,
            tripcode = AppPreferences.tripCode!!,
            accuracy = accuracy,
            bearing = bearing,
            speed = speed,
            googleid = AppPreferences.googleid,
            displayName = AppPreferences.name,
            email = AppPreferences.email,
            avatarUrl = AppPreferences.avatarUrl,
            isBg = isBg
        )
    }
}
