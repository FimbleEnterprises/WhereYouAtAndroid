package com.fimbleenterprises.whereyouat.data

import android.util.Log
import com.fimbleenterprises.whereyouat.data.local.LocalDataSource
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSource
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.model.MyLocation
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

    override suspend fun getAllMemberLocationsFromApi(tripcode: String): Flow<Resource<MemberLocationsApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.getMemberLocations(tripcode) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createInApiTrip(memberid: Long): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.createTrip(memberid) })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun uploadMyLocationToApi(locUpdate:LocUpdate): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSource.uploadMyLocation(locUpdate) })
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
    //                     MY LOCATION STUFF
    // -----------------------------------------------------------



    init { Log.i(TAG, "Initialized:TripRepositoryImpl") }
    companion object { private const val TAG = "FIMTOWN|TripRepositoryImpl" }

}

