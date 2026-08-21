package com.unictoai.unictoos.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureCapabilityCatalogTest {
    @Test
    fun catalogIncludesLocalAndBoundedExternalStatuses() {
        val capabilities = FeatureCapabilityCatalog.all()
        assertTrue(capabilities.any { it.status == CapabilityStatus.AVAILABLE })
        assertTrue(capabilities.any { it.status == CapabilityStatus.DEVICE_VALIDATION })
        assertTrue(capabilities.any { it.status == CapabilityStatus.INTEGRATION_READY })
        assertTrue(capabilities.any { it.status == CapabilityStatus.SERVICE_REQUIRED })
    }
}
