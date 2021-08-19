package com.example.push

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class SMSService : Service() {
    lateinit var fusedLocationClient: FusedLocationProviderClient
    val NOTIFICATION_ID = 46738
    private val binder = LocalBinder()
    lateinit var pendingIntent: PendingIntent
    private var latestLocation: String = ""
    lateinit var geoCoder: Geocoder
    lateinit var locationCallback: LocationCallback
    private var message : String? = ""
    private var locationEnabled = false
    private var timeEnabled = false
    private var eContactNums : Array<String>? = null
    private var smsHandler = Handler(Looper.getMainLooper())
    private val smsTimeIncrement = 30000L
    private var latestTime = ""
    private var currPrio = 0
    private val smsManager = SmsManager.getDefault()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStartCommand", "invoked")
        pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("sms_service_id", "Foreground SMS Service")
        } else {
            ""
        }
        val notification = Notification.Builder(this, channelId).setOngoing(true)
            .setContentTitle("SMS Service")
            .setContentText("Running Foreground SMS Service")
            .setSmallIcon(R.drawable.location_marker)
            .setContentIntent(pendingIntent)
            .setTicker("SMSService")
            .build()
        startForeground(NOTIFICATION_ID, notification)
        setExtras(intent)

        if (locationEnabled) {
            geoCoder = Geocoder(this, Locale.getDefault())
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = makeLocationCallback()
            startLocationUpdates(createLocationRequest(), locationCallback)
        }
        if (numArrayNotEmpty()) {
            runSMSHandler()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    private fun setExtras(intent: Intent?) {
        if (intent != null) {
            message = intent.getStringExtra("message")
            locationEnabled = intent.getBooleanExtra("locationEnabled", false)
            timeEnabled = intent.getBooleanExtra("timeEnabled", false)
            eContactNums = intent.getStringArrayExtra("eContactNums")

        }
    }

    private fun numArrayNotEmpty() : Boolean {
        return eContactNums != null || eContactNums!!.isNotEmpty()
    }

    private fun runSMSHandler() {
        smsHandler = Handler(Looper.getMainLooper())
        smsHandler.postDelayed(object: Runnable {
            override fun run() {
                if (currPrio > eContactNums!!.lastIndex) {
                    currPrio = 0
                }
                var messageToSend = "" + message
                if (locationEnabled) {
                    messageToSend += "\n" + latestLocation
                }
                if (timeEnabled) {
                    val currTime = Calendar.getInstance().time
                    val format = SimpleDateFormat("h:mm aa")
                    latestTime = format.format(currTime).toString()
                    messageToSend += "\n" + latestTime
                }
                if (messageToSend.length > 140) {
                    var bigMessageToSend = "" + message
                    smsManager.sendTextMessage(eContactNums!![currPrio], null, bigMessageToSend, null, null)
                    if (locationEnabled) {
                        bigMessageToSend = latestLocation
                        smsManager.sendTextMessage(eContactNums!![currPrio], null, bigMessageToSend, null, null)
                    }
                    if (timeEnabled) {
                        bigMessageToSend = latestTime
                        smsManager.sendTextMessage(eContactNums!![currPrio], null, bigMessageToSend, null, null)
                    }
                } else {
                    smsManager.sendTextMessage(eContactNums!![currPrio], null, messageToSend, null, null)
                }

                currPrio ++

                //addressTime.text = addressTime.text.substring(0, addressTime.text.lastIndexOf("\n")) + "\n" + formattedTime

                smsHandler.postDelayed(this, smsTimeIncrement)
            }

        }, smsTimeIncrement)
    }

    private fun stopSMSHandler() {
        smsHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        Log.d("Ondestroy service", "called")
        if (locationEnabled) {
            stopLocationUpdates()
        }
        stopSMSHandler()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val b = super.onUnbind(intent)
        Log.d("onUnbind", "Called: " + b)
        return b
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("onBind", "Invoked")
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): SMSService = this@SMSService
    }

    private fun makeLocationCallback() : LocationCallback {
        return object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                var lastLocation = locationResult.lastLocation
                val addresses = geoCoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
                val completeAddress = addresses[0].getAddressLine(0).split(",")
                latestLocation = if(completeAddress.size >= 3) {
                    val streetName = completeAddress[0]
                    val city = completeAddress[1]
                    val stateZip = completeAddress[2]
                    Log.d("Geocoder", "Resolved: " + streetName + "\n" +
                            city + ", " + stateZip + "\n" +
                            "(" + lastLocation.latitude + ", " + lastLocation.longitude + ")")

                    streetName + "\n" +
                            city + ", " + stateZip + "\n" +
                            "(" + lastLocation.latitude + ", " + lastLocation.longitude + ")"

                } else {
                    Log.d("Geocoder", "could not resolve")
                    addresses[0].getAddressLine(0) + "\n" +
                            "(" + lastLocation.latitude + ", " + lastLocation.longitude + ")"
                }
                //val zipCode = addresses[0].postalCode
                Log.d("Location Updates", "LATLNG(" + lastLocation.latitude + ", " + lastLocation.longitude + ")")
            }
        }
    }

    private fun startLocationUpdates(locationRequest: LocationRequest, locationCallback: LocationCallback) {
        if (checkPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

//    private fun requestLastLocation() {
//        if (checkPermission()) {
//            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//                if (location != null) {
//                    Log.d("Location", "LATLNG(" + location.latitude + ", " + location.longitude + ")")
//                } else {
//                    Log.d("Location", "Not Found")
//                }
//            }
//        }
//    }

    private fun createLocationRequest() : LocationRequest {
        return LocationRequest.create().apply {
            interval = 15000
            fastestInterval = 7500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun checkPermission() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

}