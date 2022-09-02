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

    override fun getAllMemberLocsFromDatabase(): Flow<List<LocUpdate>> {
        return localDataSource.getSavedMemberLocationsFromDB()
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

    override suspend fun getServiceStatus(): ServiceStatus {
        return localDataSource.getServiceStatus()
    }

    override fun getServiceStatusFlow(): Flow<ServiceStatus> {
        return localDataSource.getServiceStatusFlow()
    }

    override suspend fun deleteServiceStatus(): Int {
        return localDataSource.deleteServiceStatus()
    }

    override suspend fun insertServiceStatus(serviceStatus: ServiceStatus): Long {
        return localDataSource.insertServiceStatus(serviceStatus)
    }

    override suspend fun setServiceRunning(isRunning: Boolean): Int {
        return localDataSource.setServiceRunning(isRunning)
    }

    override suspend fun setServiceStarting(isStarting: Boolean): Int {
        return localDataSource.setServiceStarting(isStarting)
    }

    override suspend fun setServiceStopping(isStopping: Boolean): Int {
        return localDataSource.setServiceStopping(isStopping)
    }

    override suspend fun setServiceStatus(serviceStatus: ServiceStatus): Long {
        return localDataSource.insertServiceStatus(serviceStatus)
    }

    init { Log.i(TAG, "Initialized:TripRepositoryImpl") }
    companion object { private const val TAG = "FIMTOWN|TripRepositoryImpl" }

}

