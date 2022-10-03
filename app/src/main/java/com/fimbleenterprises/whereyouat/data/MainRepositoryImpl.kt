package com.fimbleenterprises.whereyouat.data

import android.util.Log
import com.fimbleenterprises.whereyouat.data.local.LocalDataSource
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSource
import com.fimbleenterprises.whereyouat.model.*
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MainRepositoryImpl
    constructor(
        private val remoteDataSource: RemoteDataSource,
        private val localDataSource: LocalDataSource
    ): MainRepository, BaseApiCaller() {

    // -----------------------------------------------------------
    //                           MISC
    // -----------------------------------------------------------

    override suspend fun isTripcodeActiveFromApi(tripcode: String): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.isTripActive(tripcode) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun removeUserFromTripInApi(memberid: Long): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.removeUserFromTrip(memberid) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun validateApiServerRunning(): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.validateApiServerRunning() })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun retrieveUpdateRateFromApi(): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.retrieveUpdateRateFromApi() })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun retrieveServerUrlFromApi(): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.retrieveServerUrlFromApi() })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createTripInApi(memberid: Long): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.createTrip(memberid) })
        }.flowOn(Dispatchers.IO)
    }

    // -----------------------------------------------------------
    //                       MEMBER LOCS
    // -----------------------------------------------------------

    override suspend fun getAllMemberLocationsFromApi(tripcode: String): Flow<Resource<MemberLocationsApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.getMemberLocations(tripcode) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveMemberLocationToDatabase(locUpdate: LocUpdate): Long {
        return localDataSource.saveMemberLocationToDB(locUpdate)
    }

    override suspend fun saveMemberLocationsToDatabase(locUpdates: List<LocUpdate>): List<Long> {
        return localDataSource.saveMemberLocationsToDB(locUpdates)
    }

    override suspend fun deleteAllLocsFromDatabase(): Int {
        return localDataSource.deleteSavedMemberLocations()
    }

    override suspend fun deleteLocFromDatabase(locUpdate: LocUpdate): Int {
        return localDataSource.deleteSavedMemberLocation(locUpdate)
    }

    override fun getAllMemberLocsFromDatabase(): Flow<List<LocUpdate>> {
        return localDataSource.getSavedMemberLocationsFromDB()
    }

    override suspend fun getAllMemberLocsFromDatabaseOneTime(): List<LocUpdate> {
        return localDataSource.getSavedMemberLocationsFromDBOneTime()
    }

    // -----------------------------------------------------------
    //                       MY LOCATIONS
    // -----------------------------------------------------------

    override suspend fun uploadMyLocationToApi(locUpdate:LocUpdate): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.uploadMyLocation(locUpdate) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveMyLocationToDb(myLocation: MyLocation): Long {
        return localDataSource.saveMyLocationToDB(myLocation)
    }

    override suspend fun deleteAllMyLocationsFromDb(): Int {
        return localDataSource.deleteAll()
    }

    override suspend fun deleteMyLocationFromDb(rowid: Int): Int {
        return localDataSource.deleteSavedMyLocation(rowid)
    }

    override suspend fun deleteMyLocationFromDb(myLocation: MyLocation): Int {
        return localDataSource.deleteSavedMyLocation(myLocation)
    }

    override fun getMyLocationFromDb(memberid: Long): Flow<MyLocation> {
        return localDataSource.getSavedMyLocationFromDB(memberid)
    }

    // -----------------------------------------------------------
    //                       SERVICE STATUS
    // -----------------------------------------------------------

    override suspend fun getServiceState(): ServiceState {
        return localDataSource.getServiceStatus()
    }

    override fun getServiceStateAsFlow(): Flow<ServiceState> {
        return localDataSource.getServiceStatusFlow()
    }

    override suspend fun deleteServiceState(): Int {
        return localDataSource.deleteServiceStatus()
    }

    override suspend fun saveServiceState(serviceState: ServiceState): Long {
        return localDataSource.saveServiceStatus(serviceState)
    }

    override suspend fun setServiceRunning(): Int {
        return localDataSource.setServiceRunning()
    }

    override suspend fun setServiceStarting(): Int {
        return localDataSource.setServiceStarting()
    }

    override suspend fun setServiceStopping(): Int {
        return localDataSource.setServiceStopping()
    }

    override suspend fun setServiceStopped(): Int {
        return localDataSource.setServiceStopped()
    }

    override suspend fun setServiceRestarting(): Int {
        return localDataSource.setServiceRestarting()
    }

    init { Log.i(TAG, "Initialized:TripRepositoryImpl") }
    companion object { private const val TAG = "FIMTOWN|TripRepositoryImpl" }

}

