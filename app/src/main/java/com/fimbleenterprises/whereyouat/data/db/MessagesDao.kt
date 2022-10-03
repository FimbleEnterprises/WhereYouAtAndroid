package com.fimbleenterprises.whereyouat.data.db
import androidx.room.*
import com.fimbleenterprises.whereyouat.model.Message
import kotlinx.coroutines.flow.Flow
@Dao
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