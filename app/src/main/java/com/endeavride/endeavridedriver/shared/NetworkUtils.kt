package com.endeavride.endeavridedriver.shared

import android.util.Log
import com.endeavride.endeavridedriver.ui.data.model.LoggedInUser
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult

data class RequestResultModel(val resData: String?, val error: FuelError?)

class NetworkUtils {
    companion object {

        val shared: NetworkUtils = NetworkUtils()
        var user: LoggedInUser? = null
        private const val TAG = "NetworkUtils"

        init{
            FuelManager.instance.baseHeaders = mapOf(
                "User-Agent" to "DemoApp ENDEAVRideUser",
                "Content-Type" to "application/json"
            )
            FuelManager.instance.basePath = "http://ec2-18-220-53-8.us-east-2.compute.amazonaws.com:3300/"  //ec2 server
//            FuelManager.instance.basePath = "https://10.0.2.2:8443/"   //local server with ssl certificate setup
//            FuelManager.instance.basePath = "http://10.0.2.2:3300/"    //local server, 10.0.2.2 represent localhost when running on Android simulator, change to your local server IP address before use
        }

        suspend fun getRequestWithFullPath(path: String, parameters: Parameters?): RequestResultModel {
            val (request, response, result) = Fuel.get(path, parameters).awaitStringResponseResult()
            println(request)
            val (bytes, error) = result
            if (error != null) {
                Log.e(TAG, "Fuel get request with full path error: $error")
            }
            return RequestResultModel(bytes, error)
        }

        suspend fun getRequest(path: String, parameters: Parameters?): RequestResultModel {
            val fuelRequest = FuelManager.instance.get(path, parameters)
            user?.userId?.let { fuelRequest.appendHeader("did", it) }
            val (request, response, result) = fuelRequest.awaitStringResponseResult()
            println(request)
            println(response)
            val (bytes, error) = result
            if (bytes != null) {
                Log.d(TAG, "[response bytes] $bytes")
            }
            if (error != null) {
                Log.e(TAG, "Fuel get request error: $error")
            }
            return RequestResultModel(bytes, error)
        }

        suspend fun postRequest(path: String, body: String): RequestResultModel {
            val fuelRequest = FuelManager.instance.post(path)
            user?.userId?.let { fuelRequest.appendHeader("did", it) }
            val (request, response, result) = fuelRequest.body(body).awaitStringResponseResult()
            println(request)
            println(response)
            val (bytes, error) = result
            if (bytes != null) {
                Log.d(TAG, "[response bytes] $bytes")
            }
            if (error != null) {
                Log.e(TAG, "Fuel post request error: $error")
            }
            return RequestResultModel(bytes, error)
        }
    }
}