package com.fimbleenterprises.whereyouat.model


import com.google.gson.annotations.SerializedName

data class BaseApiResponse(
    @SerializedName("GenericValue")
    val genericValue: String,
    @SerializedName("Operation")
    val operation: String,
    @SerializedName("WasSuccessful")
    val wasSuccessful: Boolean
)