package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.ServiceState
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface ServiceStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setServiceState(serviceState: ServiceState) : Long

    @Query("DELETE FROM service_state")
    suspend fun delete() : Int

    @Query("SELECT * FROM service_state WHERE rowid = 1")
    fun getServiceState(): ServiceState

    @Query("SELECT * FROM service_state WHERE rowid = 1")
    fun getServiceStatusFlow(): Flow<ServiceState>

    @Query("UPDATE service_state SET state = 0 WHERE rowid = 1")
    fun setServiceRunning(): Int

    @Query("UPDATE service_state SET state = 1 WHERE rowid = 1")
    fun setServiceStarting(): Int

    @Query("UPDATE service_state SET state = 2 WHERE rowid = 1")
    fun setServiceStopping(): Int

    @Query("UPDATE service_state SET state = 4 WHERE rowid = 1")
    fun setServiceStopped(): Int

    @Query("UPDATE service_state SET state = 3 WHERE rowid = 1")
    fun setServiceRestarting(): Int

    /*
        const val SERVICE_STATE_RUNNING = 0
        const val SERVICE_STATE_STARTING = 1
        const val SERVICE_STATE_STOPPING = 2
        const val SERVICE_STATE_RESTART = 3
        const val SERVICE_STATE_STOPPED = 4
        const val SERVICE_STATE_IDLE = 5
    */

}