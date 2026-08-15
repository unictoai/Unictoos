package com.unictoai.unictoos.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

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

    private val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            notifyAvailable(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            notifyAvailable(holder)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            lastSurface = null
            listener?.onSurfaceDestroyed(holder.surface)
        }
    }

    init {
        holder.addCallback(callback)
        setZOrderOnTop(false)
    }

    fun setPreviewListener(listener: Listener?) {
        val listenerChanged = this.listener !== listener
        this.listener = listener
        if (listenerChanged && listener != null && holder.surface.isValid && width > 0 && height > 0) {
            post { notifyAvailable(holder) }
        }
    }

    fun releasePreviewListener() {
        lastSurface?.let { surface -> listener?.onSurfaceDestroyed(surface) }
        lastSurface = null
        listener = null
    }

    private fun notifyAvailable(holder: SurfaceHolder) {
        if (holder.surface.isValid && width > 0 && height > 0) {
            lastSurface = holder.surface
            listener?.onSurfaceAvailable(holder.surface, width, height)
        }
    }

    override fun onDetachedFromWindow() {
        releasePreviewListener()
        holder.removeCallback(callback)
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        holder.addCallback(callback)
    }
}
