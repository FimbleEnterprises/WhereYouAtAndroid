package com.fimbleenterprises.whereyouat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.fimbleenterprises.whereyouat.data.usecases.DeleteServiceStatusUseCase
import com.fimbleenterprises.whereyouat.data.usecases.SaveServiceStatusUseCase
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class WhereYouAt : Application() {

    @Inject
    lateinit var deleteServiceStatusUseCase: DeleteServiceStatusUseCase

    @Inject
    lateinit var saveServiceStatusUseCase: SaveServiceStatusUseCase

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)

        CoroutineScope(IO).launch {
            saveServiceStatusUseCase.execute(ServiceStatus(1, false, System.currentTimeMillis(), false, null))
        }

    }

    init {

    }

    @Singleton
    object AppPreferences {
        private const val NAME = "ALL_PREFS"
        private const val MODE = Context.MODE_PRIVATE
        private lateinit var prefs: SharedPreferences

        // list of preferences
        private const val PREF_TRIPCODE = "PREF_TRIPCODE"
        private const val PREF_MEMBERID = "PREF_MEMBERID"
        private const val PREF_MEMBERNAME = "PREF_MEMBERNAME"

        var tripcode : String?
            get() = prefs.getString(PREF_TRIPCODE, "").orEmpty()
            set(value) {
                prefs.edit().putString(PREF_TRIPCODE, value).apply()
            }

        var membername : String
            get() = prefs.getString(PREF_MEMBERNAME, "Me").orEmpty()
            set(value) {
                prefs.edit().putString(PREF_MEMBERNAME, value).apply()
            }

        var memberid : Long
            get() = prefs.getLong(PREF_MEMBERID, 0)
            set(value) {
                prefs.edit().putLong(PREF_MEMBERID, value).apply()
            }

        fun init(context: Context) {
            prefs = context.getSharedPreferences(NAME, MODE)
        }

    }

}