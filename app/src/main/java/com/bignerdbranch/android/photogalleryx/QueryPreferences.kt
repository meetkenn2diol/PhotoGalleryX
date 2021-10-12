package com.bignerdbranch.android.photogalleryx

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

private const val PREF_SEARCH_QUERY = "searchQuery"

/**
 * key for storing the ID of the most recent photo the user has seen
 */
private const val PREF_LAST_RESULT_ID = "lastResultId"

/**
 * A flag indicating if the worker is enabled.
 */
private const val PREF_IS_POLLING = "isPolling"

/**
 * QueryPreferences is your entire persistence engine for PhotoGallery.
 */
object QueryPreferences {
    fun getStoredQuery(context: Context): String {

        val prefs =
            PreferenceManager.getDefaultSharedPreferences(context)//To get a specific instance of SharedPreferences, you can use the Context.getSharedPreferences(String, Int) function.
        return prefs.getString(PREF_SEARCH_QUERY, "")!!
    }

    fun setStoredQuery(context: Context, query: String) {
        /*PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREF_SEARCH_QUERY, query)
            .apply()*/
        //using 'androidx.core:core-ktx:1.0.0' library
        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            putString(PREF_SEARCH_QUERY, query)
        }
    }

    fun getLastResultId(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_LAST_RESULT_ID, "")!!
    }

    fun setLastResultId(context: Context, lastResultId: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(PREF_LAST_RESULT_ID, lastResultId)
        }
    }

    /**
     * Get the user preference on Polling
     */
    fun isPolling(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_IS_POLLING, false)
    }

    /**
     * Set the user preference on Polling
     */
    fun setPolling(context: Context, isOn: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(PREF_IS_POLLING, isOn)
        }
    }

}