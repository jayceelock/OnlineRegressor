package com.activis.jaycee.onlineregressor;

public class JNINativeInterface
{
    static
    {
        System.loadLibrary("javaInterface");
    }

    public static native boolean init();
    public static native boolean kill();

    public static native void playTarget(float[] src, float[] list, float gain, float pitch);
    public static native void playBand(float offset, boolean play, float pitch);
}
