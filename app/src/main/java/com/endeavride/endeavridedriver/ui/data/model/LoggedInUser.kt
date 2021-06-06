package com.endeavride.endeavridedriver.ui.data.model

import kotlinx.serialization.Serializable

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val userId: String,
    val displayName: String
)

@Serializable
data class User (
    val uid: String,
    val email: String
)

@Serializable
data class UserLoginRequestModel (
    val email: String,
    val hash: String
)