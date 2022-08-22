package com.fimbleenterprises.whereyouat.data

import android.util.Log
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface MainRepository {

    // -----------------------------------------------------------
    //                       MEMBER LOCS
    // -----------------------------------------------------------
    suspend fun getAllMemberLocationsFromApi(tripcode: String): Flow<Resource<MemberLocationsApiResponse>>
    suspend fun createInApiTrip(memberid: Long): Flow<Resource<BaseApiResponse>>
    suspend fun uploadMyLocationToApi(locUpdate: LocUpdate): Flow<Resource<BaseApiResponse>>
    suspend fun saveMemberLocationToDatabase(locUpdate:LocUpdate): Long
    suspend fun saveMemberLocationsToDatabase(locUpdates:List<LocUpdate>): List<Long>
    suspend fun deleteAllLocsFromDatabase(): Int
    fun getAllMemberLocsFromDatabase() : Flow<List<LocUpdate>>

    // -----------------------------------------------------------
    //                         MY LOC
    // -----------------------------------------------------------
    suspend fun deleteAllMyLocationsFromDb(): Int
    suspend fun deleteMyLocationFromDb(rowid: Int): Int
    suspend fun deleteMyLocationFromDb(myLocation: MyLocation): Int
    suspend fun saveMyLocationToDb(myLocation: MyLocation): Long
    fun getMyLocationFromDb(memberid: Long) : Flow<MyLocation>

}