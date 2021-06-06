package com.endeavride.endeavridedriver.ui.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ride (
    val rid: String,
    val status: Int,
    val uid: String,
    val did: String? = null,
    val direction: String,
    val create_time: String? = null,
    val start_time: String? = null,
    val finish_time: String? = null
)

@Serializable
data class RideRequest (
    val direction: String,
    val uid: String
)