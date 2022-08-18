package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.MyLocation

class SaveMyLocToDbUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(myLocation: MyLocation): Long {
        return mainRepository.saveMyLocationToDb(myLocation)
    }

}