package com.example.push.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.push.R
import com.example.push.db.EContact
import com.mikhaellopez.circularimageview.CircularImageView

class EContactsRecyclerAdapter(var data: ArrayList<EContact>) : RecyclerView.Adapter<EContactsRecyclerAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactPic : CircularImageView = itemView.findViewById(R.id.contact_image)
        val contactName : TextView = itemView.findViewById(R.id.contact_name)
        val contactPhone : TextView = itemView.findViewById(R.id.contact_phone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item_layout, parent, false)
        Log.d("oncreateviewholder", "called")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("onBindViewHolder", "called")
        holder.apply {
            contactPic.setImageBitmap(data[position].contactPic)
            contactName.text = data[position].contactName
            contactPhone.text = data[position].contactPhone

        }
        Log.d("holder applied data", "true")
    }

    override fun getItemCount(): Int {
        return data.size
    }
}