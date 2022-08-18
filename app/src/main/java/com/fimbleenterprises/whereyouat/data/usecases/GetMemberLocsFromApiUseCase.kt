package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

class GetMemberLocsFromApiUseCase(private val mainRepository: MainRepository) {
    suspend fun execute(tripcode: String): Flow<Resource<MemberLocationsApiResponse>> = mainRepository.getAllMemberLocationsFromApi(tripcode)
}