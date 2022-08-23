package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class SaveServiceStatusUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(serviceStatus: ServiceStatus): Long =
        mainRepository.insertServiceStatus(serviceStatus)

}