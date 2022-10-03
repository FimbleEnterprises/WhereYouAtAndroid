package com.fimbleenterprises.whereyouat.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.sql.RowId

@Entity(tableName = "messaging")
data class Message(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val rowId: Long,
    val onetimeMessage: String
)
