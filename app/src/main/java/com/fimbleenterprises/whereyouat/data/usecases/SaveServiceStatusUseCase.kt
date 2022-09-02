package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import com.fimbleenterprises.whereyouat.service.ServiceRunningState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class SaveServiceStatusUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(serviceStatus: ServiceStatus): Long =
        mainRepository.insertServiceStatus(serviceStatus)

    suspend fun setServiceRunning(isRunning: Boolean): Int =
        mainRepository.setServiceRunning(isRunning)

    suspend fun setServiceStarting(isStarting: Boolean): Int =
        mainRepository.setServiceStarting(isStarting)

    suspend fun setServiceStopping(isStopping: Boolean): Int =
        mainRepository.setServiceStopping(isStopping)

    suspend fun setServiceStatus(serviceStatus: ServiceStatus): Int =
        mainRepository.setServiceStatus(serviceStatus)

}