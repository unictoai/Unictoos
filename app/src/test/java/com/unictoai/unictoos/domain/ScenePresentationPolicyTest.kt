package com.unictoai.unictoos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePresentationPolicyTest {
    @Test
    fun normalizesGroupsToExistingSourceIds() {
        val scene = Scene(
            id = "scene",
            name = "Scene",
            sources = listOf(Source("camera", "Camera", SourceType.CAMERA)),
            sourceGroups = listOf(SourceGroup("g", "Host", listOf("camera", "missing"))),
        )
        assertEquals(listOf("camera"), ScenePresentationPolicy.normalizeGroups(scene).single().sourceIds)
    }

    @Test
    fun rejectsEmptyGroupsAndKeepsLiveTransitionsGated() {
        val scene = Scene(
            id = "scene",
            name = "Scene",
            sources = listOf(Source("camera", "Camera", SourceType.CAMERA)),
            sourceGroups = listOf(SourceGroup("g", "Empty", listOf("missing"))),
        )
        assertTrue(ScenePresentationPolicy.normalizeGroups(scene).isEmpty())
        assertTrue(ScenePresentationPolicy.canApplyLiveTransition(StreamStatus.IDLE))
        assertFalse(ScenePresentationPolicy.canApplyLiveTransition(StreamStatus.LIVE))
    }
}
