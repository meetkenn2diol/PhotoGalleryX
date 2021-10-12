package com.bignerdbranch.android.photogalleryx

import android.app.Application
import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

/**
 * Your ViewModel needs a context to use the QueryPreferences functions. Changing the parent class
 * of PhotoGalleryViewModel from ViewModel to AndroidViewModel grants PhotoGalleryViewModel access
 * to the application context. It is safe for PhotoGalleryViewModel to have a reference to the
 * application context, because the app context outlives the PhotoGalleryViewModel.
 */
class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    private val flickrFetchr: FlickrFetchr = FlickrFetchr()
    val galleryItemLiveData: LiveData<List<GalleryItem>>
    private val mutableSearchTerm = MutableLiveData<String>()

    /**
     * For a little bit of polish, pre-populate the search text box with the saved query when the user presses
     *the search icon to expand the search view.
     */
    val searchTerm: String
        get() = mutableSearchTerm.value ?: ""

    init {
        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)
        // Since both the search term and gallery item lists are wrapped in LiveData, you use
        //Transformations.switchMap(trigger: LiveData<X>, transformFunction: Function<X,
        //LiveData<Y>>)
        galleryItemLiveData = Transformations.switchMap(mutableSearchTerm) { searchTerm ->
            if (searchTerm.isBlank()) {
                flickrFetchr.fetchPhotos()
            } else {
                flickrFetchr.searchPhotos(searchTerm)
            }
        }
    }

    fun fetchPhotos(query: String = "") {
        QueryPreferences.setStoredQuery(app, query)
        mutableSearchTerm.value = query
    }

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * It is useful when ViewModel observes some data and you need to clear this subscription to prevent a leak of this ViewModel.
     *
     * Used in this case to cancel flickFetcher dowmloading request
     */
    override fun onCleared() {
        flickrFetchr.cancelRequestInFlight()
        super.onCleared()
    }
}