package com.fimbleenterprises.sportsdb.data.db

import androidx.room.TypeConverter

class Converters {

    // Probably not going to need this.

    /**
     * In case a property in an object class is not a primitive/basic type we need to create
     * a TypeConverter in order for Room to easily work with it.  We have two choices in how to deal
     * with this scenario.  The first choice would be to simply make the object become a table proper
     * in Room or we can convert the custom object to something SQLite will natively understand.
     *
     * We don't need to wire this up to anything, the @TypeConverter annotation and the
     * parameter/return type will be enough for Room to figure out who needs it.
     */

    @TypeConverter
    fun fromAny(any: Any?): String {
        return any?.toString() ?: ""
    }

    @TypeConverter
    fun toAny(any:String): Any {
        return any
    }
}