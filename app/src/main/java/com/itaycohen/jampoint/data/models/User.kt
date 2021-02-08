package com.itaycohen.jampoint.data.models

data class User(
    val id: String,
    val fullName: String,
    val email: String?,
    val imageUrl: String?,
    val mainInstrument: String?
)