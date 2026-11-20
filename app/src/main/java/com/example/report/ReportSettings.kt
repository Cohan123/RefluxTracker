package com.example.report

import android.content.Context
import android.content.SharedPreferences

/**
 * Local settings for the doctor report.
 *
 * Only the optional display name is stored; it is shown in the PDF header and
 * never leaves the device unless the user shares the generated file.
 */
class ReportSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Trimmed name, or `null` when the user has not entered one. */
    fun userName(): String? = prefs.getString(KEY_USER_NAME, null)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    fun setUserName(name: String?) {
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
    }

    private companion object {
        const val PREFS_NAME = "doctor_report.prefs"
        const val KEY_USER_NAME = "user_name"
    }
}
