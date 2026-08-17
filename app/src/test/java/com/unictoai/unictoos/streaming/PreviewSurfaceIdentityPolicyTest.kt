package com.unictoai.unictoos.streaming

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSurfaceIdentityPolicyTest {
    @Test
    fun sameDimensionsWithDifferentSurfaceTokenIsReplacement() {
        assertFalse(
            PreviewSurfaceIdentityPolicy.shouldReuse(
                currentToken = 41L,
                incomingToken = 42L,
                sameSurfaceObject = false,
                currentWidth = 1280,
                currentHeight = 720,
                incomingWidth = 1280,
                incomingHeight = 720,
            ),
        )
    }

    @Test
    fun sameSurfaceObjectAndTokenCanReuseAttachment() {
        assertTrue(
            PreviewSurfaceIdentityPolicy.shouldReuse(
                currentToken = 41L,
                incomingToken = 41L,
                sameSurfaceObject = true,
                currentWidth = 1280,
                currentHeight = 720,
                incomingWidth = 1280,
                incomingHeight = 720,
            ),
        )
    }

    @Test
    fun missingDetachTokenCannotDetachActiveSurface() {
        assertTrue(PreviewSurfaceIdentityPolicy.isStaleDetach(activeToken = 42L, detachToken = 0L))
    }

    @Test
    fun staleDestroyCannotDetachNewSurface() {
        assertTrue(PreviewSurfaceIdentityPolicy.isStaleDetach(activeToken = 42L, detachToken = 41L))
        assertFalse(PreviewSurfaceIdentityPolicy.isStaleDetach(activeToken = 42L, detachToken = 42L))
    }
}
