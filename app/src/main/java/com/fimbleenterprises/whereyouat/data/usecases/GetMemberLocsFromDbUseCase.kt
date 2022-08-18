package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate
import kotlinx.coroutines.flow.Flow

class GetMemberLocsFromDbUseCase(private val mainRepository: MainRepository) {

    fun execute(): Flow<List<LocUpdate>> = mainRepository.getAllMemberLocsFromDatabase()

}