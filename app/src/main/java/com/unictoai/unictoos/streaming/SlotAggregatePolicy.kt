package com.unictoai.unictoos.streaming

/**
 * Pure decision logic for multi-destination aggregate signaling.
 *
 * The session goes LIVE when the first destination connects; a slow or broken
 * secondary destination must never hold the session at CONNECTING or tear
 * down a healthy peer. Failures are classified per slot:
 *
 * - A slot fails while a peer is healthy -> retry only that slot.
 * - A slot fails while a peer is still attempting -> wait; the peer's outcome
 *   decides, and the failed slot is retried once the session is live.
 * - Every slot has reached a terminal failed state -> publish the aggregate
 *   failure once so the service can run its full session retry policy.
 * - A mid-session disconnect with no healthy peer left -> aggregate at once;
 *   the service owns backoff, attempt budget and watchdog.
 *
 * Authentication failures never trigger a slot retry (a retry cannot fix a
 * bad key) and never kill a healthy peer.
 *
 * All state is guarded by an internal lock; every method is safe to call from
 * RootEncoder's callback threads.
 */
class SlotAggregatePolicy {
    private val lock = Any()
    private val activeSlots = linkedSetOf<Int>()
    private val connectedSlots = mutableSetOf<Int>()
    private val failedSlots = mutableSetOf<Int>()
    private val authFailedSlots = mutableSetOf<Int>()
    private var aggregatePublished = false
    private var failureReported = false
    private var disconnectReported = false
    private var authErrorReported = false

    data class SuccessResult(val publishAggregate: Boolean, val retrySlots: List<Int>)
    data class FailureResult(val retrySelf: Boolean, val publishAggregate: Boolean)
    data class DisconnectResult(val retrySelf: Boolean, val publishAggregate: Boolean)

    /**
     * Aggregate outcome of a slot authentication error.
     *
     * - [NOTHING]: a peer is healthy, or peers are still attempting, or an
     *   aggregate was already published for this round.
     * - [PUBLISH_AUTH_ERROR]: every destination rejected the key. The session
     *   cannot recover by retrying, so the service must stop, not reconnect.
     * - [PUBLISH_FAILURE]: every destination reached a terminal state but at
     *   least one failed for a non-auth reason. The session is retryable, so
     *   the service should run its normal reconnect policy instead of
     *   treating the whole session as an auth failure.
     */
    enum class AuthOutcome { NOTHING, PUBLISH_AUTH_ERROR, PUBLISH_FAILURE }

    fun reset(slots: List<Int>) = synchronized(lock) {
        activeSlots.clear()
        activeSlots.addAll(slots)
        connectedSlots.clear()
        failedSlots.clear()
        authFailedSlots.clear()
        aggregatePublished = false
        failureReported = false
        disconnectReported = false
        authErrorReported = false
    }

    fun clear() = synchronized(lock) {
        activeSlots.clear()
        connectedSlots.clear()
        failedSlots.clear()
        authFailedSlots.clear()
    }

    fun isActive(slot: Int): Boolean = synchronized(lock) { slot in activeSlots }

    /** Slot whose bitrate/started signal represents the session, if any. */
    fun telemetrySlot(): Int? = synchronized(lock) {
        connectedSlots.firstOrNull { it in activeSlots } ?: activeSlots.firstOrNull()
    }

    fun onStarted(slot: Int) = synchronized(lock) { failedSlots.remove(slot) }

    fun onSuccess(slot: Int): SuccessResult = synchronized(lock) {
        connectedSlots += slot
        failedSlots.remove(slot)
        val publish = connectedSlots.isNotEmpty() && !aggregatePublished
        if (publish) aggregatePublished = true
        val retry = failedSlots
            .filter { it != slot && it !in authFailedSlots && it in activeSlots }
            .onEach { failedSlots.remove(it) }
        SuccessResult(publishAggregate = publish, retrySlots = retry)
    }

    fun onFailed(slot: Int): FailureResult = synchronized(lock) {
        connectedSlots.remove(slot)
        failedSlots += slot
        if (hasHealthyPeer(slot)) return FailureResult(retrySelf = true, publishAggregate = false)
        if (!peersExhausted(slot)) return FailureResult(retrySelf = false, publishAggregate = false)
        val publish = !failureReported
        if (publish) failureReported = true
        FailureResult(retrySelf = false, publishAggregate = publish)
    }

    fun onDisconnected(slot: Int): DisconnectResult = synchronized(lock) {
        connectedSlots.remove(slot)
        if (hasHealthyPeer(slot)) return DisconnectResult(retrySelf = true, publishAggregate = false)
        val publish = !disconnectReported
        if (publish) disconnectReported = true
        DisconnectResult(retrySelf = false, publishAggregate = publish)
    }

    /** Returns how the aggregate should treat a slot authentication error. */
    fun onAuthError(slot: Int): AuthOutcome = synchronized(lock) {
        connectedSlots.remove(slot)
        failedSlots += slot
        authFailedSlots += slot
        if (hasHealthyPeer(slot)) return AuthOutcome.NOTHING
        if (failureReported) return AuthOutcome.NOTHING
        if (activeSlots.all { it in authFailedSlots }) {
            val publish = !authErrorReported
            if (publish) authErrorReported = true
            return if (publish) AuthOutcome.PUBLISH_AUTH_ERROR else AuthOutcome.NOTHING
        }
        if (!peersExhausted(slot)) return AuthOutcome.NOTHING
        failureReported = true
        AuthOutcome.PUBLISH_FAILURE
    }

    private fun hasHealthyPeer(exceptSlot: Int): Boolean =
        connectedSlots.any { it != exceptSlot && it in activeSlots }

    private fun peersExhausted(exceptSlot: Int): Boolean =
        activeSlots.none { it != exceptSlot && it !in failedSlots && it !in authFailedSlots }
}
