package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.model.MemberLocation
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    suspend fun saveMemberLocationsToDB(memberLocations:List<MemberLocation>): List<Long>
    suspend fun saveMemberLocationToDB(memberLocation: MemberLocation):Long
    fun getSavedMemberLocationsFromDB(): Flow<List<MemberLocation>>
    fun getSavedMemberLocationFromDB(memberid:Long): Flow<MemberLocation>
    suspend fun deleteSavedMemberLocations():Int
    suspend fun deleteSavedMemberLocation(memberLocation: MemberLocation):Int
    suspend fun updateMemberLocation(memberLocation:MemberLocation):Int

}