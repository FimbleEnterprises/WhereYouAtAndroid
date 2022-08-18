package com.fimbleenterprises.whereyouat.di

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.data.MainRepositoryImpl
import com.fimbleenterprises.whereyouat.data.local.LocalDataSource
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Singleton
    @Provides
    fun provideTripRepository(
        localDatasource: LocalDataSource,
        remoteDataSource: RemoteDataSource
    ): MainRepository {
        return MainRepositoryImpl(remoteDataSource, localDatasource)
    }

}