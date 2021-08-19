package com.example.push.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class BitmapConverter {

    @TypeConverter
    fun fromBitmapToBlob(bm: Bitmap?) : ByteArray? { // converting to a blob wont retain the original bitmap's id
        val stream = ByteArrayOutputStream()
        bm?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
//        bm?.recycle()
        return byteArray
    }

    @TypeConverter
    fun fromBlobToBitmap(blob: ByteArray) : Bitmap? { // not equal because it makes a new bitmap!
        return BitmapFactory.decodeByteArray(blob, 0, blob.size)
    }
}