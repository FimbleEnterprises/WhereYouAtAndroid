package com.fimbleenterprises.whereyouat.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "member_locations"
)
data class LocUpdate(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("Memberid")
    val memberid: Long,
    @SerializedName("MemberName")
    val memberName: String?,
    @SerializedName("Createdon")
    val createdon: Long,
    @SerializedName("Elevation")
    val elevation: Any,
    @SerializedName("Lat")
    val lat: Double,
    @SerializedName("Lon")
    val lon: Double,
    @SerializedName("Tripcode")
    val tripcode: String
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toString(): String {
        return "ID:${this.memberid} Lat/Lng:${lat}/${lon} Name: ${this.memberName} Tripcode: ${this.tripcode} "
    }
}