package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate
import kotlinx.coroutines.flow.Flow

class GetMemberLocsFromDbUseCase(private val mainRepository: MainRepository) {

    fun executeAsFlow(): Flow<List<LocUpdate>> = mainRepository.getAllMemberLocsFromDatabase()
    suspend fun executeOneTime(): List<LocUpdate> = mainRepository.getAllMemberLocsFromDatabaseOneTime()

}