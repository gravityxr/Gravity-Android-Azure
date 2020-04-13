package com.gravityxr.gravity

import android.app.Application
import com.microsoft.CloudServices

class GravityApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Use application's context to initialize CloudServices!

        // Use application's context to initialize CloudServices!
        CloudServices.initialize(this)
    }
}