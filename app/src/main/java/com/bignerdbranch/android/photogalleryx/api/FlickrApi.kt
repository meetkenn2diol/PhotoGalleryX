package com.bignerdbranch.android.photogalleryx.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface FlickrApi {
    @GET("services/rest/?method=flickr.interestingness.getList")
    fun fetchPhotos(): Call<PhotoResponse>

    /**
     * Fetch bytes using the [url] in the parameter. By using the [@Url] annotation, OKhttp3 will ignore the base url
     * and solely use the [url] specifies
     */
    @GET
    fun fetchUrlBytes(@Url url: String): Call<ResponseBody>

    @GET("services/rest?method=flickr.photos.search")
    fun searchPhotos(@Query("text") query: String): Call<PhotoResponse>
}