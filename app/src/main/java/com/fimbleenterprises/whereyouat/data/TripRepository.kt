package com.fimbleenterprises.whereyouat.data

import android.util.Log
import com.fimbleenterprises.whereyouat.data.local.LocalDataSourceImpl
import com.fimbleenterprises.whereyouat.data.remote.RemoteDataSourceImpl
import com.fimbleenterprises.whereyouat.model.BaseApiResponse
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MemberLocationsApiResponse
import com.fimbleenterprises.whereyouat.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TripRepository @Inject constructor(
    private val remoteDataSourceImpl: RemoteDataSourceImpl,
    private val localDataSourceImpl: LocalDataSourceImpl
) : BaseApiCaller() {

    suspend fun getMemberLocations(tripcode: String): Flow<Resource<MemberLocationsApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSourceImpl.getMemberLocations(tripcode) })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun createTrip(memberid: Long): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSourceImpl.createTrip(memberid) })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun uploadMyLocation(locUpdate:LocUpdate): Flow<Resource<BaseApiResponse>> {
        return flow {
            emit(safeApiCall { remoteDataSourceImpl.uploadMyLocation(locUpdate) })
            Log.i(TAG, "-= ${locUpdate.toJson()} =-")
        }.flowOn(Dispatchers.IO)
    }

    suspend fun saveMemberLocation(memberlocation:LocUpdate): Long {
        return localDataSourceImpl.saveMemberLocationToDB(memberlocation)
    }

    init { Log.i(TAG, "Initialized:TripRepository") }
    companion object { private const val TAG = "FIMTOWN|TripRepository" }

}

