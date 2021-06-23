package com.endeavride.endeavridedriver.ui.maps

import android.location.Location
import com.endeavride.endeavridedriver.ui.data.Result
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.endeavride.endeavridedriver.shared.NetworkUtils
import com.endeavride.endeavridedriver.ui.data.MapDataSource
import com.endeavride.endeavridedriver.ui.data.model.Ride
import com.endeavride.endeavridedriver.ui.data.model.RideDriveRecord
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MapsViewModel(
    val dataSource: MapDataSource
) : ViewModel() {
    private val _mapDirectionResult = MutableLiveData<MutableList<List<LatLng>>>()
    val mapDirectionResult: LiveData<MutableList<List<LatLng>>> = _mapDirectionResult

    private val _currentRide = MutableLiveData<Ride?>()
    val currentRide: LiveData<Ride?> = _currentRide

    private val _driveRecordResult = MutableLiveData<IOException?>()
    val driveRecordResult: LiveData<IOException?> = _driveRecordResult

    private var searchJob: Job? = null

    fun getDirection(origin: LatLng, dest: LatLng, waypoint: LatLng) {
        viewModelScope.launch {
            val result = dataSource.getDirection(origin, dest, waypoint)
            _mapDirectionResult.value = result
        }
    }

    fun requestAvailableRideTask(delayTime: Long = 0, offset: Int, rid: String?) {
        viewModelScope.launch {
            delay(delayTime)
            val result = dataSource.requestAvailableRideTask(offset, rid)
            if (result is Result.Success) {
                _currentRide.value = result.data
            } else {
                _currentRide.value = null
            }
        }
    }

    fun acceptRideRequest(rid: String) {
        viewModelScope.launch {
            val result = dataSource.acceptRequest(rid)
            if (result is Result.Success) {
                _currentRide.value = result.data
            }
        }
    }

    fun updateRideRequest(rid: String, status: Int) {
        viewModelScope.launch {
            val result = dataSource.updateRideRequest(rid, status)
            if (result is Result.Success) {
                _currentRide.value = result.data
            }
        }
    }

    fun postDriveRecord(status: Int, location: Location) {
        viewModelScope.launch {
            _currentRide.value?.let {
                val error = NetworkUtils.user?.let { it1 -> RideDriveRecord(it.rid, it.uid, it1.userId, status, "${location.latitude},${location.longitude}") }
                    ?.let { it2 -> dataSource.postDriveRecord(it2) }
                delay(3000)
                _driveRecordResult.value = error
            }
        }
    }

    fun checkIfCurrentRideAvailable() {
        viewModelScope.launch {
            val result = dataSource.checkIfCurrentRideAvailable()
            println("#K_check current ride result: $result")
            if (result is Result.Success) {
                _currentRide.value = result.data
            } else {
                println("#K_current ride result error")
            }
        }
    }
}