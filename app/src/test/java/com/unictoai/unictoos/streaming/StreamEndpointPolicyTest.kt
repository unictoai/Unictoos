package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamEndpointPolicyTest {
    @Test
    fun acceptsRtmpRtmpsAndSrt() {
        assertEquals(StreamTransport.RTMP, StreamEndpointPolicy.transport("rtmp://host/app"))
        assertEquals(StreamTransport.RTMP, StreamEndpointPolicy.transport("rtmps://host/app"))
        assertEquals(StreamTransport.SRT, StreamEndpointPolicy.transport("srt://host:9000?streamid=publish:test"))
        assertTrue(StreamEndpointPolicy.isSupported("srt://host:9000"))
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        assertFalse(StreamEndpointPolicy.isSupported("https://example.com/live"))
        assertFalse(StreamEndpointPolicy.isSupported("udp://host:9000"))
    }
}
