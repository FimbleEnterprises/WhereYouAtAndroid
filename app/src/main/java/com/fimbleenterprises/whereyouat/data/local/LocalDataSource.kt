package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.model.LocUpdate
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    suspend fun saveMemberLocationsToDB(locUpdates:List<LocUpdate>): List<Long>
    suspend fun saveMemberLocationToDB(locUpdate: LocUpdate):Long
    fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>>
    fun getSavedMemberLocationFromDB(memberid:Long): Flow<LocUpdate>
    suspend fun deleteSavedMemberLocations():Int
    suspend fun deleteSavedMemberLocation(locUpdate: LocUpdate):Int
    suspend fun updateMemberLocation(locUpdate:LocUpdate):Int

}