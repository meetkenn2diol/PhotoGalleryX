package com.bignerdbranch.android.photogalleryx.api

import com.bignerdbranch.android.photogalleryx.GalleryItem
import com.google.gson.*
import java.lang.reflect.Type

private const val TAG = "PhotoDeserializer"

class PhotoDeserializer: JsonDeserializer<PhotoResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PhotoResponse {
        val jsonObject: JsonObject = json?.asJsonObject!!
        val jsonPhotosObject = jsonObject.get("photos").asJsonObject

        val jsonPhotoArray = jsonPhotosObject?.getAsJsonArray("photo")?.asJsonArray

        val photoResponse = PhotoResponse()
        val photos: MutableList<GalleryItem> = mutableListOf()
        //assign the page number from the number of pages in the Flickr.com get query
        photoResponse.page = jsonPhotosObject.get("page").asInt
        //assign the total number of pages that can be displayed from the Flickr.com get query
        photoResponse.pages= jsonPhotosObject.get("pages").asInt
        //flesh out each photo element
        jsonPhotoArray?.forEach { photo ->
            val photoElement = photo.asJsonObject
            val galleryItem = GalleryItem(
                photoElement.get("id").asString,
                photoElement.get("title").asString,
                photoElement.get("url_s").asString,
                photoElement.get("owner").asString
            )
            photos.add(galleryItem)
        }

        photoResponse.galleryItems = photos


        return photoResponse
    }
}