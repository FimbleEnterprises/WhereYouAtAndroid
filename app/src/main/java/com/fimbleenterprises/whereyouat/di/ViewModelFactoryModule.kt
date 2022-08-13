package com.fimbleenterprises.whereyouat.di

import android.app.Application
import com.fimbleenterprises.whereyouat.data.TripRepository
import com.fimbleenterprises.whereyouat.viewmodel.MainViewModelFactory
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
        tripRepository: TripRepository
    ): MainViewModelFactory {
        return MainViewModelFactory(application, tripRepository)
    }

}