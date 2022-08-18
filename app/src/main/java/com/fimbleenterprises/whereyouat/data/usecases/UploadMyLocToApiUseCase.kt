package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

class UploadMyLocToApiUseCase(private val mainRepository: MainRepository) {
    suspend fun execute(locUpdate: LocUpdate):
            Flow<Resource<BaseApiResponse>> = mainRepository.uploadMyLocationToApi(locUpdate)
}