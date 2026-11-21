package com.redcode.app.runtime

import com.redcode.app.data.model.ServerHealthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthMonitorTest {

    @Test
    fun testInitialHealthStateIsOffline() {
        val state = ServerHealthState(
            isConnected = false,
            latencyMs = 0L,
            address = "127.0.0.1:49374 · Loopback",
            statusText = "Checking..."
        )
        assertFalse(state.isConnected)
        assertEquals(0L, state.latencyMs)
        assertEquals("Checking...", state.statusText)
    }

    @Test
    fun testServerHealthStateConnected() {
        val state = ServerHealthState(
            isConnected = true,
            latencyMs = 15L,
            address = "127.0.0.1:49374 · Loopback",
            statusText = "Opencode"
        )
        assertTrue(state.isConnected)
        assertEquals(15L, state.latencyMs)
        assertEquals("127.0.0.1:49374 · Loopback", state.address)
        assertEquals("Opencode", state.statusText)
    }
}
