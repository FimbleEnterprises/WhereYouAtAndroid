package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.data.db.ServiceStatusDao
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    // Trips table
    suspend fun saveMemberLocationsToDB(locUpdates:List<LocUpdate>): List<Long>
    suspend fun saveMemberLocationToDB(locUpdate: LocUpdate):Long
    fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>>
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
    suspend fun updateMyLocation(myLocation:MyLocation):Int

    // Service status
    suspend fun getServiceStatus(): ServiceStatus
    fun getServiceStatusFlow(): Flow<ServiceStatus>
    suspend fun insertServiceStatus(serviceStatus: ServiceStatus): Long
    suspend fun deleteServiceStatus(): Int

    // Just dealing with single rows at this time for my locations
    // fun getSavedMyLocationsFromDB(): Flow<List<MyLocation>>
    // suspend fun saveMyLocationsToDB(myLocations:List<MyLocation>): List<Long>

}