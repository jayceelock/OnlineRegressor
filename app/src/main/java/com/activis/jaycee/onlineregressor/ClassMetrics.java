package com.activis.jaycee.onlineregressor;

import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;

public class ClassMetrics
{
    private static final String TAG = ClassMetrics.class.getSimpleName();

    private ActivityMain activityMain;

    private TangoPoseData poseData;
    private float gain = 0.f;
    private float pitch = 0.f;
    private double elevationAngle = 0.0;
    private double timeStamp;
    private double[] targetPosition = new double[3];

    // Regression summing
    private int n = 0;
    private double sx = 0;
    private double sx2 = 0;
    private double sx3 = 0;
    private double sx4  = 0;
    private double sx5  = 0;
    private double sx6  = 0;

    private double sy = 0;
    private double sxy = 0;
    private double sx2y = 0;
    private double sx3y = 0;

    private double a0 = 0;
    private double a1 = 0;
    private double a2 = 0;
    private double a3 = 0;

    public ClassMetrics(ActivityMain activityMain) { this.activityMain = activityMain;}

    static double log2(float x)
    {
        return (Math.log(x) / Math.log(2));
    }

    public void updateTimestamp(double timestamp)
    {
        this.timeStamp = timestamp;
        this.n += 1;

        double Z = 0;

        int order = activityMain.getRegressor();

        this.sx  +=  (this.elevationAngle);
        this.sx2 +=  Math.pow(this.elevationAngle, 2);
        this.sx3 +=  Math.pow(this.elevationAngle, 3);
        this.sx4 +=  Math.pow(this.elevationAngle, 4);
        this.sx5 +=  Math.pow(this.elevationAngle, 5);
        this.sx6 +=  Math.pow(this.elevationAngle, 6);

        this.sy    += log2(this.pitch);
        this.sxy   += log2(this.pitch) * this.elevationAngle;
        this.sx2y  += log2(this.pitch) * Math.pow(this.elevationAngle, 2);
        this.sx3y  += log2(this.pitch) * Math.pow(this.elevationAngle, 3);

        if(order == 1)
        {
            Z = this.n*this.sx2 - this.sx*this.sx;

            this.a0 = 1/Z * (this.sx2*this.sy - this.sx*this.sxy);
            this.a1 = 1/Z * (n*this.sxy - this.sx*this.sy);
            this.a2 = 0;
            this.a3 = 0;
        }

        else if(order == 2)
        {
            Z = Math.pow(this.sx2, 3) - 2*this.sx*this.sx2*this.sx3 + this.n*Math.pow(this.sx3, 2) + Math.pow(this.sx, 2)*this.sx4 - this.n*this.sx2*this.sx4;

            this.a0 = 1/Z *  (Math.pow(this.sx2, 2)*this.sx2y - this.sx*this.sx2y*this.sx3 - this.sx2*this.sx3*this.sxy + this.sx*this.sx4*this.sxy + Math.pow(this.sx3, 2)*this.sy - this.sx2*this.sx4*this.sy);
            this.a1 = 1/Z * -(this.sx*this.sx2*this.sx2y - this.n*this.sx2y*this.sx3 - Math.pow(this.sx2, 2)*this.sxy + this.n*this.sx4*this.sxy + this.sx2*this.sx3*this.sy - this.sx*this.sx4*this.sy);
            this.a2 = 1/Z *  (Math.pow(this.sx, 2)*this.sx2y - this.n*this.sx2*this.sx2y - this.sx*this.sx2*this.sxy + this.n*this.sx3*this.sxy + Math.pow(this.sx2, 2)*this.sy - this.sx*this.sx3*this.sy);
            this.a3 = 0;
        }

        else if(order == 3)
        {
            Z = Math.pow(sx3, 4) - 3*sx2*Math.pow(sx3, 2)*sx4 + Math.pow(sx2, 2)*Math.pow(sx4, 2) + 2*sx*sx3*Math.pow(sx4, 2)
                    - n*Math.pow(sx4, 3) + 2*Math.pow(sx2, 2)*sx3*sx5 - 2*sx*Math.pow(sx3, 2)*sx5 - 2*sx*sx2*sx4*sx5
                    + 2*n*sx3*sx4*sx5 + Math.pow(sx, 2)*Math.pow(sx5, 2) - n*sx2*Math.pow(sx5, 2) - Math.pow(sx2, 3)*sx6
                    + 2*sx*sx2*sx3*sx6 - n*Math.pow(sx3, 2)*sx6 - Math.pow(sx, 2)*sx4*sx6 + n*sx2*sx4*sx6;

            this.a0 = 1/Z * (Math.pow(sx3, 3)*sx3y - sx2y*Math.pow(sx3, 2)*sx4 - 2*sx2*sx3*sx3y*sx4 + sx2*sx2y*Math.pow(sx4, 2)
                    + sx*sx3y*Math.pow(sx4, 2) + sx2*sx2y*sx3*sx5 + Math.pow(sx2, 2)*sx3y*sx5 - sx*sx3*sx3y*sx5 - sx*sx2y*sx4*sx5
                    - Math.pow(sx2, 2)*sx2y*sx6 + sx*sx2y*sx3*sx6 + sx3*Math.pow(sx4,2)*sxy - Math.pow(sx3, 2)*sx5*sxy - sx2*sx4*sx5*sxy
                    + sx*Math.pow(sx5, 2)*sxy + sx2*sx3*sx6*sxy - sx*sx4*sx6*sxy - Math.pow(sx4, 3)*sy + 2*sx3*sx4*sx5*sy - sx2*Math.pow(sx5, 2)*sy
                    - Math.pow(sx3, 2)*sx6*sy + sx2*sx4*sx6*sy);
            this.a1 = 1/Z * (sx2y*Math.pow(sx3, 3) - sx2*Math.pow(sx3, 2)*sx3y - sx2*sx2y*sx3*sx4 + Math.pow(sx2, 2)*sx3y*sx4
                    + sx*sx3*sx3y*sx4 - n*sx3y*Math.pow(sx4, 2) - sx*sx2y*sx3*sx5 - sx*sx2*sx3y*sx5 + n*sx3*sx3y*sx5 + n*sx2y*sx4*sx5
                    + sx*sx2*sx2y*sx6 - n*sx2y*sx3*sx6 - Math.pow(sx3, 2)*sx4*sxy + 2*sx2*sx3*sx5*sxy - n*Math.pow(sx5, 2)*sxy
                    - Math.pow(sx2, 2)*sx6*sxy + n*sx4*sx6*sxy + sx3*Math.pow(sx4, 2)*sy - Math.pow(sx3, 2)*sx5*sy - sx2*sx4*sx5*sy
                    + sx*Math.pow(sx5, 2)*sy + sx2*sx3*sx6*sy - sx*sx4*sx6*sy);
            this.a2 = 1/Z * -(sx2*sx2y*Math.pow(sx3, 2) - Math.pow(sx2, 2)*sx3*sx3y + sx*Math.pow(sx3, 2)*sx3y - 2*sx*sx2y*sx3*sx4
                    + sx*sx2*sx3y*sx4 - n*sx3*sx3y*sx4 + n*sx2y*Math.pow(sx4, 2) - Math.pow(sx, 2)*sx3y*sx5 + n*sx2*sx3y*sx5
                    + Math.pow(sx, 2)*sx2y*sx6 - n*sx2*sx2y*sx6 - Math.pow(sx3, 3)*sxy + sx2*sx3*sx4*sxy + sx*sx3*sx5*sxy
                    - n*sx4*sx5*sxy - sx*sx2*sx6*sxy + n*sx3*sx6*sxy + Math.pow(sx3, 2)*sx4*sy - sx2*Math.pow(sx4, 2)*sy
                    - sx2*sx3*sx5*sy + sx*sx4*sx5*sy + Math.pow(sx2, 2)*sx6*sy - sx*sx3*sx6*sy);
            this.a3 = 1/Z * (Math.pow(sx2, 2)*sx2y*sx3 - sx*sx2y*Math.pow(sx3, 2) - Math.pow(sx2, 3)*sx3y + 2*sx*sx2*sx3*sx3y
                    - n*Math.pow(sx3, 2)*sx3y - sx*sx2*sx2y*sx4 + n*sx2y*sx3*sx4 - Math.pow(sx, 2)*sx3y*sx4 + n*sx2*sx3y*sx4
                    + Math.pow(sx, 2)*sx2y*sx5 - n*sx2*sx2y*sx5 - sx2*Math.pow(sx3, 2)*sxy + Math.pow(sx2, 2)*sx4*sxy + sx*sx3*sx4*sxy
                    - n*Math.pow(sx4, 2)*sxy - sx*sx2*sx5*sxy + n*sx3*sx5*sxy + Math.pow(sx3, 3)*sy - 2*sx2*sx3*sx4*sy + sx*Math.pow(sx4, 2)*sy
                    + Math.pow(sx2, 2)*sx5*sy - sx*sx3*sx5*sy);
        }

        if(Double.isInfinite(1/Z) || Double.isNaN(1/Z))
        {
            // Log.d(TAG, String.format("Pitch: %f\nElevation: %f\nLog(Pitch): %f\n", this.pitch, this.elevationAngle, log2(this.pitch)));
            Log.d(TAG, "Singularity");
            Log.d(TAG, String.format("Z: %f\nsx: %f\n sx2: %f", Z, this.sx, this.sx2));
        }

        if(Double.isInfinite(this.sx) || Double.isNaN(this.sx))
        {
            Log.d(TAG, String.format("Z: %f\nsx: %f\n sx2: %f", Z, this.sx, this.sx2));
        }

        if(Double.isInfinite(this.sx2) || Double.isNaN(this.sx2))
        {
            Log.d(TAG, String.format("Z: %f\nsx: %f\n sx2: %f", Z, this.sx, this.sx2));
        }
    }

    public void updatePoseData(TangoPoseData pose) { this.poseData = pose; }
    public void updateGain(float gain) { this.gain = gain; }
    public void updatePitch(float pitch) { this.pitch = pitch; }
    public void updateTargetPosition(double[] position){ this.targetPosition = position; }
    public void updateElevationAngle(double elevationAngle){ this.elevationAngle = elevationAngle; }

    public int getN() { return this.n; }
    public double[] getRegressorParams(){ return new double[] {this.a0, this.a1, this.a2, this.a3}; }
}
