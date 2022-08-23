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
        deleteAllMemberLocsFromDbUseCase: DeleteAllMemberLocsFromDbUseCase,
        getMemberLocsFromDbUseCase: GetMemberLocsFromDbUseCase,
        getMyLocFromDbUseCase: GetMyLocFromDbUseCase,
        getTripcodeIsActiveWithApiUseCase: GetTripcodeIsActiveWithApiUseCase,
        getServiceStatusUseCase: GetServiceStatusUseCase,
        saveServiceStatusUseCase: SaveServiceStatusUseCase
    ): MainViewModelFactory {
        return MainViewModelFactory(
            application,
            createTripWithApiUseCase,
            deleteAllMemberLocsFromDbUseCase,
            getMemberLocsFromDbUseCase,
            getMyLocFromDbUseCase,
            getTripcodeIsActiveWithApiUseCase,
            getServiceStatusUseCase,
            saveServiceStatusUseCase
        )
    }

}