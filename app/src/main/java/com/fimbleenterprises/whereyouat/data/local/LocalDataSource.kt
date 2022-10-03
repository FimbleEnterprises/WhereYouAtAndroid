package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceState
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    // Trips table
    suspend fun saveMemberLocationsToDB(locUpdates:List<LocUpdate>): List<Long>
    suspend fun saveMemberLocationToDB(locUpdate: LocUpdate):Long
    fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>>
    suspend fun getSavedMemberLocationsFromDBOneTime(): List<LocUpdate>
    fun getSavedMemberLocationFromDB(memberid:Long): Flow<LocUpdate>
    suspend fun deleteSavedMemberLocations():Int
    suspend fun deleteSavedMemberLocation(locUpdate: LocUpdate):Int
    suspend fun updateMemberLocation(locUpdate:LocUpdate):Int
    
    // My location table
    suspend fun saveMyLocationToDB(myLocation: MyLocation):Long
    fun getSavedMyLocationFromDB(memberid:Long): Flow<MyLocation>
    suspend fun deleteSavedMyLocation(myLocation: MyLocation):Int
    suspend fun deleteAll():Int
    suspend fun deleteSavedMyLocation(rowid: Int): Int

    // Service status
    suspend fun getServiceStatus(): ServiceState
    fun getServiceStatusFlow(): Flow<ServiceState>
    suspend fun saveServiceStatus(serviceState: ServiceState): Long
    suspend fun deleteServiceStatus(): Int
    suspend fun setServiceRunning(): Int
    suspend fun setServiceStarting(): Int
    suspend fun setServiceStopping(): Int
    suspend fun setServiceStopped(): Int
    suspend fun setServiceRestarting(): Int

    // Just dealing with single rows at this time for my locations
    // fun getSavedMyLocationsFromDB(): Flow<List<MyLocation>>
    // suspend fun saveMyLocationsToDB(myLocations:List<MyLocation>): List<Long>

}