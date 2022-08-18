package com.fimbleenterprises.whereyouat.data.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.buffer
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteDataSourceImplTest {

    private lateinit var service: TripsServiceApi
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        service = Retrofit.Builder()
            .baseUrl(server.url(""))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TripsServiceApi::class.java)
    }

    private fun enqueueMockResponse(
        fileName:String
    ){
        val inputStream = javaClass.classLoader!!.getResourceAsStream(fileName)
        val source = inputStream.source().buffer()
        val mockResponse = MockResponse()
        mockResponse.setBody(source.readString(Charsets.UTF_8))
        server.enqueue(mockResponse)

    }

    @After
    fun tearDown() {

    }

    @Test
    fun getMemberLocations() {
        runBlocking {
            // Using a response we obtained manually in a browser and saved to a file we parse it
            // in the same manner we will in production from the API using Retrofit.
            enqueueMockResponse("getmemberlocs_ibasj.json")
            val responseBody = service.getMemberLocations("IBASJ").body()
            val memberLocs = responseBody!!.locUpdates
            val firstLoc = memberLocs[0]
            assertThat(firstLoc).isNotNull()
            assertThat(firstLoc.tripcode).isEqualTo("IBASJ")
        }
    }

    @Test
    fun createTrip() {
    }

    @Test
    fun uploadMyLocation() {
    }

}