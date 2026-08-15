package com.unictoai.unictoos

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Production factory for StudioViewModel.
 *
 * The ViewModel has injectable repository parameters for JVM tests. Android's
 * default factory only knows how to reflect on an Application constructor, so
 * this factory makes the production construction path explicit and resilient
 * to future constructor changes.
 */
class StudioViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(StudioViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("StudioViewModelFactory requires an Application in CreationExtras")
        @Suppress("UNCHECKED_CAST")
        return StudioViewModel(application) as T
    }
}
