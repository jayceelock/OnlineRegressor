package com.activis.jaycee.onlineregressor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.Serializable;

public class ClassInterfaceParameters implements Serializable
{
    private static final String TAG = ClassInterfaceParameters.class.getSimpleName();

    private float distHighLimPitch, distLowLimPitch, distHighLimGain, distLowLimGain;
    private float pitchHighLim, pitchLowLim, pitchGradient, pitchIntercept;
    private float gainHighLim, gainLowLim, gainGradient, gainIntercept;
    private float distanceThreshold;
    private float a0, a1, a2, a3;

    private int vibrationDelay, voiceTiming;

    private ActivityMain activityMain;

    ClassInterfaceParameters(Context context)
    {
        String PREF_FILE_NAME = context.getString(R.string.pref_file_name);

        String pitchDistHigh = context.getString(R.string.pref_name_pitch_dist_high);
        String pitchDistLow = context.getString(R.string.pref_name_pitch_dist_low);
        String gainDistHigh = context.getString(R.string.pref_name_gain_dist_high);
        String gainDistLow = context.getString(R.string.pref_name_gain_dist_low);
        String pitchHigh = context.getString(R.string.pref_name_pitch_high);
        String pitchLow = context.getString(R.string.pref_name_pitch_low);
        String gainHigh = context.getString(R.string.pref_name_gain_high);
        String gainLow = context.getString(R.string.pref_name_gain_low);
        String vibration = context.getString(R.string.pref_name_vibration_delay);
        String distanceThreshold = context.getString(R.string.pref_name_distance_threshold);
        String voiceTimer = context.getString(R.string.pref_name_voice_timing);

        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);

        // If one doesn't exist, none do...not good logic I guess, but I'm lazy
        if(!prefs.contains(context.getString(R.string.pref_name_pitch_high)))
        {
            SharedPreferences.Editor editor = prefs.edit();

            // Fields do not exist yet. Save default values to fields
            editor.putFloat(pitchDistHigh, 4.f);
            editor.putFloat(pitchDistLow, -4.f);
            editor.putFloat(gainDistHigh, 6.f);
            editor.putFloat(gainDistLow, 0.f);
            editor.putFloat(pitchHigh, 11.f);
            editor.putFloat(pitchLow, 7.f);
            editor.putFloat(gainHigh, 1.f);
            editor.putFloat(gainLow, 0.5f);
            editor.putInt(vibration, 60);
            editor.putFloat(distanceThreshold, 1.15f);
            editor.putInt(voiceTimer, 5000);

            editor.apply();
        }

        /* Set the only constant: the distance limits */
        this.distHighLimPitch = prefs.getFloat(pitchDistHigh, 4.f);
        this.distLowLimPitch  = prefs.getFloat(pitchDistLow, -4.f);
        this.distHighLimGain  = prefs.getFloat(gainDistHigh, 6.f);
        this.distLowLimGain   = prefs.getFloat(gainDistLow, 0.f);

        /* Initialise the parameters to some defaults */
        this.pitchHighLim = prefs.getFloat(pitchHigh, 11.f);
        this.pitchLowLim = prefs.getFloat(pitchLow, 7.f);
        updatePitchParams(pitchHighLim, pitchLowLim);

        this.gainHighLim = prefs.getFloat(gainHigh, 1.f);
        this.gainLowLim = prefs.getFloat(gainLow, 0.5f);
        updateGainParams(gainHighLim, gainLowLim);

        /* Set Vibration params */
        this.vibrationDelay = prefs.getInt(vibration, 60);

        /* Set obstacle detection alert distance threshold */
        this.distanceThreshold = prefs.getFloat(distanceThreshold, 1.15f);

        this.voiceTiming = prefs.getInt(voiceTimer, 5000);

        activityMain = (ActivityMain)context;

        setInitialCoefficients(activityMain.getRegressionOrder());
    }

    public void setInitialCoefficients(int order)
    {
        double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

        if(order == 1)
        {
            a1 = (float) (Math.tan(Math.toRadians(gradientAngle)));
            a0 = 9.f;// (float) (pitchHighLim - Math.PI / 2 * a1);
            a2 = 0;
            a3 = 0;
        }

        else if(order == 2)
        {
            /* Get initial values from MatLab */
            a0 = 9.f;// 8.9f;
            a1 = -0.8071f;
            a2 = -0.0256f;
            a3 = 0;
        }

        else if(order == 3)
        {
            /* Get initial values from MatLab */
            a0 = 9.f;//8.9f;
            a1 = -0.8846f;
            a2 = -0.0062f;
            a3 = 0.1084f;
        }
    }

    public void updatePitchParams(float highLim, float lowLim)
    {
        pitchHighLim = highLim;
        pitchLowLim = lowLim;

        pitchGradient = (pitchLowLim - pitchHighLim) / (distLowLimPitch - distHighLimPitch);
        pitchIntercept = pitchLowLim - pitchGradient * distLowLimPitch;
    }

    public void updateGainParams(float highLim, float lowLim)
    {
        gainHighLim = highLim;
        gainLowLim = lowLim;

        gainGradient = (gainLowLim - gainHighLim) / (distLowLimGain - distHighLimGain);
        gainIntercept = gainLowLim - gainGradient * distLowLimGain;
    }

    public float getAPitch(double elevation)
    {
        float pitch;

        // Compensate for the Tango's default position being 90deg upright
        if(elevation >= Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, pitchLowLim));
        }

        else if(elevation <= -Math.PI / 2)
        {
            pitch = (float)(Math.pow(2, pitchHighLim));
        }

        else
        {
            if((activityMain.getMetrics().getN() + 1) % 100 == 0)
            {
                double[] regressorParams = activityMain.getMetrics().getRegressorParams();

                a0 = (float)(regressorParams[0]);
                a1 = (float)(regressorParams[1]);
                a2 = (float)(regressorParams[2]);
                a3 = (float)(regressorParams[3]);
                Log.d(TAG, "Updating params");
            }
            // Log.d(TAG, String.format("a0: %f a1: %f a2: %f a3: %f", a0, a1, a2, a3));
            pitch = (float)(Math.pow(2, a0 + elevation*a1 + elevation*elevation*a2 + elevation*elevation*elevation*a3));
            // Log.d(TAG, String.format("Pitch: %f", pitch));

            if(pitch > Math.pow(2, 12))
            {
                pitch = (float)Math.pow(2, 12);
            }
            else if(pitch < Math.pow(2, 6))
            {
                pitch = (float)Math.pow(2, 6);
            }

            if(activityMain.usingAdaptivePitch())
            {
                activityMain.setPitchText(getOPitch(elevation), pitch);
            }
        }

        return pitch;
    }

    public float getOPitch(double elevation)
    {
        // Compensate for the Tango's default position being 90deg upright
        // elevation -= Math.PI / 2;

        double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

        float grad = (float) (Math.tan(Math.toRadians(gradientAngle)));
        float intercept = (float) (pitchHighLim - Math.PI / 2 * grad);

        float pitch = (float)(Math.pow(2, grad * -elevation + intercept));

        if(!activityMain.usingAdaptivePitch())
        {
            activityMain.setPitchText(pitch, getAPitch(elevation));
        }

        return pitch;
    }

    public float getPitch(double srcX, double srcY, double listX, double listY)
    {
        double diffX = (listX - srcX);
        double diffY = (listY - srcY);

        float elevation = (float)(Math.atan2(diffY, diffX));

        if(elevation >= Math.PI / 2)
        {
            return (float)(Math.pow(2, pitchLowLim));
        }

        else if(elevation <= -Math.PI / 2)
        {
            return (float)(Math.pow(2, pitchHighLim));
        }

        else
        {
            double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(pitchHighLim - Math.PI / 2 * grad);

            return (float)(Math.pow(2, grad * -elevation + intercept));
        }
    }

    public float getGain(double src, double list)
    {
        double diffd = (list - src);

        // Use absolute difference, because you might end up behind the marker
        float diff = (float)Math.sqrt(diffd * diffd);

        if(diff >= distHighLimGain)
        {
            return gainHighLim;
        }

        else if(diff <= distLowLimGain)
        {
            return gainLowLim;
        }

        else
        {
            return gainGradient * diff + gainIntercept;
        }
    }

    float getGain(double distance)
    {
        // Use absolute difference, because you might end up behind the marker
        float diff = (float)Math.sqrt(distance * distance);

        if(diff >= distHighLimGain)
        {
            return gainHighLim;
        }

        else if(diff <= distLowLimGain)
        {
            return gainLowLim;
        }

        else
        {
            return gainGradient * diff + gainIntercept;
        }
    }

    public void setVibrationDelay(int vibrationDelay)
    {
        this.vibrationDelay = vibrationDelay;
    }

    public int getVibrationDelay() { return vibrationDelay; }
    public float[] getGainLimits() { return new float[]{gainLowLim, gainHighLim}; }
    public float[] getPitchLimits() { return new float[]{pitchLowLim, pitchHighLim}; }
    public float getDistanceThreshold() { return distanceThreshold; }
    int getVoiceTiming(){ return this.voiceTiming; }
}
