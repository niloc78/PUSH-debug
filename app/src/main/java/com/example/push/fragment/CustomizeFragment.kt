package com.example.push.fragment

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.push.R
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class CustomizeFragment : Fragment() {
    lateinit var addressTime: TextView
    lateinit var message: EditText
    lateinit var locationCheckBox: CheckBox
    lateinit var timeCheckBox: CheckBox
    lateinit var editMessageButton: FloatingActionButton
    lateinit var geoCoder: Geocoder
    private val updateTimeIncrement = 30000L
    lateinit var timeHandler: Handler
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locResult: LocationResult) {
            // update addressTime here...
            var lastLocation = locResult.lastLocation
            val addresses = geoCoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
            val completeAddress = addresses[0].getAddressLine(0).split(",")

            addressTime.text = if(completeAddress.size >= 3) {
                val streetName = completeAddress[0]
                val city = completeAddress[1]
                val stateZip = completeAddress[2]
                streetName + "\n" +
                        city + ", " + stateZip + "\n" +
                        "(" + lastLocation.latitude + ", " + lastLocation.longitude + ")" + addressTime.text.substring(addressTime.text.lastIndexOf("\n"))
            } else {
                addresses[0].getAddressLine(0) + "\n" +
                        "(" + lastLocation.latitude + ", " + lastLocation.longitude + ")" + addressTime.text.substring(addressTime.text.lastIndexOf("\n"))
            }
            setFragResult("updatePreviewCard", message.text.toString() + "\n" + addressTime.text)
            Log.d("addressTime", "Updated: " + addressTime.text)

        }
    }

    private fun setFragResult(requestKey : String, bundle : Any) {
        val result = Bundle()
        when(requestKey) {
            "updatePreviewCard" -> {
                result.putString("previewContent", bundle as String)
            }
            "locationEnabled" -> {
                result.putBoolean("locationEnabled", bundle as Boolean)
            }
            "timeEnabled" -> {
                result.putBoolean("timeEnabled", bundle as Boolean)
            }
            "messageChanged" -> {
                result.putString("message", bundle as String)
            }
        }
        parentFragmentManager.setFragmentResult(requestKey, result)
    }
    private val mPermissionResult: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        when (!result.containsValue(false)) {
            true -> {
                Log.d("Location permission", "Granted")
                setFragResult("locationEnabled", true)
                updateLocationPeriodically()
                storeLocationState()
                addressTime.show()
                //startAndBindLocationService()
                //requestLastLocation()
                //startLocationUpdates(createLocationRequest(), getLocationCallback())
            }
            else -> {
                Log.d("Location permission", "Denied")
                setFragResult("locationEnabled", false)
                locationCheckBox.isChecked = false
                storeLocationState()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.customize_frag_layout, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setFragResult("messageChanged", message.text.toString())
        geoCoder = Geocoder(requireContext(), Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        loadState()
    }

    private fun checkPermission() : Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun initViews(v: View) {
        v.apply {
            addressTime = this.findViewById(R.id.address_time)
            message = this.findViewById(R.id.editmessage)
            locationCheckBox = this.findViewById(R.id.location_check_box)
            timeCheckBox = this.findViewById(R.id.time_check_box)
            editMessageButton = this.findViewById(R.id.edit_message_button)
            this.setOnTouchListener { v, event ->
                message.clearFocus()
                message.isEnabled = false
                true
            }
        }
        message.apply {
            setOnFocusChangeListener { v, hasFocus ->
                if(!hasFocus) {
                    setFragResult("messageChanged", message.text.toString())
                    storeMessage()
                }
            }
            isEnabled = false
            setText(retrieveMessage())
        }

        editMessageButton.setOnClickListener {
            message.isEnabled = true
            message.requestFocus()
        }
        addressTime.visibility = View.INVISIBLE

        locationCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    if (!checkPermission()) {
                        mPermissionResult.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    } else {
                        setFragResult("locationEnabled", true)
                        updateLocationPeriodically()
                        addressTime.show()
                        storeLocationState()
                    }
                }
                else -> {
                    setFragResult("locationEnabled", false)
                    //stop periodic location message updates
                    stopLocationPeriodicUpdates()
                    if (!timeBoxIsChecked()) { // if both are unchecked, set message to blank and hide it
                        //set text blank
                        addressTime.text = "\n"
                        addressTime.hide()
                    } else { //if time is checked but location unchecked, remove location from message
                        //remove address
                        addressTime.text = addressTime.text.substring(addressTime.text.lastIndexOf("\n"))
                    }
                    storeLocationState()
                }
            }
        }
        timeCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    setFragResult("timeEnabled", true)
                    updateTimePeriodically()
                    //append time to end
                    addressTime.show()
                    storeTimeState()
                }
                else -> {
                    setFragResult("timeEnabled", false)
                    stopTimePeriodicUpdates()
                    if (!locationBoxIshecked()) { // if both are unchecked, set message to blank and hide it
                        //set text blank
                        addressTime.text = "\n"
                        addressTime.hide()
                    } else { // if location checked and time unchecked, remove time from message
                        //remove time
                        addressTime.text = addressTime.text.substring(0, addressTime.text.lastIndexOf("\n")) + "\n"
                    }
                    storeTimeState()
                }
            }
        }

        //locationHandler = Handler(Looper.getMainLooper())
        timeHandler = Handler(Looper.getMainLooper())

    }

    private fun storeLocationState() {
        val c = requireContext()
        val prefs = c.getSharedPreferences(c.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("locationEnabled", locationBoxIshecked()).apply()
        //prefs.edit().putBoolean("timeEnabled", timeBoxIsChecked()).apply()
        Log.d("locationState",""  + prefs.getBoolean("locationEnabled", false))
//        Log.d("timeState", "" + prefs.getBoolean("timeEnabled", false))
    }
    private fun storeTimeState() {
        val c = requireContext()
        val prefs = c.getSharedPreferences(c.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timeEnabled", timeBoxIsChecked()).apply()
        Log.d("timeState", "" + prefs.getBoolean("timeEnabled", false))
    }

     private fun loadState() {
         val c = requireContext()
         val prefs = c.getSharedPreferences(c.packageName + "_preferences", Context.MODE_PRIVATE)
         if (prefs.getBoolean("locationEnabled", false)) {
             locationCheckBox.performClick()
         }
         if (prefs.getBoolean("timeEnabled", false)) {
             timeCheckBox.performClick()
         }
     }

    private fun storeMessage() {
        val c = requireContext()
        val prefs = c.getSharedPreferences(c.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("message", message.text.toString()).apply()
    }
    private fun retrieveMessage() : String {
        val c = requireContext()
        val prefs = c.getSharedPreferences(c.packageName + "_preferences", Context.MODE_PRIVATE)
        return prefs.getString("message", "SOS! I am in need of urgent help!")!!
    }

    private fun updateTimePeriodically() {
        timeHandler = Handler(Looper.getMainLooper())
        timeHandler.postDelayed(object: Runnable {
            override fun run() {
                val currTime = Calendar.getInstance().time
                val format = SimpleDateFormat("h:mm aa")
                val formattedTime = format.format(currTime).toString()
                addressTime.text = addressTime.text.substring(0, addressTime.text.lastIndexOf("\n")) + "\n" + formattedTime
                try {
                    setFragResult("updatePreviewCard", message.text.toString() + "\n" + addressTime.text)
                } catch (e : IllegalStateException) {
                    e.printStackTrace()
                    stopTimePeriodicUpdates()
                }
                timeHandler.postDelayed(this, updateTimeIncrement)
            }

        }, 1000)

    }

    private fun stopTimePeriodicUpdates() {
        if (timeHandler != null) {
            timeHandler.removeCallbacksAndMessages(null)
        }
    }


    override fun onPause() {
        stopLocationPeriodicUpdates()
        stopTimePeriodicUpdates()
        super.onPause()
        Log.d("onpause", "called")
    }

    private fun updateLocationPeriodically() {
        startLocationUpdates(createLocationRequest(), locationCallback)
    }

    private fun stopLocationPeriodicUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (locationBoxIshecked()) {
            updateLocationPeriodically()
        }
        if (timeBoxIsChecked()) {
            updateTimePeriodically()
        }
        Log.d("onresume", "called")
    }

    private fun startLocationUpdates(locationRequest: LocationRequest, locationCallback: LocationCallback) {
        if (checkPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun createLocationRequest() : LocationRequest {
        return LocationRequest.create().apply {
            interval = 15000
            fastestInterval = 7500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    fun View.hide() {
        this.animate().apply {
            this.duration = 500L
            this.alpha(0.0f)
            this.setListener(object: Animator.AnimatorListener{
                override fun onAnimationRepeat(animation: Animator?) {
                }
                override fun onAnimationEnd(animation: Animator?) {
                    this@hide.visibility = View.INVISIBLE
                    if (this@hide is ViewPager2) {
                        this@hide.currentItem = 0
                    } else if (this@hide is TextView) {
                        when(this@hide.text.toString().toIntOrNull()) {
                            null -> this@hide.text = this@hide.text
                            else -> this@hide.text = "6"
                        }
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }

            })
        }
    }
    fun View.show() {
        this.animate().apply {
            this.duration = 500L
            this.alpha(1.0f)
            this.setListener(object: Animator.AnimatorListener{
                override fun onAnimationRepeat(animation: Animator?) {
                }
                override fun onAnimationEnd(animation: Animator?) {
                    this@show.visibility = View.VISIBLE
                }
                override fun onAnimationCancel(animation: Animator?) {
                }
                override fun onAnimationStart(animation: Animator?) {
                }

            })
        }
    }
    private fun locationBoxIshecked() : Boolean {
        return locationCheckBox.isChecked
    }
    private fun timeBoxIsChecked() : Boolean {
        return timeCheckBox.isChecked
    }

    override fun toString(): String {
        return "CustomizeFragment"
    }

}