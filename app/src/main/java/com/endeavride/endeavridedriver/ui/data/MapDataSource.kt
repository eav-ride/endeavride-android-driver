package com.endeavride.endeavridedriver.ui.data

import android.util.Log
import com.endeavride.endeavridedriver.shared.NetworkUtils
import com.endeavride.endeavridedriver.ui.data.model.Ride
import com.endeavride.endeavridedriver.ui.data.model.RideDriveRecord
import com.endeavride.endeavridedriver.ui.data.model.RideRequest
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.IOException

class MapDataSource {

    private val mapsKey = "AIzaSyAxxnazPy8mIAROs-chSCrDknDvzyB3Vho"

    suspend fun requestAvailableRideTask(offset: Int, rid: String?, receiveNew: Boolean): Result<Ride> {
        try {
            val result = NetworkUtils.getRequest("r/d/request?", listOf("offset" to offset, "rid" to rid, "receive_new" to receiveNew))

            if (result.resData != null) {
                val ride = Json.decodeFromString<Ride>(result.resData)
                return Result.Success(ride)
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("create ride failed $e", e))
        }
    }

    suspend fun acceptRequest(rid: String, type: Int): Result<Ride> {
        try {
            val result = NetworkUtils.postRequest("r/d", Json.encodeToString(mapOf("rid" to rid)))

            if (result.resData != null) {
                val ride = Json.decodeFromString<Ride>(result.resData)
                return Result.Success(ride)
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("accept ride failed $e", e))
        }
    }

    suspend fun updateRideRequest(rid: String, status: Int): Result<Ride> {
        try {
            val result = NetworkUtils.postRequest("r/$rid", Json.encodeToString(mapOf("status" to status)))

            if (result.resData != null) {
                val ride = Json.decodeFromString<Ride>(result.resData)
                return Result.Success(ride)
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("update ride failed $e", e))
        }
    }

    suspend fun getDirection(origin: LatLng, dest: LatLng, waypoint: LatLng?): MutableList<List<LatLng>> {
        val params = if (waypoint == null) {
            listOf(
                "origin" to "${origin.latitude},${origin.longitude}",
                "destination" to "${dest.latitude},${dest.longitude}",
                "key" to mapsKey,
            )
        } else {
            listOf(
                "origin" to "${origin.latitude},${origin.longitude}",
                "destination" to "${dest.latitude},${dest.longitude}",
                "waypoints" to "${waypoint.latitude},${waypoint.longitude}",
                "key" to mapsKey,
            )
        }
        val result = NetworkUtils.getRequestWithFullPath("https://maps.googleapis.com/maps/api/directions/json",
            params)
        val path: MutableList<List<LatLng>> = ArrayList()
        if (result.resData == null) {
            return path
        }
        val jsonResponse = JSONObject(result.resData)
        // Get routes
        val routes = jsonResponse.getJSONArray("routes")
        if (routes.length() < 1) {
            return path
        }
        println("#K_routes count: ${routes.length()}")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        println("#K_legs count: ${legs.length()}")
        for (j in 0 until legs.length()) {
            val steps = legs.getJSONObject(j).getJSONArray("steps")
            println("#K_steps count: ${steps.length()}")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
//            Log.d("Test", "#K_points: $points")
                path.add(PolyUtil.decode(points))
            }
        }
        return path
    }

    suspend fun checkIfCurrentRideAvailable(): Result<Ride> {
        try {
            val result = NetworkUtils.getRequest("r/d/", null)
            if (result.resData != null) {
                val ride = Json.decodeFromString<Ride>(result.resData)
                return Result.Success(ride)
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("check in progress (current) ride failed $e", e))
        }
    }

    // drive record
    suspend fun postDriveRecord(record: RideDriveRecord): IOException? {
        return try {
            val result = NetworkUtils.postRequest("dr/", Json.encodeToString(record))

    //            if (result.resData != null) {
    //                val ride = Json.decodeFromString<Ride>(result.resData)
    //                return Result.Success(ride)
    //            }
            null
        } catch (e: Throwable) {
            IOException("accept ride failed $e", e)
        }
    }
}