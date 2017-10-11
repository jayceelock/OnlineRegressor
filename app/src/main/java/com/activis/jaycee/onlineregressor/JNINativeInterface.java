package com.activis.jaycee.onlineregressor;

public class JNINativeInterface
{
    static
    {
        System.loadLibrary("javaInterface");
    }

    public static native boolean init();
    public static native boolean kill();

    public static native void play(float[] src, float[] list, float gain, float pitch);
}
