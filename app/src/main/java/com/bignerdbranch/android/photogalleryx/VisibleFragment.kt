package com.bignerdbranch.android.photogalleryx

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
private const val TAG = "VisibleFragment"
/**
 * This class will be a generic fragment that hides foreground notifications.
 */
abstract class VisibleFragment : Fragment() {
    /**
     * A Dynamic broadcastreceiver created to display the notification intercept the Notification of recently uploaded files if the app is in use.
     */
    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // If we receive this, we're visible, so cancel
            // the notification
            Log.i(TAG, "canceling notification")
            //change the rsult code for the BroadcastReceiver to cancel every BroadcastReceiver related to the Activity
            resultCode = Activity.RESULT_CANCELED
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PollWorker.ACTION_SHOW_NOTIFICATION)
        //register a broadcastreceiver
        // If you want to register and unregister in Fragment.onCreate(…) and Fragment.onDestroy(), use requireActivity().getApplicationContext() instead.)
        requireActivity().registerReceiver(
            onShowNotification,
            filter,
            PollWorker.PERM_PRIVATE,
            null
        )
    }

    override fun onStop() {
        super.onStop()
        //unregister the dynamic broadcast receiver
        requireActivity().unregisterReceiver(onShowNotification)
    }
}
