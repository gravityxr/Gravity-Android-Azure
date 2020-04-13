// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.microsoft.sampleandroid

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView

internal object SceneformHelper {
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    // Check to see we have the necessary permissions for this app
    fun hasCameraPermission(activity: Activity?): Boolean {
        return (ContextCompat.checkSelfPermission(activity!!, CAMERA_PERMISSION)
                === PackageManager.PERMISSION_GRANTED)
    }

    fun trySetupSessionForSceneView(
        context: Context?,
        sceneView: ArSceneView
    ): Boolean {
        return try {
            val session = Session(context)
            val config = Config(session)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            session.configure(config)
            sceneView.setupSession(session)
            true
        } catch (e: UnavailableException) {
            Log.e(
                "ASADemo: ",
                "Make sure you have a supported ARCore version installed. Exception: $e"
            )
            false
        }
    }
}