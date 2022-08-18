package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.MyLocation
import kotlinx.coroutines.flow.Flow
import retrofit2.http.DELETE

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface MyLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyLocation(myLocation: MyLocation):Long

    @Update
    suspend fun updateMyLocation(myLocation: MyLocation) : Int

    @Delete
    suspend fun deleteMyLocation(myLocation: MyLocation) : Int

    @Query("DELETE FROM my_location WHERE rowid = :rowid")
    suspend fun deleteMyLocation(rowid: Int) : Int

    @Query("DELETE FROM my_location")
    suspend fun deleteAll() : Int

    @Query("SELECT * FROM my_location WHERE rowid = :rowid")
    fun getMyLocation(rowid: Long): Flow<MyLocation>

    /*  Only dealing with single rows at present.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMyLocations(myLocations: List<MyLocation>):List<Long> */

    /*  Only dealing with single rows at present.
    @Query("SELECT * FROM my_location")
    fun getAllMyLocations(): Flow<List<MyLocation>>*/

}