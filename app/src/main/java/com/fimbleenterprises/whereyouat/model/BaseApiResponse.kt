package com.fimbleenterprises.whereyouat.model


import com.google.gson.annotations.SerializedName

data class BaseApiResponse(
    @SerializedName("genericValue")
    val genericValue: String,
    @SerializedName("operation")
    val operation: String,
    @SerializedName("wasSuccessful")
    val wasSuccessful: Boolean
)