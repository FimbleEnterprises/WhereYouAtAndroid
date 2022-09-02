package com.fimbleenterprises.whereyouat.di

import com.fimbleenterprises.whereyouat.service.ServiceMessenger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServiceMessengerModule {

    @Singleton
    @Provides
    fun providesServiceMessenger() : ServiceMessenger {
        return ServiceMessenger()
    }

}