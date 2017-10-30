package com.activis.jaycee.onlineregressor;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoPoseData;

public class RunnableSoundGenerator implements Runnable
{
    private static final String TAG = RunnableSoundGenerator.class.getSimpleName();

    private TangoPoseData tangoPose;

    private ActivityMain activityMain;

    RunnableSoundGenerator(Context context)
    {
        activityMain = (ActivityMain) context;
    }

    @Override
    public void run()
    {
        try
        {
            mVector targetPoseVector = new mVector(activityMain.getRenderer().getObjectPosition().x, activityMain.getRenderer().getObjectPosition().y, activityMain.getRenderer().getObjectPosition().z);

            double elevationAngle = ClassHelper.getElevationAngle(targetPoseVector, tangoPose) - Math.PI / 2;
            double xPositionListener = ClassHelper.getXPosition(targetPoseVector, tangoPose);
            double xPositionSource = activityMain.getRenderer().getObjectPosition().x;

            activityMain.getMetrics().updateElevationAngle(elevationAngle);
            activityMain.setAccuracyText(elevationAngle);

            /* Get current target angle and target offset */
            double angle = Math.atan2(targetPoseVector.y, targetPoseVector.x);
            float offset = (float)(Math.abs(elevationAngle / angle));

            float[] tempSrc = new float[3];
            float[] tempList = new float[3];

            for(int i = 0; i < tangoPose.translation.length; i ++)
            {
                tempSrc[i] = (float)(activityMain.getRenderer().getObjectPosition().toArray()[i]);
                tempList[i] = (float)(tangoPose.translation[i]);
            }

            // Get distance to objective, give voice confirm if it's close
            double xDist = activityMain.getRenderer().getObjectPosition().toArray()[0] - tangoPose.translation[0];
            double yDist =  activityMain.getRenderer().getObjectPosition().toArray()[1] - tangoPose.translation[2];
            double zDist = activityMain.getRenderer().getObjectPosition().toArray()[2] - tangoPose.translation[1];

            float distanceToObjective = (float)(Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist));
            float pitch = 0.f;

            if(activityMain.usingAdaptivePitch())
            {
                pitch = activityMain.getInterfaceParameters().getAPitch(elevationAngle);
            }
            else
            {
                pitch = activityMain.getInterfaceParameters().getOPitch(elevationAngle);
            }
            float gain = activityMain.getInterfaceParameters().getGain(distanceToObjective);

            tempSrc[0] = (float)xPositionSource;
            tempList[0] = (float)xPositionListener;

            activityMain.getMetrics().updatePitch(pitch);
            activityMain.getMetrics().updateGain(gain);

            JNINativeInterface.playTarget(tempSrc, tempList, gain, pitch);
            JNINativeInterface.playBand(offset, pitch);
        }

        catch(TangoException e)
        {
            Log.e(TAG, "Error getting the Tango pose: " + e);
        }
    }

    void setTangoPose(TangoPoseData tangoPose)
    {
        this.tangoPose = tangoPose;
        this.run();
    }

}
