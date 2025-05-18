package com.beknumonov.webviewapp

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("WebViewApp", Context.MODE_PRIVATE)
    private val context: Context? = null


    fun loginFinished(userId: String?) {
        // write all the data entered by the user in SharedPreference and apply
        val myEdit = sharedPreferences.edit()
        myEdit.putString("userId", userId)
        myEdit.putBoolean("isLoggedIn", true)
        myEdit.apply()
    }


    val isLoggedIn: Boolean
        get() = sharedPreferences.getBoolean("isLoggedIn", false)


    val userId: String?
        get() = sharedPreferences.getString("userId", null)


    fun setLogout() {
        val myEdit = sharedPreferences.edit()
        myEdit.remove("userId")
        myEdit.remove("isLoggedIn")
        myEdit.apply()
    }

    companion object {
        fun get(context: Context): PreferencesManager {
            return PreferencesManager(context)
        }
    }
}
