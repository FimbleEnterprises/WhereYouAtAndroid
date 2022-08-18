package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository

class DeleteMyLocFromDbUseCase(private val mainRepository: MainRepository) {
    suspend fun execute(): Int = mainRepository.deleteAllMyLocationsFromDb()
}