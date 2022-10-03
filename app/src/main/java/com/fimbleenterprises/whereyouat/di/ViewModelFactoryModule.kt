package com.fimbleenterprises.whereyouat.di

import android.app.Application
import com.fimbleenterprises.whereyouat.data.usecases.*
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModelFactory
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
        serviceStateUseCases: ServiceStateUseCases,
        validateClientTripCodeUseCase: ValidateClientTripCodeUseCase,
        validateApiServerRunningUseCase: ValidateApiServerRunningUseCase,
        getUpdateRateFromApiUseCase: GetUpdateRateFromApiUseCase,
        getServerUrlFromApiUseCase: GetServerUrlFromApiUseCase,
        saveWaypointPositionUseCase: SaveWaypointPositionUseCase,
        getWaypointPositionUseCase: GetWaypointPositionUseCase,
        removeWaypointPositionUseCase: RemoveWaypointPositionUseCase
    ): MainViewModelFactory {
        return MainViewModelFactory(
            application,
            createTripWithApiUseCase,
            getMemberLocsFromDbUseCase,
            getMyLocFromDbUseCase,
            validateTripCodeAgainstApiUseCase,
            serviceStateUseCases,
            validateClientTripCodeUseCase,
            validateApiServerRunningUseCase,
            getUpdateRateFromApiUseCase,
            getServerUrlFromApiUseCase,
            saveWaypointPositionUseCase,
            getWaypointPositionUseCase,
            removeWaypointPositionUseCase
        )
    }

}