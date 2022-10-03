package com.fimbleenterprises.whereyouat.data.local

import com.fimbleenterprises.whereyouat.data.db.MyLocationDao
import com.fimbleenterprises.whereyouat.data.db.MemberLocationsDao
import com.fimbleenterprises.whereyouat.data.db.ServiceStateDao
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceState
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * This class does the heavy lifting by interacting with the Room DB.
 */
@Singleton
class LocalDataSourceImpl(
    private val memberLocationsDao: MemberLocationsDao,
    private val myLocationDao: MyLocationDao,
    private val serviceStateDao: ServiceStateDao
)
: LocalDataSource {

    // -----------------------------------------------------------
    //                       MEMBER LOCATIONS
    // -----------------------------------------------------------
    override suspend fun saveMemberLocationsToDB(locUpdates: List<LocUpdate>): List<Long> {
        return memberLocationsDao.insertMemberLocations(locUpdates)
    }

    override suspend fun saveMemberLocationToDB(locUpdate: LocUpdate): Long {
        return memberLocationsDao.insertLocUpdate(locUpdate)
    }

    override fun getSavedMemberLocationsFromDB(): Flow<List<LocUpdate>> {
        return memberLocationsDao.getAllMemberLocations()
    }

    override suspend fun getSavedMemberLocationsFromDBOneTime(): List<LocUpdate> {
        return memberLocationsDao.getAllMemberLocationsOneTime()
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

    // -----------------------------------------------------------
    //                       SERVICE STATUS
    // -----------------------------------------------------------
    override suspend fun getServiceStatus(): ServiceState {
        return serviceStateDao.getServiceState()
    }

    override fun getServiceStatusFlow(): Flow<ServiceState> {
        return serviceStateDao.getServiceStatusFlow()
    }

    override suspend fun saveServiceStatus(serviceState: ServiceState): Long {
        return serviceStateDao.setServiceState(serviceState)
    }

    override suspend fun deleteServiceStatus(): Int {
        return serviceStateDao.delete()
    }

    override suspend fun setServiceRunning(): Int {
        return serviceStateDao.setServiceRunning()
    }

    override suspend fun setServiceStarting(): Int {
        return serviceStateDao.setServiceStarting()
    }

    override suspend fun setServiceStopping(): Int {
        return serviceStateDao.setServiceStopping()
    }

    override suspend fun setServiceStopped(): Int {
        return serviceStateDao.setServiceStopped()
    }

    override suspend fun setServiceRestarting(): Int {
        return serviceStateDao.setServiceRestarting()
    }


}