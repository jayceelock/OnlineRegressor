package com.activis.jaycee.onlineregressor;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;

public class ClassTangoUpdateCallback extends Tango.TangoUpdateCallback
{
    private static final String TAG = ClassTangoUpdateCallback.class.getSimpleName();

    private ActivityMain activityMain;

    ClassTangoUpdateCallback(Context context)
    {
        this.activityMain = (ActivityMain)context;
    }

    @Override
    public void onFrameAvailable(int cameraId)
    {
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            if (activityMain.getSurfaceView().getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityMain.getSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
            activityMain.getFrameAvailableTangoThread().set(true);
            activityMain.getSurfaceView().requestRender();
        }
    }

    @Override
    public void onPoseAvailable(TangoPoseData pose)
    {
        activityMain.getRunnableSoundGenerator().setTangoPose(pose);
        activityMain.getMetrics().updatePoseData(pose);
        activityMain.getMetrics().updateTimestamp(pose.timestamp);
        activityMain.getMetrics().updateRegressor();

        activityMain.getMetrics().writeToWifi();
    }
}
