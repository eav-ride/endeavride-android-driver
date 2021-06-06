package com.endeavride.endeavridedriver.ui.maps

import com.endeavride.endeavridedriver.ui.data.Result
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.endeavride.endeavridedriver.ui.data.MapDataSource
import com.endeavride.endeavridedriver.ui.data.model.Ride
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapsViewModel(
    val dataSource: MapDataSource
) : ViewModel() {
    private val _mapDirectionResult = MutableLiveData<MutableList<List<LatLng>>>()
    val mapDirectionResult: LiveData<MutableList<List<LatLng>>> = _mapDirectionResult

    private val _currentRide = MutableLiveData<Ride>()
    val currentRide: LiveData<Ride> = _currentRide

    private var searchJob: Job? = null

    fun getDirection(origin: LatLng, dest: LatLng, waypoint: LatLng) {
        viewModelScope.launch {
            val result = dataSource.getDirection(origin, dest, waypoint)
            _mapDirectionResult.value = result
        }
    }

    fun requestAvailableRideTask(offset: Int, rid: String?) {
        viewModelScope.launch {
            val result = dataSource.requestAvailableRideTask(offset, rid)
            if (result is Result.Success) {
                _currentRide.value = result.data
            }
        }
    }

//    fun checkIfCurrentRideAvailable() {
//        viewModelScope.launch {
//            val result = dataSource.checkIfCurrentRideAvailable()
//            println("#K_check current ride result: $result")
//            if (result is Result.Success) {
//                _currentRide.value = result.data
//            } else {
//                println("#K_current ride result error")
//            }
//        }
//    }
}