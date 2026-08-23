package com.unictoai.unictoos.stream

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.view.GlInterface
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.PipGeometryPolicy

/**
 * Optional screen-plus-camera composition using RootEncoder's supported GL
 * filter surface. The primary video source remains the screen source owned by
 * the service; this class adds a second camera texture as a bounded PiP layer.
 * If the device cannot create the secondary camera, the caller can continue
 * with the screen-only source instead of blocking stream startup.
 */
class CompositorSurface(
    private val context: Context,
    private val glInterface: GlInterface,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val frameFps: Int,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pipFilter: SurfaceFilterRender? = null
    private var pipCamera: Camera2Source? = null
    private var released = false

    fun start(config: PipConfig): Boolean {
        if (released || !config.enabled) return false
        if (pipFilter != null || pipCamera != null) return true
        return runCatching {
            val camera = Camera2Source(context)
            val filter = SurfaceFilterRender { texture ->
                mainHandler.post {
                    if (released || pipCamera !== camera) return@post
                    runCatching {
                        check(camera.init(frameWidth, frameHeight, frameFps.coerceIn(15, 60), 0)) {
                            "PiP camera could not initialize"
                        }
                        camera.start(texture)
                    }.onFailure { releaseSecondaryCamera(camera) }
                }
            }
            val frameAspect = frameWidth.toFloat() / frameHeight.coerceAtLeast(1).toFloat()
            val rect = PipGeometryPolicy.rect(config, frameAspect)
            filter.setScale((rect.right - rect.left) * 100f, (rect.bottom - rect.top) * 100f)
            filter.setPosition(rect.left * 100f, rect.top * 100f)
            glInterface.addFilter(filter)
            pipCamera = camera
            pipFilter = filter
        }.isSuccess
    }

    fun switchCamera(): Boolean = runCatching {
        check(!released) { "PiP compositor is released" }
        pipCamera?.switchCamera() ?: error("PiP camera is not active")
    }.isSuccess

    fun release() {
        if (released) return
        released = true
        val camera = pipCamera
        pipCamera = null
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { camera?.stop() }
        runCatching { camera?.release() }
        val filter = pipFilter
        pipFilter = null
        runCatching { filter?.let(glInterface::removeFilter) }
        runCatching { filter?.release() }
    }

    private fun releaseSecondaryCamera(camera: Camera2Source) {
        if (pipCamera === camera) pipCamera = null
        runCatching { camera.stop() }
        runCatching { camera.release() }
        val filter = pipFilter
        pipFilter = null
        runCatching { filter?.let(glInterface::removeFilter) }
        runCatching { filter?.release() }
    }
}
