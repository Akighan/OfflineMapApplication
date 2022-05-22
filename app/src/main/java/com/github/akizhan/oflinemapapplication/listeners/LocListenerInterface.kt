package com.github.akizhan.oflinemapapplication.listeners

import android.location.Location

interface LocListenerInterface {
    fun onLocationChanged(lat:Double, lng:Double)
}