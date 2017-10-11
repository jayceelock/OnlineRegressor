package com.activis.jaycee.onlineregressor;

import com.google.atap.tangoservice.TangoPoseData;

public class ClassMetrics
{
    private TangoPoseData poseData;
    private float gain = 0.f;
    private float pitch = 0.f;
    private double elevationAngle;
    private double timeStamp;
    private double[] targetPosition = new double[3];

    // Regression summing
    private double n = 0;
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

    public ClassMetrics() { }

    public void updateTimestamp(double timestamp)
    {
        this.timeStamp = timestamp;

        this.n += 1;

        this.sx +=  this.pitch;
        this.sx2 +=  Math.pow(this.pitch, 2);
        this.sx3 +=  Math.pow(this.pitch, 3);
        this.sx4 +=  Math.pow(this.pitch, 4);
        this.sx5 +=  Math.pow(this.pitch, 5);
        this.sx6 +=  Math.pow(this.pitch, 6);

        this.sy += this.elevationAngle;
        this.sxy += (this.pitch * this.elevationAngle);
        this.sx2y += (Math.pow(this.pitch, 2) * this.elevationAngle);
        this.sx3y += (Math.pow(this.pitch, 3) * this.elevationAngle);

        double Z = Math.pow(this.sx3, 4) - 3*this.sx2*Math.pow(this.sx3, 2)*this.sx4 + Math.pow(this.sx2, 2)*Math.pow(this.sx4, 2) + 2*this.sx*this.sx3*Math.pow(this.sx4, 2)
                - this.n*Math.pow(this.sx4, 3) + 2*Math.pow(this.sx2, 2)*this.sx3*this.sx5 - 2*this.sx*Math.pow(this.sx3, 2)*this.sx5 - 2*this.sx*this.sx2*this.sx4*this.sx5
                + 2*this.n*this.sx3*this.sx4*this.sx5 + Math.pow(this.sx, 2)*Math.pow(this.sx5, 2) - this.n*this.sx2*Math.pow(this.sx5, 2) - Math.pow(this.sx2, 3)*this.sx6
                + 2*this.sx*this.sx2*this.sx3*this.sx6 - this.n*Math.pow(this.sx3, 2)*this.sx6 - Math.pow(this.sx, 2)*this.sx4*this.sx6 + this.n*this.sx2*this.sx4*this.sx6;

        this.a0 = 1 / Z * (Math.pow(this.sx3, 3)*this.sx3y - this.sx2y*Math.pow(this.sx3, 2)*this.sx4 - 2*this.sx2*this.sx3*this.sx3y*this.sx4 + this.sx2*this.sx2y*Math.pow(this.sx4, 2)
                + this.sx*this.sx3y*Math.pow(this.sx4, 2) + this.sx2*this.sx2y*this.sx3*this.sx5 + Math.pow(this.sx2, 2)*this.sx3y*this.sx5 - this.sx*this.sx3*this.sx3y*this.sx5 - this.sx*this.sx2y*this.sx4*this.sx5
                - Math.pow(this.sx2, 2)*this.sx2y*this.sx6 + this.sx*this.sx2y*this.sx3*this.sx6 + this.sx3*Math.pow(this.sx4, 2)*this.sxy - Math.pow(this.sx3, 2)*this.sx5*this.sxy - this.sx2*this.sx4*this.sx5*this.sxy
                + this.sx*Math.pow(this.sx5, 2)*this.sxy + this.sx2*this.sx3*this.sx6*this.sxy - this.sx*this.sx4*this.sx6*this.sxy - Math.pow(this.sx4, 3)*this.sy + 2*this.sx3*this.sx4*this.sx5*this.sy
                - this.sx2*Math.pow(this.sx5, 2)*this.sy - Math.pow(this.sx3, 2)*this.sx6*this.sy + this.sx2*this.sx4*this.sx6*this.sy);
        this.a1 = 1 / Z * (this.sx2y*Math.pow(this.sx3, 3) - this.sx2*Math.pow(this.sx3, 2)*this.sx3y - this.sx2*this.sx2y*this.sx3*this.sx4 + Math.pow(this.sx2, 2)*this.sx3y*this.sx4
                + this.sx*this.sx3*this.sx3y*this.sx4 - this.n*this.sx3y*Math.pow(this.sx4, 2) - this.sx*this.sx2y*this.sx3*this.sx5 - this.sx*this.sx2*this.sx3y*this.sx5
                + this.n*this.sx3*this.sx3y*this.sx5 + this.n*this.sx2y*this.sx4*this.sx5 + this.sx*this.sx2*this.sx2y*this.sx6 - this.n*this.sx2y*this.sx3*this.sx6
                - Math.pow(this.sx3, 2)*this.sx4*this.sxy + 2*this.sx2*this.sx3*this.sx5*this.sxy - this.n*Math.pow(this.sx5, 2)*this.sxy - Math.pow(this.sx2, 2)*this.sx6*this.sxy
                + this.n*this.sx4*this.sx6*this.sxy + this.sx3*Math.pow(this.sx4, 2)*this.sy - Math.pow(this.sx3, 2)*this.sx5*this.sy - this.sx2*this.sx4*this.sx5*this.sy
                + this.sx*Math.pow(this.sx5, 2)*this.sy + this.sx2*this.sx3*this.sx6*this.sy - this.sx*this.sx4*this.sx6*this.sy);
        this.a2 = -1 / Z * (this.sx2*this.sx2y*Math.pow(this.sx3, 2) - Math.pow(this.sx2, 2)*this.sx3*this.sx3y + this.sx*Math.pow(this.sx3, 2)*this.sx3y - 2*this.sx*this.sx2y*this.sx3*this.sx4
                + this.sx*this.sx2*this.sx3y*this.sx4 - this.n*this.sx3*this.sx3y*this.sx4 + this.n*this.sx2y*Math.pow(this.sx4, 2) - Math.pow(this.sx, 2)*this.sx3y*this.sx5 + this.n*this.sx2*this.sx3y*this.sx5
                + Math.pow(this.sx, 2)*this.sx2y*this.sx6 - this.n*this.sx2*this.sx2y*this.sx6 - Math.pow(this.sx3, 3)*this.sxy + this.sx2*this.sx3*this.sx4*this.sxy + this.sx*this.sx3*this.sx5*this.sxy
                - this.n*this.sx4*this.sx5*this.sxy - this.sx*this.sx2*this.sx6*this.sxy + this.n*this.sx3*this.sx6*this.sxy + Math.pow(this.sx3, 2)*this.sx4*this.sy - this.sx2*Math.pow(this.sx4, 2)*this.sy
                - this.sx2*this.sx3*this.sx5*this.sy + this.sx*this.sx4*this.sx5*this.sy + Math.pow(this.sx2, 2)*this.sx6*this.sy - this.sx*this.sx3*this.sx6*this.sy);
        this.a3 = 1 / Z * (Math.pow(this.sx2, 2)*this.sx2y*this.sx3 - this.sx*this.sx2y*Math.pow(this.sx3, 2) - Math.pow(this.sx2, 3)*this.sx3y + 2*this.sx*this.sx2*this.sx3*this.sx3y
                - this.n*Math.pow(this.sx3, 2)*this.sx3y - this.sx*this.sx2*this.sx2y*this.sx4 + this.n*this.sx2y*this.sx3*this.sx4 - Math.pow(this.sx, 2)*this.sx3y*this.sx4 + this.n*this.sx2*this.sx3y*this.sx4
                + Math.pow(this.sx, 2)*this.sx2y*this.sx5 - this.n*this.sx2*this.sx2y*this.sx5 - this.sx2*Math.pow(this.sx3, 2)*this.sxy + Math.pow(this.sx2, 2)*this.sx4*this.sxy + this.sx*this.sx3*this.sx4*this.sxy
                - this.n*Math.pow(this.sx4, 2)*this.sxy - this.sx*this.sx2*this.sx5*this.sxy + this.n*this.sx3*this.sx5*this.sxy + Math.pow(this.sx3, 3)*this.sy - 2*this.sx2*this.sx3*this.sx4*this.sy
                + this.sx*Math.pow(this.sx4, 2)*this.sy + Math.pow(this.sx2, 2)*this.sx5*this.sy - this.sx*this.sx3*this.sx5*this.sy);
    }

    public void updatePoseData(TangoPoseData pose) { this.poseData = pose; }
    public void updateGain(float gain) { this.gain = gain; }
    public void updatePitch(float pitch) { this.pitch = pitch; }
    public void updateTargetPosition(double[] position){ this.targetPosition = position; }
    public void updateElevationAngle(double elevationAngle){ this.elevationAngle = elevationAngle; }
}
