package com.fimbleenterprises.whereyouat.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fimbleenterprises.whereyouat.model.LocUpdate
import com.fimbleenterprises.whereyouat.model.MyLocation
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class WhereYouAtDatabaseTest {

    private lateinit var db: WhereYouAtDatabase

    private lateinit var memberLocsDao: MemberLocationsDao
    private lateinit var myLocationDao: MyLocationDao
    private lateinit var serviceStateDao: ServiceStateDao

    private val memberLoc1 = "{\"Accuracy\":12.876,\"AvatarUrl\":\"https://lh3.googleusercontent.com/a/ALm5wu23v2CHzUP4W4iLvqOfGsm1NcD1EZVG3Engt6ti\\u003ds96-c\",\"Bearing\":0.0,\"Createdon\":1664738257459,\"DisplayName\":\"Ricoh Printer\",\"Elevation\":264.0,\"Email\":\"medistimusa.ricoh.printer@gmail.com\",\"GoogleId\":\"105209906016523668388\",\"IsBg\":0,\"Lat\":45.263349,\"Lon\":-93.656128,\"MemberName\":\"Me\",\"Memberid\":1663205179416,\"Speed\":0.0,\"Tripcode\":\"F3SA\"}"
    private val memberLoc2 = "{\"Accuracy\":20.0,\"AvatarUrl\":\"https://lh3.googleusercontent.com/a-/AFdZucq-oHBSUBNXGXowWVOFfoeKlEqwAz6w04s4T3ymyEw\\u003ds96-c\",\"Bearing\":166.26106,\"Createdon\":1664738259167,\"DisplayName\":\"Matt Weber\",\"Elevation\":0.0,\"Email\":\"weber.mathew@gmail.com\",\"GoogleId\":\"116830684150145127689\",\"IsBg\":0,\"Lat\":45.262716,\"Lon\":-93.655542,\"MemberName\":\"Me\",\"Memberid\":1663204314706,\"Speed\":7.7720923,\"Tripcode\":\"F3SA\",\"Waypoint\":\"{\\\"latitude\\\": 45.26321986008235, \\\"longitude\\\": -93.65569800138474}\"}"
    private val serviceState_Idle = ServiceState() // Defaults to rowid=1, IDLE
    private val myLoc = MyLocation(
        rowid = 0,
        createdon = System.currentTimeMillis(),
        elevation = 0.0,
        lat = 45.263349,
        lon = -93.656128,
        speed = 10f,
        bearing = 0f,
        accuracy = 5f,
        isBg = 0
    )


    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, WhereYouAtDatabase::class.java)
            .setQueryCallback(
                fun(sqlQuery: String, bindArgs: MutableList<Any>) {
                    println("SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor()
            )
            .build()

        memberLocsDao = db.getMemberLocationsDao()
        myLocationDao = db.getMyLocationDao()
        serviceStateDao = db.getServiceStateDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun canSaveAndRetrieveLocUpdates_test(): Unit = runBlocking {
        val psuedoLocs = ArrayList<LocUpdate>()
        psuedoLocs.add(Gson().fromJson(memberLoc1, LocUpdate::class.java))
        psuedoLocs.add(Gson().fromJson(memberLoc2, LocUpdate::class.java))
        memberLocsDao.insertMemberLocations(psuedoLocs.toList())
        val retrievedLocs = memberLocsDao.getAllMemberLocationsOneTime()
        assertThat(retrievedLocs.size).isEqualTo(2)
    }

    @Test
    fun canSaveAndRetrieveSingleLocUpdate_test(): Unit {
        val saved = Gson().fromJson(memberLoc1, LocUpdate::class.java)
        val retrieved: LocUpdate
        runBlocking { memberLocsDao.insertLocUpdate(saved) }
        runBlocking { retrieved = memberLocsDao.getMemberLocation(1663205179416).take(1).first() }
        assertThat(retrieved).isNotNull()
    }

    @Test
    fun canDeleteLocUpdate_test(): Unit {
        val saved = Gson().fromJson(memberLoc1, LocUpdate::class.java)
        val retrieved: LocUpdate
        runBlocking { memberLocsDao.insertLocUpdate(saved) }
        runBlocking { memberLocsDao.deleteMemberLocation(saved) }
        runBlocking { retrieved = memberLocsDao.getMemberLocation(1663205179416).first() }
        assertThat(retrieved).isNull()
    }

    @Test
    fun canDeleteAllLocUpdates_test() = runBlocking {
        val saved1 = Gson().fromJson(memberLoc1, LocUpdate::class.java)
        val saved2 = Gson().fromJson(memberLoc2, LocUpdate::class.java)

        memberLocsDao.insertLocUpdate(saved1)
        memberLocsDao.insertLocUpdate(saved2)
        var allLocUpdates: List<LocUpdate> = memberLocsDao.getAllMemberLocationsOneTime()
        assertThat(allLocUpdates.size).isGreaterThan(0)
        memberLocsDao.deleteAll()
        allLocUpdates = memberLocsDao.getAllMemberLocationsOneTime()
        runBlocking(Main) { assertThat(allLocUpdates.size).isEqualTo(0) }
    }

    @Test
    fun canSaveAndRetrieveMyLocation_test(): Unit = runBlocking {
        val rowid = myLocationDao.insertMyLocation(myLoc)
        assertThat(rowid).isEqualTo(0)
        val retrievedLoc = myLocationDao.getMyLocation(0).first()
        assertThat(retrievedLoc).isNotNull()
    }

    @Test
    fun canOverwriteMyLocation_test(): Unit = runBlocking {
        val newLoc = MyLocation(
            rowid = 0,
            createdon = System.currentTimeMillis(),
            elevation = 0.0,
            lat = 45.263349,
            lon = -93.656128,
            speed = 20f, /* change speed from 10 */
            bearing = 0f,
            accuracy = 5f,
            isBg = 0
        )
        val rowId: Long = myLocationDao.insertMyLocation(myLoc)
        assertThat(rowId).isEqualTo(0)
        val updatedRowId = myLocationDao.insertMyLocation(newLoc)
        assertThat(updatedRowId).isEqualTo(0)
        val updatedLoc: MyLocation = myLocationDao.getMyLocation(0).first()
        assertThat(updatedLoc.speed).isEqualTo(20f)
    }

    @Test
    fun serviceStateRunning_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        assertThat(rowid).isEqualTo(1)
        val rowsAffected = serviceStateDao.setServiceRunning()
        assertThat(rowsAffected).isEqualTo(1)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_RUNNING)
    }

    @Test
    fun serviceStateStarting_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        assertThat(rowid).isEqualTo(1)
        val rowsAffected = serviceStateDao.setServiceStarting()
        assertThat(rowsAffected).isEqualTo(1)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_STARTING)
    }

    @Test
    fun serviceStateStopping_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        assertThat(rowid).isEqualTo(1)
        val rowsAffected = serviceStateDao.setServiceStopping()
        assertThat(rowsAffected).isEqualTo(1)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_STOPPING)
    }

    @Test
    fun serviceStateStopped_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        assertThat(rowid).isEqualTo(1)
        val rowsAffected = serviceStateDao.setServiceStopped()
        assertThat(rowsAffected).isEqualTo(1)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_STOPPED)
    }

    @Test
    fun serviceStateRestarting_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        assertThat(rowid).isEqualTo(1)
        val rowsAffected = serviceStateDao.setServiceRestarting()
        assertThat(rowsAffected).isEqualTo(1)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_RESTART)
    }

    @Test
    fun serviceStateIdle_test() = runBlocking {
        val rowid = serviceStateDao.setServiceState(serviceState_Idle)
        val state = serviceStateDao.getServiceState()
        assertThat(state.state).isEqualTo(ServiceState.SERVICE_STATE_IDLE)
    }
}