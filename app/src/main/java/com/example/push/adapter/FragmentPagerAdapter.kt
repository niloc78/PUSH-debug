package com.example.push.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.push.fragment.AlertFragment
import com.example.push.fragment.CustomizeFragment
import com.example.push.fragment.EContactsFrag

class FragmentPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return EContactsFrag()
            1 -> return CustomizeFragment()
            2 -> return AlertFragment()
        }
        return AlertFragment()
    }

}