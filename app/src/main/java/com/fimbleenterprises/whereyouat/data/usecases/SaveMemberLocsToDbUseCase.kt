package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate

class SaveMemberLocsToDbUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(locUpdate: LocUpdate): Long = mainRepository.saveMemberLocationToDatabase(locUpdate)
    suspend fun executeMany(locUpdates: List<LocUpdate>): List<Long> = mainRepository.saveMemberLocationsToDatabase(locUpdates)

}