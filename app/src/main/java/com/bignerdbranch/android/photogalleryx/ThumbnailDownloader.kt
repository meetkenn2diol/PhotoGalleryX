package com.bignerdbranch.android.photogalleryx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.concurrent.ConcurrentHashMap


private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0
private const val ERROR_TAG = "Error_TAG"

/**
 * This class receive and process download requests one at a time using Threads, and it will provide the resulting image for each individual request as the corresponding download completes
 *
 * <>
 *
 * Implementing LifecycleObserver means you can register ThumbnailDownloader to receive lifecycle callbacks from any LifecycleOwner e.g [Activity] and [Fragment]
 *
 * <>
 *@param responseHandler: The Handler of the Fragment calling this class
 * @param onThumbnailDownloaded: A function to assign the downloaded bitmap image to the PhotoHolder
 */
class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
) : HandlerThread(TAG) {
    private lateinit var context: Context

    /**
     * property for caching Bitmap images
     */
    private val cache = object : LruCache<String, Bitmap>(150) {
        override fun sizeOf(key: String?, value: Bitmap?): Int {
            return super.sizeOf(key, value)
            //return value?.byteCount
        }
    }
   // private var maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    /**
     * signals when your thread has qui
     */
    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrFetchr = FlickrFetchr()

    /**
     * This [LifecycleObserver] is used to solve the bug on configuration change for the [PhotoGalleryFragment]
     */
    val fragmentLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun setup() {
                Log.i(TAG, "Starting background thread")
                start()
                looper
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(TAG, "Destroying background thread")
                quit()
            }
        }

    /**
     * This [LifecycleObserver] is used to solve the bug on configuration change for the [PhotoGalleryFragment] Views
     */
    val viewLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun clearQueue() {
                Log.i(TAG, "Clearing all requests from queue")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()


    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {

        requestHandler = object : Handler(this.looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    Log.i(TAG, "Got a request for URL: ${requestMap[target!!]}")
                    handleRequest(target)
                }
            }
        }
    }

    /**
     * A method started in order to begin downloading images from Flickr.com
     *
     * @param target: Used as the target for the Looper message
     * @param url: The url from which the image will be downloaded
     * @param context: the context of the calling class. [context] is used to set a bitmap image should there be a failure in the downloading image from the server.
     */
    fun queueThumbnail(target: T, url: String, context: Context) {
        this.context = context
        Log.i(TAG, "Got a URL: $url")
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()

    }

    private fun handleRequest(target: T) {
        val url = requestMap[target!!] ?: return
        var bitmap: Bitmap

        try {
            if (cache.get(url) != null) {
                //set the bitmap image to it's corresponding cached image
                bitmap = cache.get(url)
            } else {
                bitmap = flickrFetchr.fetchPhoto(url) ?: return
                Log.d(TAG, "Image successfully downloaded")
                cache.put(url,bitmap) //cache the downloaded images
            }
        } catch (e: Exception) {
            //if there is an error in downloading the image from the server, set the bitmap to a default bitmap
            Log.d(ERROR_TAG, "Error during Bitmap Download... \nMost likely Network Interruption")
            //region CREATE A BITMAP FROM A VECTOR DRAWABLE
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val drawable = context.getDrawable(R.drawable.ic_baseline_photo_size_select_actual_24)
            drawable?.alpha = 255
            drawable?.setBounds(0, 0, 100, 100)
            drawable?.draw(canvas)
            //endregion
        }
        /*update the bitmap image of the PhotoHolder to the new bitmap.
        Because responseHandler is associated with the main thread’s Looper, all of the code inside of
        Runnable’s run() will be executed on the main thread.
         First, you double-check the requestMap. This is necessary because the
        RecyclerView recycles its views*/
        responseHandler.post(Runnable {
            if (requestMap[target] != url || hasQuit) {
                return@Runnable
            }
            requestMap.remove(target)
            onThumbnailDownloaded(target, bitmap)
        })
    }
}