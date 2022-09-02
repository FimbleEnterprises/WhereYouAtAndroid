package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * This interface is what Room will use to actually perform CRUD operations in the db.
 */
@Dao // This annotation turns this interface into a magical mechanism to actually perform CRUD operations in the Room db.
interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(Message: Message) : Long

    @Query("DELETE FROM messaging")
    suspend fun delete() : Int

    @Query("SELECT * FROM messaging WHERE rowid = 1")
    fun getMessage(): Message

    @Query("SELECT * FROM messaging WHERE rowid = 1")
    fun getMessageFlow(): Flow<Message>

}