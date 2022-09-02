package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import kotlinx.coroutines.flow.Flow

class GetServiceStatusUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(): ServiceStatus =
        mainRepository.getServiceStatus()

    fun executeFlow(): Flow<ServiceStatus> =
        mainRepository.getServiceStatusFlow()

}