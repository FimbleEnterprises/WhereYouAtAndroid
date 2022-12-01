package com.fimbleenterprises.whereyouat.data.usecases

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.utils.Constants
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import retrofit2.Response
import kotlin.contracts.Returns

class GetServerUrlFromApiUseCase(private val mainRepository: MainRepository) {

    suspend fun execute(): String {
        val response: Resource<BaseApiResponse> =
            mainRepository.retrieveServerUrlFromApi().single()
        when (response) {
            is Resource.Success -> {
                response.data?.let {
                    if (response.data.wasSuccessful && response.data.genericValue.toString().isNotEmpty()) {
                        return response.data.genericValue.toString()
                    }
                }
                return handleError()
            }
            is Resource.Error -> {}
            is Resource.Loading -> {}
        }
        return handleError()
    }

    private fun handleError(): String {
        // For now, we have only two urls to pick from.  We would like a more elegant way
        // to cycle through multiple possible urls if ever there are more.
        return if (Constants.DEFAULT_BASE_URL == Constants.AZURE_BASE_URL) {
            Constants.HOME_BASE_URL
        } else {
            Constants.AZURE_BASE_URL
        }
    }
}