package com.bignerdbranch.android.photogalleryx

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class GalleryItem(
    var id: String = "",
    var title: String = "",
    @SerializedName("url_s") var url: String = "",
    @SerializedName("owner") var owner: String = ""
) {
    /**
     * This is a computed property to get the Photo page from Flickr.com.
     * * It is the webpage of the full photo
     */
    val photoPageUri: Uri
        get() {
            return Uri.parse("https://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(owner)
                .appendPath(id)
                .build()
        }
}