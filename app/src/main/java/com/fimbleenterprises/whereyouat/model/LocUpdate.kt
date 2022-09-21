package com.fimbleenterprises.whereyouat.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Represents any member's location.  This is the data type that is returned by the API for all
 * members.  Very similar to [MyLocation] however the latter class is created wholly client-side and
 * represents the user's location as determined by the device sensors. Both classes implement the
 * abstract class [LocationBasics] to provide unified shared properties (like lat, lon etc.)
 * as well as provide shared functionality (like [bearingTo], [toLocation] etc.)
 */
@Entity(
    tableName = "member_locations"
)
data class LocUpdate (
    // Primary key
    @PrimaryKey(autoGenerate = false)
    @SerializedName("Memberid")
    val memberid: Long,

    // Overridden from LocationBasics
    @SerializedName("Speed")
    override val speed: Float? = null,

    @SerializedName("Bearing")
    override val bearing: Float? = null,

    @SerializedName("Accuracy")
    override val accuracy: Float? = null,

    @SerializedName("Elevation")
    override val elevation: Double?,

    @SerializedName("Lat")
    override val lat: Double,

    @SerializedName("Lon")
    override val lon: Double,

    // The rest of the properties
    /**
     * Not implemented - may be used later to represent custom name instead of the displayname
     * from the user's Google account.
     */
    @SerializedName("MemberName")
    val memberName: String?,

    /**
     * When the loc update was created (in millis)
     */
    @SerializedName("Createdon")
    val createdon: Long,

    /**
     * The trip that this loc update is associated with.
     */
    @SerializedName("Tripcode")
    val tripcode: String,

    /**
     * The display name from their Google account.
     */
    @SerializedName("DisplayName")
    val displayName: String? = null,

    /**
     * User's Google id from their Google account.
     */
    @SerializedName("GoogleId")
    val googleid: String?,

    /**
     * The user's avatar url from their Google account profile.
     */
    @SerializedName("AvatarUrl")
    val avatarUrl: String? = null,

    /**
     * User's email address from their Google account profile.
     */
    @SerializedName("Email")
    val email: String? = null,

    /**
     * When the member's map is in the background this will be == 1.
     */
    @SerializedName("IsBg")
    val isBg: Int = 0,

    @SerializedName("Misc1")
    val misc1: String? = null

): LocationBasics() {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toString(): String {
        return "ID:${this.memberid} Lat/Lng:${lat}/${lon} Name: ${this.memberName} Tripcode: ${this.tripcode} "
    }
}

/**
 * Extension function to look for specific loc updates in the list
 */
fun List<LocUpdate>?.containsMember(locUpdate: LocUpdate): Boolean {
    this?.forEach {
        if (it.memberid == locUpdate.memberid) { return true }
    }
    return false
}

/**
 * Extension function to look for the supplied loc update in the list.
 */
fun List<LocUpdate>?.findMember(locUpdate: LocUpdate): LocUpdate? {
    this?.forEach {
        if (it.memberid == locUpdate.memberid) { return it }
    }
    return null
}