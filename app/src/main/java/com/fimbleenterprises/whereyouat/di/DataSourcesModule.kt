package com.fimbleenterprises.whereyouat.di

import com.fimbleenterprises.whereyouat.data.db.MyLocationDao
import com.fimbleenterprises.whereyouat.data.db.MemberLocationsDao
import com.fimbleenterprises.whereyouat.data.db.ServiceStateDao
import com.fimbleenterprises.whereyouat.data.local.LocalDataSource
import com.fimbleenterprises.whereyouat.data.local.LocalDataSourceImpl
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSource
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSourceImpl
import com.fimbleenterprises.whereyouat.data.remote.WhereYouAtWebApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataSourcesModule {

    @Provides
    @Singleton
    fun providesLocalDataSource(
        memberLocationsDao: MemberLocationsDao,
        myLocationDao: MyLocationDao,
        serviceStateDao: ServiceStateDao
    ): LocalDataSource {
        return LocalDataSourceImpl(memberLocationsDao, myLocationDao,serviceStateDao)
    }

    @Provides
    @Singleton
    fun providesRemoteDataSource(
        whereYouAtWebApi: WhereYouAtWebApi
    ): RemoteDataSource {
        return RemoteDataSourceImpl(whereYouAtWebApi)
    }

}