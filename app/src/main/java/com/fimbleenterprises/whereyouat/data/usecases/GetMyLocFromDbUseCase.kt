package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.MyLocation
import dagger.Component
import dagger.Provides
import kotlinx.coroutines.flow.Flow

class GetMyLocFromDbUseCase(private val mainRepository: MainRepository) {
    // Supply memberid argument of 0 to get our row.
    // As of this writing there should only ever be a single row in there anyway.
    fun execute(): Flow<MyLocation> = mainRepository.getMyLocationFromDb(0L)
}