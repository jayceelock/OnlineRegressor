package com.activis.jaycee.onlineregressor;

import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;

public class ClassMetrics
{
    private static final String TAG = ClassMetrics.class.getSimpleName();

    private TangoPoseData poseData;
    private float gain = 0.f;
    private float pitch = 0.f;
    private double elevationAngle = 0.0;
    private double timeStamp;
    private double[] targetPosition = new double[3];

    // Regression summing
    private int n = 0;
    private double sx = 1;
    private double sx2 = 1;
    private double sx3 = 1;
    private double sx4  = 1;
/*    private double sx5  = 0;
    private double sx6  = 0;*/

    private double sy = 1;
    private double sxy = 1;
    private double sx2y = 0;
    /*private double sx3y = 0;*/

    private double a0 = 1;
    private double a1 = 1;
    private double a2 = 0;
    /*private double a3 = 0;*/

    private int order = 2;

    public ClassMetrics() { }

    static double log2(float x)
    {
        return (Math.log(x) / Math.log(2));
    }

    public void updateTimestamp(double timestamp)
    {
        this.timeStamp = timestamp;
        this.n += 1;

        double Z = 0;

        if(this.order == 2)
        {
            this.sx  +=  (this.elevationAngle);
            this.sx2 +=  Math.pow(this.elevationAngle, 2);

            this.sy   += log2(this.pitch);
            this.sxy  += log2(this.pitch) * this.elevationAngle;

            Z = this.n*this.sx2 - this.sx*this.sx;

            this.a0 = 1/Z * (this.sx2*this.sy - this.sx*this.sxy);
            this.a1 = 1/Z * (n*this.sxy - this.sx*this.sy);
        }

        else if(order == 3)
        {
            this.sx  +=  (this.elevationAngle);
            this.sx2 +=  Math.pow(this.elevationAngle, 2);
            this.sx3 +=  Math.pow(this.elevationAngle, 3);

            this.sy    += log2(this.pitch);
            this.sxy   += log2(this.pitch) * this.elevationAngle;
            this.sx2y  += log2(this.pitch) * Math.pow(this.elevationAngle, 2);

            Z = Math.pow(this.sx2, 3) - 2*this.sx*this.sx2*this.sx3 + this.n*Math.pow(this.sx3, 2) + Math.pow(this.sx, 2)*this.sx4 - this.n*this.sx2*this.sx4;

            this.a0 = 1 / Z * (Math.pow(this.sx2, 2)*this.sx2y - this.sx*this.sx2y*this.sx3 - this.sx2*this.sx3*this.sxy + this.sx*this.sx4*this.sxy + Math.pow(this.sx3, 2)*this.sy - this.sx2*this.sx4*this.sy);
            this.a1 = 1 / Z * -(this.sx*this.sx2*this.sx2y - this.n*this.sx2y*this.sx3 - Math.pow(this.sx2, 2)*this.sxy + this.n*this.sx4*this.sxy + this.sx2*this.sx3*this.sy - this.sx*this.sx4*this.sy);
            this.a2 = 1 / Z * (Math.pow(this.sx, 2)*this.sx2y - this.n*this.sx2*this.sx2y - this.sx*this.sx2*this.sxy + this.n*this.sx3*this.sxy + Math.pow(this.sx2, 2)*this.sy - this.sx*this.sx3*this.sy);
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
    public double[] getRegressorParams(){ return new double[] {this.a0, this.a1, this.a2}; }
    public int getOrder() { return this.order; }
}
