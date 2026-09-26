package com.unictoai.unictoos

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamEndpointBuilderTest {
    @Test
    fun rtmpJoinsUrlAndKey() {
        assertEquals(
            "rtmp://host/app/key123",
            buildStreamEndpoint("rtmp://host/app", "key123"),
        )
    }

    @Test
    fun rtmpTrimsTrailingSlashAndWhitespace() {
        assertEquals(
            "rtmps://host/app/key123",
            buildStreamEndpoint("  rtmps://host/app/  ", "  key123  "),
        )
    }

    @Test
    fun srtEncodesKeyAsStreamId() {
        assertEquals(
            "srt://host:9000?streamid=key123",
            buildStreamEndpoint("srt://host:9000", "key123"),
        )
    }

    @Test
    fun srtAppendsStreamIdWhenOtherQueryParamsExist() {
        assertEquals(
            "srt://host:9000?latency=200&streamid=key123",
            buildStreamEndpoint("srt://host:9000?latency=200", "key123"),
        )
    }

    @Test
    fun srtKeepsExistingStreamId() {
        assertEquals(
            "srt://host:9000?streamid=publish%3Aoriginal",
            buildStreamEndpoint("srt://host:9000?streamid=publish%3Aoriginal", "other"),
        )
    }

    @Test
    fun srtWithoutKeyLeavesUrlUntouched() {
        assertEquals(
            "srt://host:9000",
            buildStreamEndpoint("srt://host:9000", ""),
        )
    }

    @Test
    fun srtKeyIsUrlEncoded() {
        assertEquals(
            "srt://host:9000?streamid=key+with+spaces%2Fslash",
            buildStreamEndpoint("srt://host:9000", "key with spaces/slash"),
        )
    }
}
