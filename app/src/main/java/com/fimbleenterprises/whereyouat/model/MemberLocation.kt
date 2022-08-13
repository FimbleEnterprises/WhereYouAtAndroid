package com.fimbleenterprises.whereyouat.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "member_locations"
)
data class MemberLocation(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("Memberid")
    val memberid: Double,
    @SerializedName("Createdon")
    val createdon: String,
    @SerializedName("Elevation")
    val elevation: Any,
    @SerializedName("Json")
    val json: String,
    @SerializedName("Lat")
    val lat: Any,
    @SerializedName("Lon")
    val lon: Any,
    @SerializedName("Tripcode")
    val tripcode: String
)