package com.bese.lib.picker

import android.app.Application
import com.netlib.APICreator

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        APICreator.init("http://qa.goapi.vedeng.com/")
    }

}