package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface ServiceStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceStatus(serviceStatus: ServiceStatus) : Long

    @Query("DELETE FROM service_status")
    suspend fun delete() : Int

    @Query("SELECT * FROM service_status WHERE rowid = 1")
    fun getServiceStatus(): ServiceStatus

    @Query("SELECT * FROM service_status WHERE rowid = 1")
    fun getServiceStatusFlow(): Flow<ServiceStatus>

}