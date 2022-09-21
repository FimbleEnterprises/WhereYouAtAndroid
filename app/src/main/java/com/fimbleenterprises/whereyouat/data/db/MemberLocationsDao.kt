package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.LocUpdate
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface MemberLocationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberLocation(locUpdate: LocUpdate):Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberLocations(locUpdates: List<LocUpdate>):List<Long>

    @Update
    suspend fun updateMemberLocation(locUpdate: LocUpdate) : Int

    @Delete
    suspend fun deleteMemberLocation(locUpdate: LocUpdate) : Int

    @Query("DELETE FROM member_locations")
    suspend fun deleteAll() : Int

    @Query("SELECT * FROM member_locations")
    fun getAllMemberLocations(): Flow<List<LocUpdate>>

    @Query("SELECT * FROM member_locations")
    suspend fun getAllMemberLocationsOneTime(): List<LocUpdate>

    @Query("SELECT * FROM member_locations WHERE memberid = :memberid")
    fun getMemberLocation(memberid: Long): Flow<LocUpdate>

    @Query("SELECT * FROM member_locations WHERE tripcode = :tripcode")
    fun getMemberLocationsForTrip(tripcode: String): Flow<List<LocUpdate>>

}