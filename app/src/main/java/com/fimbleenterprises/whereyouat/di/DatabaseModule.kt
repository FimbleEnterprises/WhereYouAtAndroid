package com.fimbleenterprises.whereyouat.di

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.fimbleenterprises.whereyouat.data.db.MyLocationDao
import com.fimbleenterprises.whereyouat.data.db.MemberLocationsDao
import com.fimbleenterprises.whereyouat.data.db.ServiceStateDao
import com.fimbleenterprises.whereyouat.data.db.WhereYouAtDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun provideTripDb(app:Application): WhereYouAtDatabase {
        return Room.databaseBuilder(app, WhereYouAtDatabase::class.java, "where_you_at_main")
            .fallbackToDestructiveMigration()
            /*.setQueryCallback(
                fun(sqlQuery: String, bindArgs: MutableList<Any>) {
                    println("SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor()
            )*/
            .allowMainThreadQueries()
            .build()
    }

    @Singleton
    @Provides
    fun provideTripsDAO(whereYouAtDatabase: WhereYouAtDatabase): MemberLocationsDao {
        return whereYouAtDatabase.getMemberLocationsDao()
    }

    @Singleton
    @Provides
    fun provideMyLocationsDAO(whereYouAtDatabase: WhereYouAtDatabase): MyLocationDao {
        return whereYouAtDatabase.getMyLocationDao()
    }

    @Singleton
    @Provides
    fun provideServiceStatusDAO(whereYouAtDatabase: WhereYouAtDatabase): ServiceStateDao {
        return whereYouAtDatabase.getServiceStateDao()
    }


  init { Log.i(TAG, "Initialized:DatabaseModule") }
  companion object { private const val TAG = "FIMTOWN|DatabaseModule" }

}