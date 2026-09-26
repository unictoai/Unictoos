package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotAggregatePolicyTest {
    @Test
    fun singleSlotPublishesAggregateOnFirstSuccess() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))

        val result = policy.onSuccess(0)

        assertTrue(result.publishAggregate)
        assertTrue(result.retrySlots.isEmpty())
    }

    @Test
    fun singleSlotAggregatesFailureWhenItFails() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))

        val result = policy.onFailed(0)

        assertFalse(result.retrySelf)
        assertTrue(result.publishAggregate)
    }

    @Test
    fun aggregateFailurePublishedOnlyOnce() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))

        assertTrue(policy.onFailed(0).publishAggregate)
        assertFalse(policy.onFailed(0).publishAggregate)
    }

    @Test
    fun secondSlotSuccessDoesNotRepublishAggregate() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        assertTrue(policy.onSuccess(0).publishAggregate)
        val second = policy.onSuccess(1)

        assertFalse(second.publishAggregate)
    }

    @Test
    fun failedSlotRetriesAtSlotLevelWhilePeerIsHealthy() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))
        policy.onSuccess(0)

        val result = policy.onFailed(1)

        assertTrue(result.retrySelf)
        assertFalse(result.publishAggregate)
    }

    @Test
    fun slowSecondaryDoesNotBlockPrimaryAggregate() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        // Secondary fails fast while primary is still attempting: no aggregate yet.
        val earlyFailure = policy.onFailed(1)
        assertFalse(earlyFailure.retrySelf)
        assertFalse(earlyFailure.publishAggregate)

        // Primary connects: aggregate goes live and the failed peer is retried.
        val success = policy.onSuccess(0)
        assertTrue(success.publishAggregate)
        assertEquals(listOf(1), success.retrySlots)
    }

    @Test
    fun aggregateFiresOnlyWhenEverySlotHasFailed() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        assertFalse(policy.onFailed(0).publishAggregate)
        assertTrue(policy.onFailed(1).publishAggregate)
    }

    @Test
    fun authErrorOnOneSlotDoesNotPublishWhilePeerIsHealthy() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))
        policy.onSuccess(0)

        assertEquals(SlotAggregatePolicy.AuthOutcome.NOTHING, policy.onAuthError(1))
    }

    @Test
    fun authFailedSlotIsNeverRetried() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))
        policy.onAuthError(1)

        val success = policy.onSuccess(0)

        assertTrue(success.publishAggregate)
        assertTrue(success.retrySlots.isEmpty())
    }

    @Test
    fun singleSlotAuthErrorPublishesAggregate() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))

        assertEquals(SlotAggregatePolicy.AuthOutcome.PUBLISH_AUTH_ERROR, policy.onAuthError(0))
        assertEquals(SlotAggregatePolicy.AuthOutcome.NOTHING, policy.onAuthError(0))
    }

    @Test
    fun allSlotsAuthFailedPublishesFatalAuthError() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        assertEquals(SlotAggregatePolicy.AuthOutcome.NOTHING, policy.onAuthError(0))
        assertEquals(SlotAggregatePolicy.AuthOutcome.PUBLISH_AUTH_ERROR, policy.onAuthError(1))
    }

    @Test
    fun mixedAuthAndNetworkFailureStaysRetryable() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        // Network failure first: peer still attempting, so the aggregate waits.
        assertFalse(policy.onFailed(0).publishAggregate)
        // Auth failure arrives last: not every slot rejected its key, and the
        // other failure was retryable, so the aggregate must be a failure
        // (reconnectable), never a fatal auth error.
        assertEquals(SlotAggregatePolicy.AuthOutcome.PUBLISH_FAILURE, policy.onAuthError(1))
    }

    @Test
    fun authErrorAfterAggregateFailurePublishesNothing() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        policy.onFailed(0)
        assertTrue(policy.onFailed(1).publishAggregate)
        assertEquals(SlotAggregatePolicy.AuthOutcome.NOTHING, policy.onAuthError(0))
    }

    @Test
    fun midSessionDisconnectAggregatesWhenNoPeerIsLive() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))
        policy.onSuccess(0)

        val result = policy.onDisconnected(0)

        assertFalse(result.retrySelf)
        assertTrue(result.publishAggregate)
        assertFalse(policy.onDisconnected(0).publishAggregate)
    }

    @Test
    fun midSessionDisconnectRetriesSlotWhenPeerIsLive() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))
        policy.onSuccess(0)
        policy.onSuccess(1)

        val result = policy.onDisconnected(0)

        assertTrue(result.retrySelf)
        assertFalse(result.publishAggregate)
    }

    @Test
    fun telemetrySlotPrefersConnectedSlot() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0, 1))

        assertEquals(0, policy.telemetrySlot())
        policy.onSuccess(1)
        assertEquals(1, policy.telemetrySlot())
    }

    @Test
    fun inactiveSlotsAreIgnored() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))

        assertFalse(policy.isActive(7))

        policy.clear()
        assertFalse(policy.isActive(0))
    }

    @Test
    fun resetClearsPriorSessionState() {
        val policy = SlotAggregatePolicy()
        policy.reset(listOf(0))
        policy.onSuccess(0)
        policy.onDisconnected(0)

        policy.reset(listOf(0, 1))

        assertTrue(policy.onSuccess(1).publishAggregate)
    }
}
