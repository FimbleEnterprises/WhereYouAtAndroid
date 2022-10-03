package com.fimbleenterprises.whereyouat.model

data class ShareCodeAction(
    val tripcode: String,
    val message: String,
    val intentTag: String
)
