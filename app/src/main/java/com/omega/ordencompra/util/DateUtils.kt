package com.omega.ordencompra.util

object DateUtils {
    fun formatDateForDisplay(dateStr: String): String {
        return try {
            if (dateStr.contains("-")) {
                val parts = dateStr.split("-") // yyyy-MM-dd
                if (parts.size == 3) {
                    "${parts[2]}/${parts[1]}/${parts[0]}"
                } else dateStr
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
