package com.endeavride.endeavridedriver.ui.data

import com.endeavride.endeavridedriver.shared.NetworkUtils
import com.endeavride.endeavridedriver.ui.data.model.LoggedInUser
import com.endeavride.endeavridedriver.ui.data.model.User
import com.endeavride.endeavridedriver.ui.data.model.UserLoginRequestModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            val result = NetworkUtils.postRequest("driver",
                Json.encodeToString(UserLoginRequestModel(username, password)))

            if (result.resData != null) {
                val user = Json.decodeFromString<User>(result.resData)
                return Result.Success(LoggedInUser(user.did, user.email))
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("login failed $e", e))
        }
    }

    suspend fun register(username: String, password: String): Result<LoggedInUser> {
        try {
            val result = NetworkUtils.postRequest(
                "driver/register",
                Json.encodeToString(UserLoginRequestModel(username, password))
            )

            if (result.resData != null) {
                val user = Json.decodeFromString<User>(result.resData)
                return Result.Success(LoggedInUser(user.did, user.email))
            }
            return Result.Error(IOException(result.error))
        } catch (e: Throwable) {
            return Result.Error(IOException("register failed $e", e))
        }
    }

    fun loadUserInfoIfAvailable(): Result<LoggedInUser> {
//        try {
//            // TODO: read user token and valid from server
//            val fakeUser = LoggedInUser(UUID.randomUUID().toString(), "Jane Doe")
//            return Result.Success(fakeUser)
//        } catch (e: Throwable) {
        return Result.Error(IOException("No available user"))
//        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}