package com.endeavride.endeavridedriver.ui.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ride (
    val rid: String,
    val type: Int = 0,  // 0 = ride service, 1 = home service
    val status: Int,  //status: 0 = unassigned, 1 = assigning, 2 = picking up, 3 = arrived user location, 4 = ride start, 5 = finished, 6 = canceled
    val uid: String,
    val did: String? = null,
    val user_location: String?,
    val destination: String,
    val create_time: String? = null,
    val start_time: String? = null,
    val finish_time: String? = null
)

@Serializable
data class RideRequest (
    val rid: String,
    val type: Int = 0,
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