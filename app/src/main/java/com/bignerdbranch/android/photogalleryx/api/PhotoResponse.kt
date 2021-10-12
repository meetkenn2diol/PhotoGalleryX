package com.bignerdbranch.android.photogalleryx.api

import com.bignerdbranch.android.photogalleryx.GalleryItem
import com.google.gson.annotations.SerializedName

/**
 * A class to map to the "photos" object in the JSON data
 */
class PhotoResponse {
    @SerializedName("page")
    var page: Int = 1

    @SerializedName("pages")
    var pages: Int = 5

    @SerializedName("photo")
    lateinit var galleryItems: MutableList<GalleryItem>
}