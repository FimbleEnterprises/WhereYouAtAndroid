package com.fimbleenterprises.whereyouat.di

import android.app.Application
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModelFactory
import com.fimbleenterprises.whereyouat.service.ServiceMessenger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelFactoryModule {
    @Singleton
    @Provides
    fun provideViewModelFactory(
        application: Application,
        createTripWithApiUseCase: CreateTripWithApiUseCase,
        getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
        getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
        validateTripCodeAgainstApiUseCase: ValidateTripCodeAgainstApiUseCase,
        getServiceStatusUseCase: GetServiceStatusUseCase,
        saveServiceStatusUseCase: SaveServiceStatusUseCase,
        uploadMyLocToApiUseCase: UploadMyLocToApiUseCase,
        getMemberLocsFromApiUseCase: GetMemberLocsFromApiUseCase,
        serviceMessenger: ServiceMessenger,
        validateClientTripCodeUseCase: ValidateClientTripCodeUseCase
    ): MainViewModelFactory {
        return MainViewModelFactory(
            application,
            createTripWithApiUseCase,
            getMemberLocsFromDbUseCase,
            getMyLocFromDbUseCase,
            validateTripCodeAgainstApiUseCase,
            getServiceStatusUseCase,
            saveServiceStatusUseCase,
            uploadMyLocToApiUseCase,
            getMemberLocsFromApiUseCase,
            serviceMessenger,
            validateClientTripCodeUseCase
        )
    }

}