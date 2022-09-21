package com.fimbleenterprises.whereyouat.model

/**
 * Simple class to relay API success/failure from the viewmodel to observers (startFragment for sure)
 */
data class ApiEvent (
    val event: Event?,
    val msg: String? = null
) {

    override fun toString(): String {
        return "Event: ${this.event?.name}, Msg: $msg"
    }

    enum class Event {
        REQUEST_FAILED,
        REQUEST_SUCCEEDED
    }
}