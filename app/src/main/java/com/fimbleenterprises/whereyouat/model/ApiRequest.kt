package com.fimbleenterprises.whereyouat.model

import android.util.Log

data class ApiRequest(val function: String) {

    val arguments = ArrayList<Argument>()

    init {
        Log.i(TAG, "Initialized:ApiRequest")
    }

    companion object {
        private const val TAG = "FIMTOWN|ApiRequest"
        // OPERATIONS
        const val CREATE_NEW_TRIP = "createnewtrip";
        const val JOIN_TRIP = "jointrip";
        const val UPSERT_USER = "upsertuser";
        const val UPDATE_TRIP = "updatetrip";
        const val UPSERT_FCMTOKEN = "upsertfcmtoken";
        const val UPSERT_AVATAR = "upsertavatar";
        const val LOCATION_UPDATE_REQUESTED = "locationupdaterequested";
        const val LEAVE_TRIP = "leavetrip";
        const val REQUEST_JOIN = "requestjoin";
        const val TRIP_EXISTS = "tripexists";
        const val SEND_MESSAGE = "sendmsg";
        const val GET_TRIP_MESSAGES = "gettripmessages";
        const val TEST_FUNCTION = "TEST_FUNCTION";
        const val TEST_RESPONSE = "testresponse";
    }

}
