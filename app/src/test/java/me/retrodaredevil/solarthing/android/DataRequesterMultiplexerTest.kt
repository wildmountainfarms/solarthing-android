package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.android.request.DataRequest
import me.retrodaredevil.solarthing.android.request.DataRequester
import me.retrodaredevil.solarthing.android.request.DataRequesterMultiplexer
import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class DataRequesterMultiplexerTest {
    @Test
    fun testMultiplexer() {
        val dummy1 = DummyDataRequester(1)
        val dummy2 = DummyDataRequester(2)

        val requester = DataRequesterMultiplexer(listOf(dummy1, dummy2))
        requester.requestData()
        assertEquals(1, dummy1.timesRequested)
        requester.requestData()
        assertEquals(2, dummy1.timesRequested)

        dummy1.returnSuccessful = false
        assertFalse(requester.requestData().successful)
        assertEquals(3, dummy1.timesRequested)

        requester.requestData()
        assertEquals(3, dummy1.timesRequested)
        assertEquals(1, dummy2.timesRequested)

        requester.requestData()
        assertEquals(3, dummy1.timesRequested)
        assertEquals(2, dummy2.timesRequested)

        requester.requestData()
        assertEquals(3, dummy1.timesRequested)
        assertEquals(3, dummy2.timesRequested)

        assertFalse(requester.requestData().successful) // back to dummy1
        assertEquals(4, dummy1.timesRequested)
        assertEquals(3, dummy2.timesRequested)

        requester.requestData()
        assertEquals(4, dummy1.timesRequested)
        assertEquals(4, dummy2.timesRequested)

        requester.requestData()
        assertEquals(4, dummy1.timesRequested)
        assertEquals(5, dummy2.timesRequested)

        requester.requestData()
        assertEquals(4, dummy1.timesRequested)
        assertEquals(6, dummy2.timesRequested)

        assertFalse(requester.requestData().successful) // back to dummy1
        assertEquals(5, dummy1.timesRequested)
        assertEquals(6, dummy2.timesRequested)
    }
}
private class DummyDataRequester(
        private val id: Int
) : DataRequester {
    override val currentlyUpdating = false
    var timesRequested = 0
    var returnSuccessful = true

    override fun requestData(): DataRequest {
        println("ID: $id")
        timesRequested++
        return DataRequest(Collections.emptyList(), null, returnSuccessful, "Request Failed", null)
    }

}
