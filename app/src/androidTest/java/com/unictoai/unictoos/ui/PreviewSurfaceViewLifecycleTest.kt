package com.unictoai.unictoos.ui

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSurfaceViewLifecycleTest {
    @Test
    fun releasingPreviewListenerDoesNotSynthesizeSurfaceDestroyed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var destroyedCount = 0
        val view = PreviewSurfaceView(context)
        view.setPreviewListener(object : PreviewSurfaceView.Listener {
            override fun onSurfaceAvailable(surface: android.view.Surface, width: Int, height: Int) = Unit
            override fun onSurfaceDestroyed(surface: android.view.Surface) { destroyedCount++ }
        })

        view.releasePreviewListener()

        assertEquals(0, destroyedCount)
    }

    @Test
    fun repeatedPreviewListenerRecreateCyclesDoNotEmitSyntheticDestroyEvents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var destroyedCount = 0
        repeat(50) {
            val view = PreviewSurfaceView(context)
            view.setPreviewListener(object : PreviewSurfaceView.Listener {
                override fun onSurfaceAvailable(surface: android.view.Surface, width: Int, height: Int) = Unit
                override fun onSurfaceDestroyed(surface: android.view.Surface) { destroyedCount++ }
            })
            view.releasePreviewListener()
            view.releasePreviewListener()
        }

        assertEquals(0, destroyedCount)
    }
}
