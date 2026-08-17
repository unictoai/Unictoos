package com.unictoai.unictoos.streaming

/**
 * Surface identity is independent from dimensions. A new Android Surface with the same
 * width/height is still a replacement and must not reuse the old preview attachment.
 */
object PreviewSurfaceIdentityPolicy {
    fun shouldReuse(
        currentToken: Long,
        incomingToken: Long,
        sameSurfaceObject: Boolean,
        currentWidth: Int,
        currentHeight: Int,
        incomingWidth: Int,
        incomingHeight: Int,
    ): Boolean =
        currentToken > 0L &&
            incomingToken > 0L &&
            currentToken == incomingToken &&
            sameSurfaceObject &&
            currentWidth == incomingWidth &&
            currentHeight == incomingHeight

    fun isStaleDetach(activeToken: Long, detachToken: Long): Boolean =
        activeToken > 0L && detachToken != activeToken
}
