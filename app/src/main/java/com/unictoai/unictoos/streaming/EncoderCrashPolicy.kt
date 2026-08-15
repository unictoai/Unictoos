package com.unictoai.unictoos.streaming

/**
 * Classifies uncaught failures from RootEncoder's render threads.
 *
 * Only the confirmed GL_OUT_OF_MEMORY signature is intercepted by the app-level
 * safety net. Unknown exceptions remain eligible for the previous process
 * handler so unrelated defects are not silently swallowed.
 */
object EncoderCrashPolicy {
    const val GRAPHICS_RESOURCE_MESSAGE = "Preview stopped: graphics resources exhausted. Restart capture and try again"

    fun isRecoverableGraphicsFailure(threadName: String?, throwable: Throwable): Boolean {
        val text = generateSequence(throwable) { it.cause }
            .joinToString(" ") { it.message.orEmpty() }
            .lowercase()
        val rootEncoderFrame = throwable.stackTrace.any { it.className.startsWith("com.pedro.encoder") }
        val renderThread = threadName?.startsWith("pool-") == true || threadName?.contains("encoder", ignoreCase = true) == true
        val glOutOfMemory = text.contains("gl error: 1285") || text.contains("gl_out_of_memory")
        val drawScreenFailure = text.contains("drawscreen end") && glOutOfMemory
        return rootEncoderFrame && renderThread && (glOutOfMemory || drawScreenFailure)
    }
}
