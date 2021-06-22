package com.endeavride.endeavridedriver.shared

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

        init{
            FuelManager.instance.baseHeaders = mapOf(
                "User-Agent" to "DemoApp ENDEAVRideUser",
                "Content-Type" to "application/json"
            )
            FuelManager.instance.basePath = "http://10.0.2.2:3300/"
        }

        suspend fun getRequestWithFullPath(path: String, parameters: Parameters?): RequestResultModel {
            val (request, response, result) = Fuel.get(path, parameters).awaitStringResponseResult()
            println(request)
            println(response)
            val (bytes, error) = result
            if (bytes != null) {
                println("[response bytes] $bytes")
            }
            if (error != null) {
                println("fuel get request error: $error")
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
                println("[response bytes] $bytes")
            }
            if (error != null) {
                println("fuel get request error: $error")
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
                println("[response bytes] $bytes")
            }
            if (error != null) {
                println("fuel post request error: $error")
            }
            return RequestResultModel(bytes, error)
        }
    }
}