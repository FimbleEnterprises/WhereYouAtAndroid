package com.fimbleenterprises.whereyouat.model

import android.util.Log

interface Request {
    val functionType: FunctionName
}

/**
 * Provides strongly-typed function names for calls to the API.
 */
enum class FunctionName {
    CREATE_NEW_TRIP,
    UPDATE_TRIP,
    LEAVE_TRIP;

    override fun toString(): String {
        return when (this) {
            CREATE_NEW_TRIP -> {
                ApiRequest.CREATE_NEW_TRIP
            }
            UPDATE_TRIP -> {
                ApiRequest.UPDATE_TRIP
            }
            LEAVE_TRIP -> {
                ApiRequest.LEAVE_TRIP
            }
        }
    }
}

data class ApiRequest(override val functionType: FunctionName): Request {

    private val function: String = functionType.toString()
    val arguments = ArrayList<Argument>()

    init {
        Log.v(TAG, "Initialized:ApiRequest")
    }

    companion object {
        private const val TAG = "FIMTOWN|ApiRequest"
        // OPERATIONS
        const val CREATE_NEW_TRIP = "createnewtrip"
        const val JOIN_TRIP = "jointrip"
        const val UPSERT_USER = "upsertuser"
        const val UPDATE_TRIP = "updatetrip"
        const val UPSERT_FCMTOKEN = "upsertfcmtoken"
        const val UPSERT_AVATAR = "upsertavatar"
        const val LOCATION_UPDATE_REQUESTED = "locationupdaterequested"
        const val LEAVE_TRIP = "leavetrip"
        const val REQUEST_JOIN = "requestjoin"
        const val TRIP_EXISTS = "tripexists"
        const val SEND_MESSAGE = "sendmsg"
        const val GET_TRIP_MESSAGES = "gettripmessages"
        const val TEST_FUNCTION = "TEST_FUNCTION"
        const val TEST_RESPONSE = "testresponse"
    }

}
