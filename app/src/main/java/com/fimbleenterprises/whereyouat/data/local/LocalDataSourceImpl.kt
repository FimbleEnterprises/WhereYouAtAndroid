package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.data.db.TripDao
import com.fimbleenterprises.whereyouat.model.LocUpdate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * This class does the heavy lifting by interacting with the Room DB.
 */
class LocalDataSourceImpl
    @Inject constructor(
        private val tripDao: TripDao
    )
: LocalDataSource {
    override suspend fun saveMemberLocationsToDB(locUpdates: List<LocUpdate>): List<Long> {
        return tripDao.insertMemberLocations(locUpdates)
    }

    override suspend fun saveMemberLocationToDB(locUpdate: LocUpdate): Long {
        return tripDao.insertMemberLocation(locUpdate)
    }

    override fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>> {
        return tripDao.getAllMemberLocations()
    }

    override fun getSavedMemberLocationFromDB(memberid: Long): Flow<LocUpdate> {
        return tripDao.getMemberLocation(memberid)
    }

    override suspend fun deleteSavedMemberLocations(): Int {
        return tripDao.deleteAll()
    }

    override suspend fun deleteSavedMemberLocation(locUpdate: LocUpdate): Int {
        return tripDao.deleteMemberLocation(locUpdate)
    }

    override suspend fun updateMemberLocation(locUpdate: LocUpdate): Int {
        return tripDao.updateMemberLocation(locUpdate)
    }


}