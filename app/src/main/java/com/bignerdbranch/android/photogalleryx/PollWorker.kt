package com.bignerdbranch.android.photogalleryx

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

private const val TAG = "PollWorker"
private const val POLL_WORKER_NOTIFICAION_ID: Int = 10

/**
 *This class will be the base class for adding functionality to poll Flickr and determine whether there are new photos the user has not seen yet.
 */
class PollWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        //get the last searched query
        val query = QueryPreferences.getStoredQuery(context)
        //get the last result id
        val lastResultId = QueryPreferences.getLastResultId(context)
        //make a synchronous network request for to return galleryItems from the JSON object
        val items: List<GalleryItem> = if (query.isEmpty()) {
            FlickrFetchr().fetchPhotosRequest()
                .execute()
                .body()
                ?.galleryItems
        } else {
            FlickrFetchr().searchPhotosRequest(query)
                .execute()
                .body()
                ?.galleryItems
        } ?: emptyList()

        //if items isEmpty(): network request returned no list
        if (items.isEmpty()) {
            return Result.success()
        }
        //check to see if the = lastResultId
        val resultId = items.first().id
        if (resultId == lastResultId) {
            Log.i(TAG, "Got an old result: $resultId\n----------------------------")
        } else {
            Log.i(TAG, "Got a new result: $resultId\n------------------------------")
            //change the PREF_LAST_RESULT_ID if a new result came forth
            QueryPreferences.setLastResultId(context, resultId)

            //region prepare the Notification property the user of a new result
            val intent = PhotoGalleryApplication.newIntent(context)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val resources = context.resources
            val notification = NotificationCompat
                .Builder(context, NOTIFICATION_CHANNEL_ID)
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            //endregion

          /*  //region send a notification
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(POLL_WORKER_NOTIFICAION_ID, notification)
            //endregion

            //To send an un-ordered custom Broadcast Event
            context.sendBroadcast(Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE)*/

            showBackgroundNotification(0, notification)
        }
        return Result.success()
    }

    private fun showBackgroundNotification(
        requestCode: Int,
        notification: Notification
    ) {
        val intent = Intent(ACTION_SHOW_NOTIFICATION).apply {
            putExtra(REQUEST_CODE, requestCode)
            putExtra(NOTIFICATION, notification)
        }
        //The result code will be initially set to Activity.RESULT_OK when this ordered broadcast is sent.
        context.sendOrderedBroadcast(intent, PERM_PRIVATE)
    }

    companion object {
        /**
         * The action of the Notification Intent
         */
        const val ACTION_SHOW_NOTIFICATION =
            "com.bignerdranch.android.photogalleryx.SHOW_NOTIFICATION"
        /**
         *The Notification Intent permission
         */
        const val PERM_PRIVATE = "com.bignerdranch.android.photogalleryx.PRIVATE"
        /**
         * requesCode key for an intent putExtra(...)
         */
        const val REQUEST_CODE = "REQUEST_CODE"
        /**
         *  notification object key for an intent putExtra(...)
         */
        const val NOTIFICATION = "NOTIFICATION"
    }

}