package com.example.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    fun formatDate(isoDate: String?): String {
        if (isoDate == null) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(isoDate)
            
            val formatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale("pt", "BR"))
            formatter.timeZone = TimeZone.getDefault()
            date?.let { formatter.format(it) } ?: ""
        } catch (e: Exception) {
            isoDate
        }
    }
}
