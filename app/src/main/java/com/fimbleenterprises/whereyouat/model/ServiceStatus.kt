package com.fimbleenterprises.whereyouat.model

data class ServiceStatus(
    var isRequestingMemberLocs: Boolean = false,
    var isUploadingMyLoc: Boolean = false,
    var isRigorouslyRequestingLocations: Boolean = false,
    var log1: String? = null,
    var locationInterval: Long = 1L,
    var requestInterval: Long = 15L
)