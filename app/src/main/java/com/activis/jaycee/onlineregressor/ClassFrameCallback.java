package com.activis.jaycee.onlineregressor;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.scene.ASceneFrameCallback;

public class ClassFrameCallback extends ASceneFrameCallback
{
    private static final String TAG = ClassFrameCallback.class.getSimpleName();

    private double cameraPoseTimestamp, rgbTimestampGlThread;

    private final Context activityContext;
    private ActivityMain activityMain;

    public ClassFrameCallback(Context activityContext)
    {
        this.activityContext = activityContext;

        this.activityMain = (ActivityMain)activityContext;
    }

    @Override
    public void onPreFrame(long sceneTime, double deltaTime)
    {
        synchronized (activityContext)
        {
            // Don't execute any tango API actions if we're not connected to the service.
            if (!activityMain.getTangoConnected())
            {
                return;
            }

            // Set-up scene camera projection to match RGB camera intrinsics.
            if (!activityMain.getRenderer().isSceneCameraConfigured())
            {
                TangoCameraIntrinsics intrinsics = TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityMain.getDisplayRotation());
                activityMain.getRenderer().setProjectionMatrix(intrinsics);
            }

            // Connect the camera texture to the OpenGL Texture if necessary
            // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
            // texture with a different ID.
            if (activityMain.getConnectedTextureIdGlThread() != activityMain.getRenderer().getTextureId())
            {
                activityMain.getTango().connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityMain.getRenderer().getTextureId());
                activityMain.setConnectedTextureIdGlThread(activityMain.getRenderer().getTextureId());
            }

            // If there is a new RGB camera frame available, update the texture with it
            if (activityMain.getFrameAvailableTangoThread().compareAndSet(true, false))
            {
                rgbTimestampGlThread = activityMain.getTango().updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
            }

            // If a new RGB frame has been rendered, update the camera pose to match.
            if (rgbTimestampGlThread > cameraPoseTimestamp)
            {
                // Calculate the camera color pose at the camera frame update time in
                // OpenGL engine.
                TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(rgbTimestampGlThread,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        activityMain.getDisplayRotation());
                if (lastFramePose.statusCode == TangoPoseData.POSE_VALID)
                {
                    // Update the camera pose from the renderer
                    activityMain.getRenderer().updateRenderCameraPose(lastFramePose);
                    cameraPoseTimestamp = lastFramePose.timestamp;
                }
                else
                {
                    Log.w(TAG, "Can't get device pose at time: " + rgbTimestampGlThread);
                }
            }
        }
    }

    @Override
    public void onPreDraw(long sceneTime, double deltaTime) { }

    @Override
    public void onPostFrame(long sceneTime, double deltaTime) { }

    @Override
    public boolean callPreFrame()
    {
        return true;
    }
}
