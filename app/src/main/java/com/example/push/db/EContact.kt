package com.example.push.db

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "econtacts")
data class EContact (@PrimaryKey val contactId : Int,
                     @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val contactPic : Bitmap,
                     val contactName : String?,
                     val contactPhone : String?,
                     var priority: Int?)
