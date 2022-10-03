package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

class ValidateClientTripCodeUseCase() {
    fun execute(tripcode: String?): Boolean = tripcode != null && tripcode.length >= 4
}