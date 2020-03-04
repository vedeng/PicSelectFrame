package com.bese.lib.picker.net

import com.netlib.BaseRequest
import com.netlib.upload.ProcessCallback
import retrofit2.Call
import java.io.File

/**
 * 上传图片接口
 */
class PicUploadRequest(var file: File?, var processCallback: ProcessCallback?) : BaseRequest<Any, PicUploadResponse>(file?.path) {
    override fun getCall(): Call<PicUploadResponse> {
        val param = HashMap<String, String>()
        param["type"] = "TestUploadFunction"
        return NetTool.getApi().uploadFile(param, getProcessMultiPart(file ?: File("-"), processCallback, file?.name ?: "--"))
    }

    data class Param(var file: File?)
}
