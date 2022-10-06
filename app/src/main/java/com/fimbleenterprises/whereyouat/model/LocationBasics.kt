package com.fimbleenterprises.whereyouat.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.oAuthProvider

/**
 * Contract for classes that represent a location while providing some additional utility.
 */
abstract class LocationBasics {
    /**
     * Lattitude.
     */
    abstract val lat: Double

    /**
     * Longitude
     */
    abstract val lon: Double
    /**
     * Speed in meters per second.
     */
    abstract val speed: Float?

    /**
     * Accuracy in meters (+/-) from shown placement.
     */
    abstract val accuracy: Float?

    /**
     * The direction of travel in degrees (0 - 360)
     */
    abstract val bearing: Float?

    /**
     * Elevation above/below sea level in meters.
     */
    abstract val elevation: Double?

    /**
     * Converts this object to a [LatLng] (used with map markers for example)
     */
    fun toLatLng(): LatLng {
        return LatLng(this.lat, this.lon)
    }

    /**
     * Converts this object to a [Location] object.
     */
    fun toLocation(): Location {
        val location = Location("GPS")
        location.latitude = this.lat
        location.longitude = this.lon
        this.speed?.let { location.speed = it }
        this.bearing?.let { location.bearing = it }
        this.accuracy?.let { location.accuracy = it }
        return location
    }

    /**
     * Calculates the direction to the supplied point in degrees (0 - 360).
     */
    fun bearingTo(target: LocUpdate) : Float {
        return toLocation().bearingTo(target.toLocation())
    }

    /**
     * Calculates the direction to the supplied point in degrees (0 - 360).
     */
    fun bearingTo(target: Location) : Float {
        return toLocation().bearingTo(target)
    }

    fun bearingTo(target: LatLng) : Float {
        val loc = Location("GPS")
        loc.latitude = target.latitude
        loc.longitude = target.longitude
        return toLocation().bearingTo(loc)
    }
}