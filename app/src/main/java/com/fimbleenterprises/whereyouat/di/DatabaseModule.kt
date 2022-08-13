package com.fimbleenterprises.whereyouat.di

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.fimbleenterprises.whereyouat.data.db.TripDao
import com.fimbleenterprises.whereyouat.data.db.TripDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun provideTripDb(app:Application): TripDatabase {
        return Room.databaseBuilder(app, TripDatabase::class.java, "teams_db")
            .fallbackToDestructiveMigration()
            .setQueryCallback(
                fun(sqlQuery: String, bindArgs: MutableList<Any>) {
                    println("SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor()
            )
            .allowMainThreadQueries()
            .build()
    }

    @Singleton
    @Provides
    fun provideTeamsDAO(tripDatabase: TripDatabase): TripDao {
        return tripDatabase.getTripDao()
    }


  init { Log.i(TAG, "Initialized:DatabaseModule") }
  companion object { private const val TAG = "FIMTOWN|DatabaseModule" }

}