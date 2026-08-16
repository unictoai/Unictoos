package com.unictoai.unictoos.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.unictoai.unictoos.streaming.CaptureCompatibilityPolicy

/**
 * Surface used by RootEncoder to render the actual camera or screen capture.
 * The view deliberately reports lifecycle events instead of owning streaming
 * state, because the encoder lives in StreamingForegroundService.
 */
class PreviewSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs) {
    interface Listener {
        fun onSurfaceAvailable(surface: Surface, width: Int, height: Int)
        fun onSurfaceDestroyed(surface: Surface)
    }

    private var listener: Listener? = null
    private var lastSurface: Surface? = null
    private var callbackRegistered = false
    private var maxBufferWidth = 0
    private var maxBufferHeight = 0
    private var configuredBufferWidth = 0
    private var configuredBufferHeight = 0

    private val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            notifyAvailable(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            notifyAvailable(holder)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            val destroyedSurface = holder.surface
            if (lastSurface !== destroyedSurface) return
            lastSurface = null
            listener?.onSurfaceDestroyed(destroyedSurface)
        }
    }

    init {
        setZOrderOnTop(false)
    }

    /**
     * Limits the local preview buffer to the encoder profile without changing the
     * view's measured size or the stream output dimensions.
     */
    fun setPreviewBufferLimit(maxWidth: Int, maxHeight: Int) {
        maxBufferWidth = maxWidth
        maxBufferHeight = maxHeight
        configurePreviewBuffer()
    }

    fun setPreviewListener(listener: Listener?) {
        val listenerChanged = this.listener !== listener
        this.listener = listener
        if (listenerChanged && listener != null && holder.surface.isValid && width > 0 && height > 0) {
            post { notifyAvailable(holder) }
        }
    }

    fun releasePreviewListener() {
        // Do not synthesize surface destruction here. Android owns the holder
        // lifecycle and will deliver the real surfaceDestroyed callback.
        listener = null
        lastSurface = null
    }

    private fun notifyAvailable(holder: SurfaceHolder) {
        val availableWidth = configuredBufferWidth.takeIf { it > 0 } ?: width
        val availableHeight = configuredBufferHeight.takeIf { it > 0 } ?: height
        if (holder.surface.isValid && availableWidth > 0 && availableHeight > 0) {
            lastSurface = holder.surface
            listener?.onSurfaceAvailable(holder.surface, availableWidth, availableHeight)
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        configurePreviewBuffer()
    }

    private fun configurePreviewBuffer() {
        if (width <= 0 || height <= 0 || maxBufferWidth <= 0 || maxBufferHeight <= 0) return
        val target = CaptureCompatibilityPolicy.previewBufferSize(width, height, maxBufferWidth, maxBufferHeight)
        if (target.width <= 0 || target.height <= 0) return
        if (configuredBufferWidth == target.width && configuredBufferHeight == target.height) return
        configuredBufferWidth = target.width
        configuredBufferHeight = target.height
        holder.setFixedSize(target.width, target.height)
    }

    override fun onDetachedFromWindow() {
        releasePreviewListener()
        unregisterHolderCallback()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerHolderCallback()
    }

    private fun registerHolderCallback() {
        if (callbackRegistered) return
        holder.addCallback(callback)
        callbackRegistered = true
    }

    private fun unregisterHolderCallback() {
        if (!callbackRegistered) return
        holder.removeCallback(callback)
        callbackRegistered = false
    }
}
