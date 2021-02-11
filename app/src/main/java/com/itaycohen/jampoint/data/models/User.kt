package com.itaycohen.jampoint.data.models

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null,
    val mainInstrument: String? = null
)