package com.bese.lib.picker.net

import com.netlib.APICreator

object NetTool {

    /** 获取网络请求url接口 */
    fun getApi() : API {
        return APICreator.getRetrofit().create(API::class.java)
    }

}