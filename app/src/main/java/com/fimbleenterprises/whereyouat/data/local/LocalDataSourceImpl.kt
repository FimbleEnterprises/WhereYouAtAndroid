package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.data.db.TripDao
import com.fimbleenterprises.whereyouat.model.MemberLocation
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
    override suspend fun saveMemberLocationsToDB(memberLocations: List<MemberLocation>): List<Long> {
        return tripDao.insertMemberLocations(memberLocations)
    }

    override suspend fun saveMemberLocationToDB(memberLocation: MemberLocation): Long {
        return tripDao.insertMemberLocation(memberLocation)
    }

    override fun getSavedMemberLocationsFromDB(): Flow<List<MemberLocation>> {
        return tripDao.getAllMemberLocations()
    }

    override fun getSavedMemberLocationFromDB(memberid: Long): Flow<MemberLocation> {
        return tripDao.getMemberLocation(memberid)
    }

    override suspend fun deleteSavedMemberLocations(): Int {
        return tripDao.deleteAll()
    }

    override suspend fun deleteSavedMemberLocation(memberLocation: MemberLocation): Int {
        return tripDao.deleteMemberLocation(memberLocation)
    }

    override suspend fun updateMemberLocation(memberLocation: MemberLocation): Int {
        return tripDao.updateMemberLocation(memberLocation)
    }


}