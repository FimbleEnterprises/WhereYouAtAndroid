package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.data.db.MyLocationDao
import com.fimbleenterprises.whereyouat.data.db.MemberLocationsDao
import com.fimbleenterprises.whereyouat.data.db.ServiceStatusDao
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * This class does the heavy lifting by interacting with the Room DB.
 */
@Singleton
class LocalDataSourceImpl(
    private val memberLocationsDao: MemberLocationsDao,
    private val myLocationDao: MyLocationDao,
    private val serviceStatusDao: ServiceStatusDao
)
: LocalDataSource {

    // -----------------------------------------------------------
    //                       MEMBER LOCATIONS
    // -----------------------------------------------------------
    override suspend fun saveMemberLocationsToDB(locUpdates: List<LocUpdate>): List<Long> {
        return memberLocationsDao.insertMemberLocations(locUpdates)
    }

    override suspend fun saveMemberLocationToDB(locUpdate: LocUpdate): Long {
        return memberLocationsDao.insertMemberLocation(locUpdate)
    }

    override fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>> {
        return memberLocationsDao.getAllMemberLocations()
    }

    override fun getSavedMemberLocationFromDB(memberid: Long): Flow<LocUpdate> {
        return memberLocationsDao.getMemberLocation(memberid)
    }

    override suspend fun deleteSavedMemberLocations(): Int {
        return memberLocationsDao.deleteAll()
    }

    override suspend fun deleteSavedMemberLocation(locUpdate: LocUpdate): Int {
        return memberLocationsDao.deleteMemberLocation(locUpdate)
    }

    override suspend fun updateMemberLocation(locUpdate: LocUpdate): Int {
        return memberLocationsDao.updateMemberLocation(locUpdate)
    }

    // -----------------------------------------------------------
    //                       MY LOCATIONS
    // -----------------------------------------------------------
    override suspend fun saveMyLocationToDB(myLocation: MyLocation): Long {
        return myLocationDao.insertMyLocation(myLocation)
    }

    override fun getSavedMyLocationFromDB(memberid: Long): Flow<MyLocation> {
        return myLocationDao.getMyLocation(memberid)
    }

    override suspend fun deleteSavedMyLocation(myLocation: MyLocation): Int {
        return myLocationDao.deleteMyLocation(myLocation)
    }

    override suspend fun deleteSavedMyLocation(rowid: Int): Int {
        return myLocationDao.deleteMyLocation(rowid)
    }

    override suspend fun deleteAll(): Int {
        return myLocationDao.deleteAll()
    }

    override suspend fun updateMyLocation(myLocation: MyLocation): Int {
        return myLocationDao.updateMyLocation(myLocation)
    }

    // -----------------------------------------------------------
    //                       SERVICE STATUS
    // -----------------------------------------------------------
    override suspend fun getServiceStatus(): ServiceStatus {
        return serviceStatusDao.getServiceStatus()
    }

    override fun getServiceStatusFlow(): Flow<ServiceStatus> {
        TODO("Not yet implemented")
    }

    override suspend fun insertServiceStatus(serviceStatus: ServiceStatus): Long {
        return serviceStatusDao.insertServiceStatus(serviceStatus)
    }

    override suspend fun deleteServiceStatus(): Int {
        return serviceStatusDao.delete()
    }


}