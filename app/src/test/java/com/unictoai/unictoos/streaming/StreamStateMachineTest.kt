package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamStateMachineTest {
    @Test
    fun connectionLifecycleAllowsPreparationConnectionLiveAndStop() {
        assertTrue(StreamStateMachine.canTransition(StreamStatus.IDLE, StreamStatus.PREPARING))
        assertTrue(StreamStateMachine.canTransition(StreamStatus.PREPARING, StreamStatus.CONNECTING))
        assertTrue(StreamStateMachine.canTransition(StreamStatus.CONNECTING, StreamStatus.LIVE))
        assertTrue(StreamStateMachine.canTransition(StreamStatus.LIVE, StreamStatus.STOPPING))
        assertTrue(StreamStateMachine.canTransition(StreamStatus.STOPPING, StreamStatus.STOPPED))
        assertTrue(StreamStateMachine.canTransition(StreamStatus.STOPPED, StreamStatus.IDLE))
    }

    @Test
    fun previewFreeCaptureCanReturnToIdleBeforeBroadcastStart() {
        assertTrue(StreamStateMachine.canTransition(StreamStatus.PREPARING, StreamStatus.IDLE))
    }

    @Test
    fun staleConnectionCannotResurrectStoppedSession() {
        assertFalse(StreamStateMachine.canTransition(StreamStatus.STOPPED, StreamStatus.LIVE))
        assertFalse(StreamStateMachine.canTransition(StreamStatus.IDLE, StreamStatus.LIVE))
    }

    @Test
    fun onlyIdleStoppedOrErrorAcceptsAStartRequest() {
        assertTrue(StreamStateMachine.acceptsStart(StreamStatus.IDLE))
        assertTrue(StreamStateMachine.acceptsStart(StreamStatus.STOPPED))
        assertTrue(StreamStateMachine.acceptsStart(StreamStatus.ERROR))
        assertFalse(StreamStateMachine.acceptsStart(StreamStatus.PREPARING))
        assertFalse(StreamStateMachine.acceptsStart(StreamStatus.CONNECTING))
        assertFalse(StreamStateMachine.acceptsStart(StreamStatus.LIVE))
        assertFalse(StreamStateMachine.acceptsStart(StreamStatus.RECONNECTING))
    }

    @Test
    fun stopIsAcceptedForPendingOrActiveStatesButNotIdle() {
        assertFalse(StreamStateMachine.acceptsStop(StreamStatus.IDLE))
        assertFalse(StreamStateMachine.acceptsStop(StreamStatus.STOPPED))
        assertTrue(StreamStateMachine.acceptsStop(StreamStatus.PREPARING))
        assertTrue(StreamStateMachine.acceptsStop(StreamStatus.CONNECTING))
        assertTrue(StreamStateMachine.acceptsStop(StreamStatus.LIVE))
        assertTrue(StreamStateMachine.acceptsStop(StreamStatus.RECONNECTING))
    }
}
