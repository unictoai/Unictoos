package com.unictoai.unictoos.streaming

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncoderCrashPolicyTest {
    @Test
    fun recognizesConfirmedRootEncoderGlOutOfMemoryFailure() {
        val failure = RuntimeException(
            "drawScreen end. GL error: 1285",
            null,
        ).apply {
            stackTrace = arrayOf(
                StackTraceElement("com.pedro.encoder.utils.gl.GlUtil", "checkGlError", "GlUtil.java", 144),
            )
        }

        assertTrue(EncoderCrashPolicy.isRecoverableGraphicsFailure("pool-4-thread-1", failure))
    }

    @Test
    fun recognizesNestedGlOutOfMemoryFailure() {
        val failure = RuntimeException(
            "render thread failed",
            RuntimeException("GL_OUT_OF_MEMORY"),
        ).apply {
            stackTrace = arrayOf(
                StackTraceElement("com.pedro.encoder.utils.gl.GlUtil", "checkGlError", "GlUtil.java", 144),
            )
        }

        assertTrue(EncoderCrashPolicy.isRecoverableGraphicsFailure("encoder-render", failure))
    }

    @Test
    fun doesNotInterceptUnrelatedFailure() {
        val failure = IllegalStateException("camera permission revoked").apply {
            stackTrace = arrayOf(
                StackTraceElement("com.pedro.encoder.camera.Camera2Source", "start", "Camera2Source.java", 88),
            )
        }

        assertFalse(EncoderCrashPolicy.isRecoverableGraphicsFailure("pool-4-thread-1", failure))
    }

    @Test
    fun doesNotInterceptGlFailureOnUnrelatedThread() {
        val failure = RuntimeException("GL error: 1285").apply {
            stackTrace = arrayOf(
                StackTraceElement("com.pedro.encoder.utils.gl.GlUtil", "checkGlError", "GlUtil.java", 144),
            )
        }

        assertFalse(EncoderCrashPolicy.isRecoverableGraphicsFailure("main", failure))
    }
}
