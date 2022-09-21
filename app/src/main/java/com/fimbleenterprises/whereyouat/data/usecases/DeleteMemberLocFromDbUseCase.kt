package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.LocUpdate

class DeleteMemberLocFromDbUseCase(private val mainRepository: MainRepository) {
    suspend fun execute(memberLoc: LocUpdate): Int = mainRepository.deleteLocFromDatabase(memberLoc)
}