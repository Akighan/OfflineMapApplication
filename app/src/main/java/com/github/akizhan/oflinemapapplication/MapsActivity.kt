package com.github.akizhan.oflinemapapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.github.akizhan.oflinemapapplication.databinding.ActivityMapsBinding
import com.github.akizhan.oflinemapapplication.entiies.User
import com.github.akizhan.oflinemapapplication.listeners.LocListenerInterface
import com.github.akizhan.oflinemapapplication.listeners.MyLocationListener
import com.github.akizhan.oflinemapapplication.listeners.PositionChangeListener

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocListenerInterface {

    private var mainUser: User = User("Hi", 56.296504, 43.936059, null)
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var myLocationListener: MyLocationListener
    private lateinit var locationManager: LocationManager

    private fun init() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        myLocationListener = MyLocationListener(this)
        checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == RESULT_OK) {
           checkPermissions()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, myLocationListener)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        init()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        var userPosition = LatLng(mainUser.lat, mainUser.lng)
        val userMarker = MarkerOptions().position(userPosition).title(mainUser.name)
        mainUser.positionChangeListener = object : PositionChangeListener {
            override fun onPositionChange() {
                runOnUiThread {
                    userPosition = LatLng(mainUser.lat, mainUser.lng)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 17f))
                    mMap.addMarker(userMarker.position(userPosition))
                }
            }
        }
    }

    override fun onLocationChanged(lat: Double, lng: Double) {
        mainUser.lat = lat
        mainUser.lng = lng
        mainUser.positionChangeListener?.onPositionChange()
    }
}