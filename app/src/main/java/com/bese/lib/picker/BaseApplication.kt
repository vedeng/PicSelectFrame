package com.bese.lib.picker

import android.app.Application
import com.netlib.APICreator

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Retrofit框架初始化 ，传入网络基本域名
        APICreator.init("http://127.0.0.1/")
    }

}