package com.endeavride.endeavridedriver.ui.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ride (
    val rid: String,
    val status: Int,
    val uid: String,
    val did: String? = null,
    val user_location: String,
    val destination: String,
    val create_time: String? = null,
    val start_time: String? = null,
    val finish_time: String? = null
)

@Serializable
data class RideRequest (
    val rid: String,
    val status: Int? = null
)

@Serializable
data class RideDriveRecord (
    val rid: String,
    val uid: String,
    val did: String,
    val status: Int,
    val driver_location: String,
    val create_time: String? = null
)