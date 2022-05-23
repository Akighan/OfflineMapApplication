package com.github.akizhan.oflinemapapplication.entiies

import com.google.gson.annotations.SerializedName


data class User(
    @SerializedName("User")
    var name: String,
    var lat: Double,
    var lng: Double,
)