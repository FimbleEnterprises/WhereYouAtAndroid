package com.fimbleenterprises.whereyouat.service

import android.util.Log

/**
 * Used as an intermediary object in order to communicate events in real-time between our
 * Service and our ViewModel.
 * Adapted from this brilliant solution: https://github.com/uberchilly/BoundServiceMVVM
 */
class ServiceMessenger {

    interface ServiceListener {
        fun onServiceRunningChanged(state: ServiceRunningState)
    }

    // A list of subscribers to this manager
    private val connectionListenerListeners: HashSet<ServiceListener> = HashSet()

    /**
     * Subscribes a new listener for connection updates.
     */
    fun addConnectionListener(listener: ServiceListener) {
        connectionListenerListeners.add(listener)
    }

    /**
     * Removes a subscribed listener from the list.
     */
    fun removeConnectionListener(listener: ServiceListener) {
        if (connectionListenerListeners.contains(listener)) {
            connectionListenerListeners.remove(listener)
        }
    }

    /**
     * Sends the actual update containing the current state of the service to all subscribers.
     */
    private fun publishConnectionState(state: ServiceRunningState) {
        for (listener in connectionListenerListeners) {
            listener.onServiceRunningChanged(state)
        }
    }

    /**
     * This will set the ServiceRunningState to STARTING and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStarting() {
        publishConnectionState(ServiceRunningState.STARTING)
    }

    /**
     * This will set the ServiceRunningState to RUNNING and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStarted() {
        publishConnectionState(ServiceRunningState.RUNNING)
    }

    /**
     * This will set the ServiceRunningState to STOPPED and then call the internal publish method
     * to send it off to all subscribers.
     */
    fun serviceStopped() {
        publishConnectionState(ServiceRunningState.STOPPED)
    }

    init { Log.i(TAG, "Initialized:ServiceManager") }
    companion object { private const val TAG = "FIMTOWN|ServiceManager" }
}

sealed class ServiceRunningState {
    object STARTING : ServiceRunningState()
    object RUNNING : ServiceRunningState()
    object STOPPED : ServiceRunningState()
    init { Log.i(TAG, "Initialized:ServiceRunningState") }
    companion object { private const val TAG = "FIMTOWN|ServiceRunningState" }
}