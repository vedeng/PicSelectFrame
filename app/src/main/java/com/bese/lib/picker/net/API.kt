package com.bese.lib.picker.net

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import java.util.HashMap

/**
 * Retrofit调用的API
 */
interface API {

    @Multipart
    @POST( "fileUpload/fileUploadImgForAndroid")
    fun uploadFile(@PartMap request: HashMap<String, String>, @Part filePart: MultipartBody.Part): Call<PicUploadResponse>

}
