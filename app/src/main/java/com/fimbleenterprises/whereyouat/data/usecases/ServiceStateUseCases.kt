package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow

/**
 * To prevent very difficult to debug [ServiceState.state] issues, these use cases shall ONLY be
 * called by either the viewmodel or the service.  Do not call from fragments!
 * Moreover, the service shall be solely responsible for setting some states and viewmodel solely
 * responsible for setting others.
 *
 * &nbsp;
 *
 * [TripUsersLocationManagementService] shall be solely responsible for setting [ServiceState.state]
 * to the following values:
 *
 * &nbsp;
 *
 * [ServiceState.SERVICE_STATE_STOPPED]
 *
 * [ServiceState.SERVICE_STATE_RUNNING]
 *
 * &nbsp;
 *
 * These values should never be set by viewmodel.
 *
 * &nbsp;
 *
 * [MainViewModel] is solely responsible for setting [ServiceState.state]
 * to the following values:
 *
 * &nbsp;
 *
 * [ServiceState.SERVICE_STATE_STARTING]
 *
 * [ServiceState.SERVICE_STATE_STOPPING]
 *
 * &nbsp;
 *
 * These values should never be set by the service.
 *
 * &nbsp;
 *
 */
class ServiceStateUseCases(private val mainRepository: MainRepository) {

    suspend fun setServiceStarting(): Int =
        mainRepository.setServiceStarting()

    suspend fun setServiceRunning(): Int =
        mainRepository.setServiceRunning()

    suspend fun setServiceStopping(): Int =
        mainRepository.setServiceStopping()

    suspend fun setServiceStopped(): Int =
        mainRepository.setServiceStopped()

    suspend fun setServiceRestarting(): Int =
        mainRepository.setServiceRestarting()

    fun getServiceStateAsFlow(): Flow<ServiceState> =
        mainRepository.getServiceStateAsFlow()

    suspend fun getServiceState(): ServiceState =
        mainRepository.getServiceState()

    suspend fun setServiceState(serviceState: ServiceState): Long =
        mainRepository.saveServiceState(serviceState)

}