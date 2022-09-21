package com.fimbleenterprises.whereyouat.model


import com.google.gson.annotations.SerializedName

data class MemberLocationsApiResponse(
    @SerializedName("GenericValue")
    val value: Any,

    @SerializedName("MemberLocations")
    val locUpdates: List<LocUpdate>,

    @SerializedName("Operation")
    val operation: String,

    @SerializedName("WasSuccessful")
    val wasSuccessful: Boolean
)