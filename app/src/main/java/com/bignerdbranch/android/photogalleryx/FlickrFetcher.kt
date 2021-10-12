package com.bignerdbranch.android.photogalleryx

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bignerdbranch.android.photogalleryx.api.FlickrApi
import com.bignerdbranch.android.photogalleryx.api.PhotoDeserializer
import com.bignerdbranch.android.photogalleryx.api.PhotoInterceptor
import com.bignerdbranch.android.photogalleryx.api.PhotoResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FlickrFetchr"

/**
 * All the Network request codes of PhotoGallery are configured in this class
 */
class FlickrFetchr {

    private var flickrApi: FlickrApi

    /**
     * A property for making FlickrRequest calls. This property is moves outside the [fetchPhotos] method so that
     * the [cancelRequestInFlight] can be actualized
     */
    private lateinit var flickrRequest: Call<PhotoResponse>
    private var gson: Gson
    private var retrofit: Retrofit

    init {
        /* var client = OkHttpClient.Builder().connectTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES).readTimeout(2, TimeUnit.MINUTES).build()*/
        val client = OkHttpClient.Builder()
            .addInterceptor(PhotoInterceptor())
            .build()

        gson = GsonBuilder().registerTypeAdapter(PhotoResponse::class.java, PhotoDeserializer())
            .create()

        retrofit =
            Retrofit.Builder()//.addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .baseUrl("https://www.Flickr.com/").build()


        flickrApi = retrofit.create(FlickrApi::class.java)
    }

    fun fetchPhotos(): LiveData<List<GalleryItem>> {
        return fetchPhotoMetadata(fetchPhotosRequest())
    }

    /**
     * NOTE: This method is also called by [PollWorker] to perform synchronous network request
     */
    fun fetchPhotosRequest(): Call<PhotoResponse> {
        return flickrApi.fetchPhotos()
    }

    fun searchPhotos(query: String): LiveData<List<GalleryItem>> {
        return fetchPhotoMetadata(searchPhotosRequest(query))
    }

    /**
     * NOTE: This method is also called by [PollWorker] to perform synchronous network request
     */
    fun searchPhotosRequest(query: String): Call<PhotoResponse> {
        return flickrApi.searchPhotos(query)
    }

    /**
     * Fetches photos from Flickr.com.
     *
     * Note: This method first returns an empty [List<GalleryItem>] because the network request happens on a background thread.
     * The list is updated when the request is finished.
     *
     * TIP: for this reason, a network request should be connected with a livedata
     */
    fun fetchPhotoMetadata(flickrRequest: Call<PhotoResponse>): LiveData<List<GalleryItem>> {
        val responseLiveData: MutableLiveData<List<GalleryItem>> = MutableLiveData()

        //Note: the .enqueue(...) happens on a background thread
        flickrRequest.enqueue(object : Callback<PhotoResponse> {
            override fun onFailure(call: Call<PhotoResponse>, t: Throwable) {
                Log.e(TAG, "Failed to fetch photos", t)
                //region Toast a message when fetching photofails
                /*Toast.makeText(
                    ,
                    "Error!!! \nFailed to Fetch File...\nCheck your connection.",
                    Toast.LENGTH_LONG
                ).show()*/
                //endregion
            }

            override fun onResponse(call: Call<PhotoResponse>, response: Response<PhotoResponse>) {
                Log.d(TAG, "Response received")
                val photoResponse: PhotoResponse? = response.body()
                var galleryItems: MutableList<GalleryItem> =
                    photoResponse?.galleryItems ?: mutableListOf()
                galleryItems =
                    galleryItems.filterNot { it.url.isBlank() } as MutableList<GalleryItem>
                responseLiveData.value = galleryItems
            }
        })


        return responseLiveData
    }

    /**
     * Fetch Photos from the specified [url]. The [@WorkerThread] annotation indicates that this function should only be called on a background thread.
     */
    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response: Response<ResponseBody> = flickrApi.fetchUrlBytes(url).execute()
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        Log.i(TAG, "Decoded bitmap=$bitmap from Response=$response")
        response.body()?.close()
        return bitmap
    }

    /**
     * This method is used to cancel a FlickrRequest
     */
    fun cancelRequestInFlight() {
        if (::flickrRequest.isInitialized) {
            flickrRequest.cancel()
        }
    }
}