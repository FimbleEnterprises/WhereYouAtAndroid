package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.MemberLocation
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberLocation(memberLocation: MemberLocation):Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberLocations(memberLocations: List<MemberLocation>):List<Long>

    @Update
    suspend fun updateMemberLocation(memberLocation: MemberLocation) : Int

    @Delete
    suspend fun deleteMemberLocation(memberLocation: MemberLocation) : Int

    @Query("DELETE FROM member_locations")
    suspend fun deleteAll() : Int

    @Query("SELECT * FROM member_locations")
    fun getAllMemberLocations(): Flow<List<MemberLocation>>

    @Query("SELECT * FROM member_locations WHERE memberid = :memberid")
    fun getMemberLocation(memberid: Long): Flow<MemberLocation>

    @Query("SELECT * FROM member_locations WHERE tripcode = :tripcode")
    fun getMemberLocationsForTrip(tripcode: String): Flow<List<MemberLocation>>

}