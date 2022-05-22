package com.github.akizhan.oflinemapapplication.listeners

import android.location.Location
import android.location.LocationListener

class MyLocationListener(private val locListenerInterface: LocListenerInterface): LocationListener {
    override fun onLocationChanged(location: Location) {
        locListenerInterface.onLocationChanged(location.latitude, location.longitude)
    }
}