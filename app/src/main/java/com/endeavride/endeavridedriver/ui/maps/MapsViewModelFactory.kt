package com.endeavride.endeavridedriver.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.endeavride.endeavridedriver.ui.data.MapDataSource

class MapsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            return MapsViewModel(
                dataSource = MapDataSource()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}