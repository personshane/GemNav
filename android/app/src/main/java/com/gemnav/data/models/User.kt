package com.gemnav.data.models

data class User(
    val id: String,
    val tier: Tier,
    val createdAt: Long
)
