package com.github.akizhan.oflinemapapplication.entiies

import com.github.akizhan.oflinemapapplication.listeners.PositionChangeListener


data class User(
    var name: String,
    var lat: Double,
    var lng: Double,
    var positionChangeListener: PositionChangeListener?
)