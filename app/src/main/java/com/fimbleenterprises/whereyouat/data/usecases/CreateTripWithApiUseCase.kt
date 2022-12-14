package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

class CreateTripWithApiUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(memberid: Long):
            Flow<Resource<BaseApiResponse>> = mainRepository.createTripInApi(memberid)

}