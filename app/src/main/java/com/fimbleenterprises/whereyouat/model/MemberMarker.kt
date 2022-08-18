package com.fimbleenterprises.whereyouat.model

import com.google.android.gms.maps.model.Marker

data class MemberMarker(
    val marker: Marker,
    val locUpdate: LocUpdate
)