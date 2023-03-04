package net.faizud.exam.network

import com.squareup.moshi.Json

data class Status (
    @Json(name="enable")
    val enable: Boolean
)