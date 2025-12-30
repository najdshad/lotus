package com.dn0ne.player.app.data.database

import android.net.Uri
import androidx.room.TypeConverter

object UriConverter {
    @TypeConverter
    fun fromUri(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun toUri(uriString: String?): Uri? = uriString?.let { Uri.parse(it) }
}