package com.activis.jaycee.onlineregressor;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

public class ClassInterfaceParameters implements Serializable
{
    private static final String TAG = ClassInterfaceParameters.class.getSimpleName();

    private float distHighLimPitch, distLowLimPitch, distHighLimGain, distLowLimGain;
    private float pitchHighLim, pitchLowLim, pitchGradient, pitchIntercept;
    private float gainHighLim, gainLowLim, gainGradient, gainIntercept;
    private float distanceThreshold;

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
    }

    void updatePitchParams(float highLim, float lowLim)
    {
        pitchHighLim = highLim;
        pitchLowLim = lowLim;

        pitchGradient = (pitchLowLim - pitchHighLim) / (distLowLimPitch - distHighLimPitch);
        pitchIntercept = pitchLowLim - pitchGradient * distLowLimPitch;
    }

    void updateGainParams(float highLim, float lowLim)
    {
        gainHighLim = highLim;
        gainLowLim = lowLim;

        gainGradient = (gainLowLim - gainHighLim) / (distLowLimGain - distHighLimGain);
        gainIntercept = gainLowLim - gainGradient * distLowLimGain;
    }

    float getPitch(double elevation)
    {
        float pitch;

        // Compensate for the Tango's default position being 90deg upright
        elevation -= Math.PI / 2;
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
            double gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI));

            float grad = (float)(Math.tan(Math.toRadians(gradientAngle)));
            float intercept = (float)(pitchHighLim - Math.PI / 2 * grad);

            pitch = (float)(Math.pow(2, grad * -elevation + intercept));
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
