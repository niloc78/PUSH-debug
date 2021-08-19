package com.example.push.fragment

import android.Manifest
import android.animation.Animator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.push.*
import com.example.push.adapter.ReassuranceAdapter
import com.example.push.view.ConfirmCancelDialog
import com.example.push.view.RippleBackground
import com.example.push.view.ZoomOutPageTransformer
import com.google.android.material.card.MaterialCardView

class AlertFragment : Fragment(), ConfirmCancelDialog.CancelServiceDialogListener {
    lateinit var alertButton: MaterialCardView;
    lateinit var growingBlueCircle: MaterialCardView
    lateinit var ripple: RippleBackground
    lateinit var sceneRoot: ViewGroup
    lateinit var beforeAlertingScene: Scene
    lateinit var alertingScene: Scene
    lateinit var transitionSet: Transition
    lateinit var reassurancePager: ViewPager2
    lateinit var holdToAlertText: TextView
    lateinit var countDownTextView: TextView
    lateinit var contactsWillBeAlertedText: TextView
    lateinit var secondsText: TextView
    lateinit var messageCard: MaterialCardView
    lateinit var previewMessage: TextView
    private lateinit var smsService: SMSService
    private var mBound: Boolean = false
    private var message : String = ""
    private var locationEnabled = false
    private var timeEnabled = false
    private var handler = Handler(Looper.getMainLooper())
    private var imageHandler = Handler(Looper.getMainLooper())
    private var previewText = ""
    private var countDownHandler = Handler(Looper.getMainLooper())
    private var eContactNums : Array<String>? = null
    private var state : AlertState = AlertState.DEFAULT
    lateinit var cancelButton : TextView
    lateinit var requestSMSPermissionsButton : View

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("Bound", "Success")
            val binder = service as SMSService.LocalBinder
            smsService = binder.getService()
            //updateLocationPeriodically()
            mBound = true
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("Unbound", "Success")
            mBound = false
        }

    }

    private fun checkPermission() : Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

//    private fun isSMSServiceRunning(clazz: Class<Any>) : Boolean {
//        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (clazz.name.equals(service.service.className)) {
//                return true
//            }
//        }
//        return false
//    }

    private val mPermissionResult: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { result ->
        when (result) {
            true -> {
                if (numsArrayIsNotEmpty()) {
                    alertButton.isEnabled = true
                } else {
                    Toast.makeText(context, "You must add at least one emergency contact to enable the SOS button", Toast.LENGTH_LONG).show()
                }
                Log.d("Send SMS permission", "Granted")
                //startAndBindLocationService()
                //requestLastLocation()
                //startLocationUpdates(createLocationRequest(), getLocationCallback())
            }
            else -> {
                alertButton.isEnabled = false
                Log.d("Send SMS permission", "Denied")
//                locationCheckBox.isChecked = false
            }
        }
    }

    private fun checkForSMSPermissionAndLaunchPermissionResult() {
        if (!checkPermission()) {
            mPermissionResult.launch(Manifest.permission.SEND_SMS)
        } else {
            if (numsArrayIsNotEmpty()) {
                alertButton.isEnabled = true
            } else {
                Toast.makeText(context, "You must add at least one emergency contact to enable the SOS button", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopAndUnbindSMSService() {
        //requireContext().unbindService(connection)
        Intent(context, SMSService::class.java).also { intent ->
            context?.stopService(intent)
            context?.unbindService(connection)
            mBound = false
        }
    }

    private fun startAndBindSMSService() {
        Intent(context, SMSService::class.java).also { intent ->
            //intent.put
            intent.putExtra("message", message)
            intent.putExtra("locationEnabled", locationEnabled)
            intent.putExtra("timeEnabled", timeEnabled)
            intent.putExtra("eContactNums", eContactNums)
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            context?.startForegroundService(intent)
        }
    }

    private fun toDefault() {
        stopAndUnbindSMSService()
        TransitionManager.go(beforeAlertingScene, transitionSet)
    }


    private fun initScenes(v: View) {
        sceneRoot = v.findViewById(R.id.scene_root)
        beforeAlertingScene = Scene.getSceneForLayout(sceneRoot, R.layout.alert_frag_layout, context)
        alertingScene = Scene.getSceneForLayout(sceneRoot, R.layout.alerting_scene, context)
        transitionSet = TransitionInflater.from(context).inflateTransition(R.transition.animate)
        transitionSet.duration = 500L
        transitionSet.addListener(object: Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
                when(state) {
                    AlertState.DEFAULT -> {
                        initRipple(v)
                        startAndBindSMSService()
                        cancelButton = v.findViewById(R.id.cancel_service_button)
                        cancelButton.setOnClickListener {
                            showConfirmCancelDialog()
                        }
                        state = AlertState.ALERTING
                    }
                    AlertState.ALERTING -> {
                        initViews(v)
                        state = AlertState.DEFAULT
                    }
                }
            }

            override fun onTransitionResume(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionCancel(transition: Transition?) {}
            override fun onTransitionStart(transition: Transition?) {}
        })
    }

    private fun View.setOnVeryLongClickListener(listener: () -> Unit) {
        setOnTouchListener(object : View.OnTouchListener {
            private val longClickDuration = 6000L
            private var countDownIncrement = 1000L
            private var timeIncrement = 2000L
            private val animation : Animation = AnimationUtils.loadAnimation(context,
                R.anim.grow_circle
            )

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    //show reassurance images and hide card
                    fadeInPagerOutCard()
                    showCountDownTexts()
                    //page through every reassurance pager images every x seconds
                    imageHandler.postDelayed(object: Runnable{
                        override fun run() {
                            reassurancePager.currentItem = reassurancePager.currentItem + 1
                            imageHandler.postDelayed(this, timeIncrement)
                        }

                    }, timeIncrement)

                    countDownHandler.postDelayed(object: Runnable{
                        override fun run() {
                            countDownTextView.text = (countDownTextView.text.toString().toInt() - 1).toString()
                            countDownHandler.postDelayed(this, countDownIncrement)
                        }

                    }, countDownIncrement)

                    //do buffer/growing animation here
                    growingBlueCircle.startAnimation(animation)
                    growingBlueCircle.visibility = View.VISIBLE

                    handler.postDelayed({listener.invoke()}, longClickDuration)
                } else if (event?.action == MotionEvent.ACTION_UP) {
                    //cancel delayed runnable
                    handler.removeCallbacksAndMessages(null)
                    imageHandler.removeCallbacksAndMessages(null)
                    countDownHandler.removeCallbacksAndMessages(null)
                    //clear animations and set reassurance images invisible in card
                    hideCountDownTexts()
                    growingBlueCircle.clearAnimation()
                    fadeInCardOutPager()
                    growingBlueCircle.visibility = View.INVISIBLE
                }
                return true
            }


        })
    }

    private fun toAlerting() {
        TransitionManager.go(alertingScene, transitionSet)
    }
    private fun initViews(v: View) {
        growingBlueCircle = v.findViewById(R.id.growing_blue_circle)
        alertButton = v.findViewById(R.id.alert_button)
        alertButton.setOnVeryLongClickListener {
            handler.removeCallbacksAndMessages(null)
            imageHandler.removeCallbacksAndMessages(null)
            countDownHandler.removeCallbacksAndMessages(null)
            val vibrate : Vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrate.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            toAlerting()
        }
//        alertButton.isEnabled = numsArrayIsNotEmpty() && checkPermission()
//        alertButton.isLongClickable = false
        val reassuranceAdapter = ReassuranceAdapter(context, intArrayOf(R.drawable.take_a_deep_breath, R.drawable.try_to_stay_calm, R.drawable.help_is_coming))
        reassurancePager = v.findViewById(R.id.reassurance_pager)
        reassurancePager.apply {
            this.adapter = reassuranceAdapter
            this.setPageTransformer(ZoomOutPageTransformer())
        }
        reassurancePager.visibility = View.INVISIBLE
        messageCard = v.findViewById(R.id.message_card)
        countDownTextView = v.findViewById(R.id.count)
        contactsWillBeAlertedText = v.findViewById(R.id.contacts_will_be_alerted)
        secondsText = v.findViewById(R.id.seconds)
        holdToAlertText = v.findViewById(R.id.hold_to_alert_contacts)
        previewMessage = v.findViewById(R.id.preview_message)
        previewMessage.text = previewText
        requestSMSPermissionsButton = v.findViewById(R.id.request_sms_permission_button)
        requestSMSPermissionsButton.setOnClickListener {
            checkForSMSPermissionAndLaunchPermissionResult()
        }

    }

    private fun showConfirmCancelDialog() {
        val dialog = ConfirmCancelDialog()
        dialog.show(childFragmentManager, "confirm cancel dialog")
    }


    fun numsArrayIsNotEmpty() : Boolean {
        return eContactNums != null && eContactNums!!.isNotEmpty()
    }

    fun showCountDownTexts() {
        holdToAlertText.hide()
        contactsWillBeAlertedText.show()
        countDownTextView.show()
        secondsText.show()
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

    fun hideCountDownTexts() {
        holdToAlertText.show()
        contactsWillBeAlertedText.hide()
        countDownTextView.hide()
        secondsText.hide()
    }

    fun initRipple(v: View) {
        ripple = v.findViewById(R.id.ripple)
        ripple.startRippleAnimation()
    }

    fun fadeInPagerOutCard() {
        messageCard.hide()
        reassurancePager.show()

    }

    fun fadeInCardOutPager() {
        messageCard.show()
        reassurancePager.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.alert_frag_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initScenes(view)

        parentFragmentManager.apply {
            this.setFragmentResultListener("updatePreviewCard", this@AlertFragment,
                { requestKey, result ->
                    previewText = result.getString("previewContent")!!
                    previewMessage.text = result.getString("previewContent")
                    Log.d("received Result", "updatePreviewCard")
                })
            this.setFragmentResultListener("locationEnabled", this@AlertFragment,
                { requestKey, result ->
                    locationEnabled = result.getBoolean("locationEnabled")
                    Log.d("received Result", "locationEnabled: $locationEnabled")
                })
            this.setFragmentResultListener("timeEnabled", this@AlertFragment,
                { requestKey, result ->
                    timeEnabled = result.getBoolean("timeEnabled")
                    Log.d("received Result", "timeEnabled: $timeEnabled")
                })
            this.setFragmentResultListener("eContactsChanged", this@AlertFragment,
                { requestKey, result ->
                    eContactNums = result.getStringArray("eContactNums")
                    Log.d("received Result", "eContactNums: $result")
                    alertButton.isEnabled = numsArrayIsNotEmpty() && checkPermission()
                })
            this.setFragmentResultListener("messageChanged", this@AlertFragment,
                { requestKey, result ->
                    message = result.getString("message")!!
                    previewText = message
                    if (previewMessage.text.indexOf("\n") >= 0) { // address or time exists in preview
                        previewMessage.text = message + previewMessage.text.substring(previewMessage.text.indexOf("\n"))
                    } else {
                        previewMessage.text = message
                    }
                    Log.d("received Result", "message: $message")
                })
            val result = Bundle()
            result.putBoolean("AlertFragmentCreated", true)
            this.setFragmentResult("AlertFragmentCreated", result)
        }
        checkForSMSPermissionAndLaunchPermissionResult()
        Log.d("alertbutton enabled view created: ", "${alertButton.isEnabled}")


    }

    override fun onDetach() {
        super.onDetach()
        if (mBound) {
            context?.unbindService(connection)
        }
    }

    override fun confirmCancel() {
        //to default
        toDefault()
    }

    override fun toString(): String {
        return "AlertFragment"
    }


}