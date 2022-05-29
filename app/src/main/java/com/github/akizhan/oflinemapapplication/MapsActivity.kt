package com.github.akizhan.oflinemapapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.akizhan.oflinemapapplication.databinding.ActivityMapsBinding
import com.github.akizhan.oflinemapapplication.entiies.User
import com.github.akizhan.oflinemapapplication.listeners.ApiService
import com.github.akizhan.oflinemapapplication.listeners.LocListenerInterface
import com.github.akizhan.oflinemapapplication.listeners.MyLocationListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timer

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocListenerInterface {

    private var mainUser: User = User("", 56.296504, 43.936059)
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var myLocationListener: MyLocationListener
    private lateinit var locationManager: LocationManager
    private lateinit var apiService: ApiService
    private lateinit var alertDialog: AlertDialog

    private lateinit var changeNameAcceptButton: Button
    private lateinit var changeNameEditText: EditText
    private lateinit var userNamePreferences: SharedPreferences
    private lateinit var userToken: SharedPreferences
    private lateinit var getUsersTimer:Timer
    private lateinit var getUsersExecutorService: ExecutorService
    private var gpsEnableStatus = false


    companion object {
        const val APP_PREFERENCES_USER_NAME = "UserName"
        const val APP_PREFERENCES_USER_TOKEN = "UserToken"
    }

    private fun init() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        myLocationListener = MyLocationListener(this)
        checkPermissionsAndUpdateGps()
        userNamePreferences = getSharedPreferences(APP_PREFERENCES_USER_NAME, Context.MODE_PRIVATE)
        userNamePreferences = getSharedPreferences(APP_PREFERENCES_USER_TOKEN, Context.MODE_PRIVATE)
        mainUser.name = userNamePreferences.getString(APP_PREFERENCES_USER_NAME, "").toString()
        apiService = ApiService.create()
        openChangeNamePopUp()
        getToken()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == RESULT_OK) {
            checkPermissionsAndUpdateGps()
        }
    }

    private fun checkPermissionsAndUpdateGps(): Boolean {
        var result = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        } else {
            result = changeGpsUpdatesStatus()
        }
        return result
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

    override fun onDestroy() {
        if (this::getUsersTimer.isInitialized) {
            getUsersTimer.cancel()
        }
        if (this::getUsersExecutorService.isInitialized) {
            getUsersExecutorService.shutdown()
        }

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.name_button -> {
                item.setOnMenuItemClickListener {
                    openChangeNamePopUp()
                    true
                }
            }
            R.id.gps_button -> {
                item.setOnMenuItemClickListener {
                    if (checkPermissionsAndUpdateGps()) {
                        Toast.makeText(this, "GPS location updates turned ON", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(this, "GPS location updates turned OFF", Toast.LENGTH_SHORT)
                            .show()
                    }
                    true
                }
            }
        }
        return super.onOptionsItemSelected(item)
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
        initUsersGetTimer()
    }

    override fun onLocationChanged(lat: Double, lng: Double) {
        mainUser.lat = lat
        mainUser.lng = lng
        GlobalScope.launch {
            if (userToken.contains(APP_PREFERENCES_USER_TOKEN)
                && !userToken.getString(APP_PREFERENCES_USER_TOKEN, "").isNullOrEmpty()) {
                apiService.addUser(mainUser, userToken.getString(APP_PREFERENCES_USER_TOKEN, "")!!)
            }
        }
    }

    private fun initUsersGetTimer() {
        getUsersExecutorService = Executors.newFixedThreadPool(1)

        val task = Runnable {
            if (userToken.contains(APP_PREFERENCES_USER_TOKEN)
                && !userToken.getString(APP_PREFERENCES_USER_TOKEN, "").isNullOrEmpty()) {
                val usersResponse = apiService.getUsers(userToken.getString(APP_PREFERENCES_USER_TOKEN, "")!!).execute()
                if (usersResponse.isSuccessful) {
                    val users = usersResponse.body()
                    if (users != null) {
                        runOnUiThread {
                            mMap.clear()
                            users.forEach {
                                val userPosition = LatLng(it.lat, it.lng)
                                val userMarker =
                                    MarkerOptions().position(userPosition).title(it.name)
                                mMap.addMarker(userMarker.position(userPosition))
                            }
                        }
                    }
                }
            }
        }

        getUsersTimer = timer("users-update-timer", true, period = 2000) {
            getUsersExecutorService.submit(task)
        }
    }

    private fun openChangeNamePopUp() {
        val view = layoutInflater.inflate(R.layout.popup_input_dialog, null)

        changeNameEditText = view.findViewById(R.id.change_name_edit_text)
        changeNameAcceptButton = view.findViewById(R.id.change_name_accept_button)

        changeNameAcceptButton.setOnClickListener {
            mainUser.name = changeNameEditText.text.toString()
            userNamePreferences.edit().putString(APP_PREFERENCES_USER_NAME, mainUser.name).apply()
            alertDialog.cancel()
        }

        alertDialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
        if (mainUser.name.isNotBlank()) {
            changeNameEditText.setText(mainUser.name)
        }
        alertDialog.show()
    }

    private fun getToken () {
        GlobalScope.launch {
            if (userToken.contains(APP_PREFERENCES_USER_TOKEN)
                && !userToken.getString(APP_PREFERENCES_USER_TOKEN, "").isNullOrEmpty()) {
                val tokenResponse = apiService.init(mainUser.name).execute()
                if (tokenResponse.isSuccessful) {
                    userToken.edit().putString(APP_PREFERENCES_USER_TOKEN, tokenResponse.body())
                        .apply()
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun changeGpsUpdatesStatus(): Boolean {
        gpsEnableStatus = if (gpsEnableStatus) {
            locationManager.removeUpdates(myLocationListener)
            false
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                5f,
                myLocationListener
            )
            true
        }
        return gpsEnableStatus
    }
}