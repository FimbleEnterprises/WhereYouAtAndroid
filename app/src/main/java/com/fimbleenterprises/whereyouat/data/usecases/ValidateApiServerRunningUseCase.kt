package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

class ValidateApiServerRunningUseCase(private val mainRepository: MainRepository) {

    suspend fun execute():
        Flow<Resource<BaseApiResponse>> = mainRepository.validateApiServerRunning()
}