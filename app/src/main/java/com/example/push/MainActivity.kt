package com.example.push

import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.push.adapter.FragmentPagerAdapter
import com.example.push.db.EContactDB
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    lateinit var fragPager: ViewPager2
    lateinit var bot_nav_bar: BottomNavigationView
    lateinit var db : EContactDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = EContactDB.invoke(applicationContext)
//            Room.databaseBuilder(applicationContext, EContactDB::class.java, "econtactdb.db").build()

        initPager()
        initBar()
    }

    fun initPager() {
        fragPager = findViewById(R.id.fragment_pager)
        val fragAdapter = FragmentPagerAdapter(this)
        fragPager.apply {
            this.adapter = fragAdapter
            this.isUserInputEnabled = false
        }
    }
    fun initBar() {
        bot_nav_bar = findViewById(R.id.nav_bar)
        bot_nav_bar.apply {
            this.itemIconTintList = null

            this.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.contacts_icon -> {
                        fragPager.currentItem = 0
                        item.isChecked = true
                        true
                        }
                    R.id.message_icon -> {
                        fragPager.currentItem = 1
                        item.isChecked = true
                        true
                    }
                    R.id.alert_icon -> {
                        fragPager.currentItem = 2
                        item.isChecked = true
                        true
                    }
                }
                false
            }
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        //super.onBackPressed()
    }

}


