package com.gravityxr.gravity

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.ArFragment
import com.gravityxr.gravity.azure.AnchorVisual
import com.gravityxr.gravity.azure.AzureSpatialAnchorsManager
import com.microsoft.azure.spatialanchors.AnchorLocatedEvent
import com.microsoft.azure.spatialanchors.LocateAnchorsCompletedEvent
import com.microsoft.azure.spatialanchors.SessionUpdatedEvent
import com.microsoft.sampleandroid.SceneformHelper
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap

class GravityExperienceActivity : AppCompatActivity(){

    private val anchorVisuals =
        ConcurrentHashMap<String, AnchorVisual>()
    private var enoughDataForSaving = false

    private lateinit var sceneView: ArSceneView
    private lateinit var arFragment: ArFragment
    private var rootAnchorId: String? = null
    private lateinit var launchMode: MainActivity.LaunchMode
    private var cloudAnchorManager : AzureSpatialAnchorsManager? = null
    private var currentArState : ArState = ArState.Start;

    private val progressLock = String()
    private val renderLock = Any()

    object Companion {
        val KEY_ROOT_ANCHOR_ID = "ROOT_ANCHOR_ID"
        val KEY_LAUNCH_MODE = "LAUNCH_MODE"
    }

    enum class ArState {
        Start,  ///< the start of the session
        CreateLocalAnchor,  ///< the session will create a local anchor
        SaveCloudAnchor,  ///< the session will save the cloud anchor
        SavingCloudAnchor,  ///< the session is in the process of saving the cloud anchor
        DeleteCloudAnchor,  ///< the session will delete the cloud anchor
        DeletingCloudAnchor,  ///< the session is in the process of deleting the cloud anchor
        CreateSessionForQuery,  ///< a session will be created to query for an anchor
        LookForAnchor,  ///< the session will run the query
        LookForNearbyAnchors,  ///< the session will run a query for nearby anchors
        End,  ///< the end of the session
        Restart ///< waiting to restart
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gravity_experience_layout)
        if (intent != null && intent.extras != null) {
            val launchModeString = intent.getStringExtra(Companion.KEY_LAUNCH_MODE);
            if (launchModeString != null) {
                launchMode = MainActivity.LaunchMode.valueOf(launchModeString);
            }
            val anchorId = intent.getStringExtra(Companion.KEY_ROOT_ANCHOR_ID);
            if (anchorId != null) {
                rootAnchorId = anchorId;
            }

            arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment;
            arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
                onTapPlane(
                    hitResult,
                    plane,
                    motionEvent
                )
            }

            sceneView = arFragment.arSceneView as ArSceneView;

            val scene = sceneView.scene
            scene.addOnUpdateListener { frameTime: FrameTime? ->
                if (cloudAnchorManager != null) {
                    // Pass frames to Spatial Anchors for processing.
                    cloudAnchorManager!!.update(sceneView.arFrame)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        destroySession()
    }

    override fun onResume() {
        super.onResume()

        // ArFragment of Sceneform automatically requests the camera permission before creating the AR session,
        // so we don't need to request the camera permission explicitly.
        // This will cause onResume to be called again after the user responds to the permission request.
        if (!SceneformHelper.hasCameraPermission(this)) {
            return
        }
        if (sceneView != null && sceneView.session == null) {
            if (!SceneformHelper.trySetupSessionForSceneView(this, sceneView)) {
                finish()
                return
            }
        }
        if (AzureSpatialAnchorsManager.SpatialAnchorsAccountId == null || AzureSpatialAnchorsManager.SpatialAnchorsAccountId.equals(
                "Set me"
            )
            || AzureSpatialAnchorsManager.SpatialAnchorsAccountKey == null || AzureSpatialAnchorsManager.SpatialAnchorsAccountKey.equals(
                "Set me"
            )
        ) {
            Toast.makeText(
                this,
                "\"Set SpatialAnchorsAccountId and SpatialAnchorsAccountKey in AzureSpatialAnchorsManager.java\"",
                Toast.LENGTH_LONG
            )
                .show()
            finish()
        }
        if (rootAnchorId != null) {
            currentArState = ArState.LookForAnchor;
        }
        proceed()
    }


    private fun startNewRoom() {
        startNewSession()
        runOnUiThread {
//            scanProgressText.setVisibility(View.GONE)
//            statusText.setText("Tap a surface to create an anchor")
//            actionButton.setVisibility(View.INVISIBLE)
        }
        currentArState = ArState.CreateLocalAnchor;
    }

    private fun startNewSession() {
        destroySession()
        cloudAnchorManager = AzureSpatialAnchorsManager(sceneView.session)
        cloudAnchorManager!!.addAnchorLocatedListener { onAnchorLocated(it) }
        cloudAnchorManager!!.addLocateAnchorsCompletedListener {onLocateAnchorsCompleted(it)}
        cloudAnchorManager!!.addSessionUpdatedListener {onSessionUpdated(it)}
        cloudAnchorManager!!.start()
    }

    private fun onLocateAnchorsCompleted(it: LocateAnchorsCompletedEvent?) {

    }

    private fun onSessionUpdated(args: SessionUpdatedEvent?) {


        val progress: Float = args!!.getStatus().getRecommendedForCreateProgress()
        enoughDataForSaving = progress >= 1.0
        synchronized(progressLock) {
            if (currentArState == ArState.SaveCloudAnchor) {
                val decimalFormat = DecimalFormat("00")
                runOnUiThread {
                    val progressMessage = "Scan progress is " + decimalFormat.format(
                        Math.min(
                            1.0f,
                            progress
                        ) * 100.toDouble()
                    ) + "%"
//                    scanProgressText.setText(progressMessage)
                    val anchorVisual = anchorVisuals.get("");
                    anchorVisual?.setLoadingText(progressMessage);
                    anchorVisual?.render(arFragment)
                }
                if (enoughDataForSaving /*&& actionButton.getVisibility() != View.VISIBLE*/) {
                    // Enable the save button
                    runOnUiThread {
//                        statusText.setText("Ready to save")
//                        actionButton.setText("Save cloud anchor")
//                        actionButton.setVisibility(View.VISIBLE)
                        val anchorVisual = anchorVisuals.get("");
                        anchorVisual?.shape = AnchorVisual.Shape.CardPicker;
                        anchorVisual?.render(arFragment)
                    }
                    currentArState = ArState.SaveCloudAnchor
                    proceed();
                }
            }
        }
    }

    private fun onAnchorLocated(it: AnchorLocatedEvent?) {
    }

    private fun proceed() {

        when(currentArState) {
            ArState.Start -> {
                //this starts a new room, callthis when user wants to create a new room
                startNewRoom()
            }
            ArState.LookForAnchor -> {

            }
            ArState.CreateLocalAnchor -> {

            }
            ArState.CreateSessionForQuery -> {

            }
            ArState.LookForNearbyAnchors -> {

            }
            ArState.SavingCloudAnchor -> {

            }
            ArState.SaveCloudAnchor -> {

            }
            ArState.DeletingCloudAnchor -> {

            }
            ArState.DeleteCloudAnchor -> {

            }
            ArState.End -> {

            }
            ArState.Restart -> {

            }
        }

    }


    private fun destroySession() {
        if (cloudAnchorManager != null) {
            cloudAnchorManager!!.stop()
            cloudAnchorManager = null
        }
//        clearVisuals()
    }


    private fun onTapPlane(hitResult: HitResult?, plane: Plane?, motionEvent: MotionEvent?) {
        if (currentArState == ArState.CreateLocalAnchor) {
            createAnchor(hitResult!!)
        }
    }


    private fun createAnchor(hitResult: HitResult): Anchor? {
        val visual = AnchorVisual(arFragment, hitResult.createAnchor())
        visual.setShape(AnchorVisual.Shape.AnchorRegister)
        visual.render(arFragment)
        anchorVisuals.put("", visual)
        runOnUiThread {
//            scanProgressText.setVisibility(View.VISIBLE)
//            if (enoughDataForSaving) {
//                statusText.setText("Ready to save")
//                actionButton.setText("Save cloud anchor")
//                actionButton.setVisibility(View.VISIBLE)
//            } else {
//                statusText.setText("Move around the anchor")
//            }
        }
        currentArState = ArState.SaveCloudAnchor;
        return visual.getLocalAnchor()
    }

}