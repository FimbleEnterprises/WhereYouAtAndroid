package com.fimbleenterprises.whereyouat.data

import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.flow.Flow

interface MainRepository {

    // -----------------------------------------------------------
    //                       MEMBER LOCS
    // -----------------------------------------------------------
    suspend fun getAllMemberLocationsFromApi(tripcode: String): Flow<Resource<MemberLocationsApiResponse>>
    suspend fun createTripInApi(memberid: Long): Flow<Resource<BaseApiResponse>>
    suspend fun uploadMyLocationToApi(locUpdate: LocUpdate): Flow<Resource<BaseApiResponse>>
    suspend fun saveMemberLocationToDatabase(locUpdate:LocUpdate): Long
    suspend fun saveMemberLocationsToDatabase(locUpdates:List<LocUpdate>): List<Long>
    suspend fun deleteAllLocsFromDatabase(): Int
    suspend fun deleteLocFromDatabase(locUpdate: LocUpdate):Int
    fun getAllMemberLocsFromDatabase() : Flow<List<LocUpdate>>
    suspend fun getAllMemberLocsFromDatabaseOneTime() : List<LocUpdate>

    // -----------------------------------------------------------
    //                         GENERIC
    // -----------------------------------------------------------
    suspend fun isTripcodeActiveFromApi(tripcode: String): Flow<Resource<BaseApiResponse>>
    suspend fun removeUserFromTripInApi(memberid: Long): Flow<Resource<BaseApiResponse>>
    suspend fun validateApiServerRunning(): Flow<Resource<BaseApiResponse>>
    suspend fun retrieveUpdateRateFromApi(): Flow<Resource<BaseApiResponse>>
    suspend fun retrieveServerUrlFromApi(): Flow<Resource<BaseApiResponse>>

    // -----------------------------------------------------------
    //                         MY LOC
    // -----------------------------------------------------------
    suspend fun deleteAllMyLocationsFromDb(): Int
    suspend fun deleteMyLocationFromDb(rowid: Int): Int
    suspend fun deleteMyLocationFromDb(myLocation: MyLocation): Int
    suspend fun saveMyLocationToDb(myLocation: MyLocation): Long
    fun getMyLocationFromDb(memberid: Long) : Flow<MyLocation>

    // -----------------------------------------------------------
    //                       SERVICE STATE
    // -----------------------------------------------------------
    fun getServiceStateAsFlow(): Flow<ServiceState>
    suspend fun getServiceState(): ServiceState
    suspend fun deleteServiceState(): Int
    suspend fun saveServiceState(serviceState: ServiceState): Long
    suspend fun setServiceRunning(): Int
    suspend fun setServiceStarting(): Int
    suspend fun setServiceStopping(): Int
    suspend fun setServiceStopped(): Int
    suspend fun setServiceRestarting(): Int
}