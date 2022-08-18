package com.fimbleenterprises.whereyouat.di

import com.fimbleenterprises.whereyouat.WhereYouAt
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MyAppModule {

    @Singleton
    @Provides
    fun providesWhereYouAt() : WhereYouAt {
        return WhereYouAt()
    }

}