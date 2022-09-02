package com.fimbleenterprises.whereyouat.utils

/**
 * Validates that a trip code adheres to constraints before being submitted to the API
 */
interface TripCodeClientSideValidator {
    fun isValid(tripcode: String): Boolean
}
