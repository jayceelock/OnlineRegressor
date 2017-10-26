package com.activis.jaycee.onlineregressor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.view.SurfaceView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityMain extends AppCompatActivity
{
    private static final String TAG = ActivityMain.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    private AtomicBoolean frameAvailableTangoThread = new AtomicBoolean(false);

    private boolean tangoConnected = false;

    private int connectedTextureIdGlThread = INVALID_TEXTURE_ID;

    private Tango tango;

    private SurfaceView surfaceView;
    private TextView textViewAccuracy, textViewOrigPitch, textViewAdaptedPitch;
    private Switch switchAdaptivePitch;

    private ClassFrameCallback sceneFrameCallback = new ClassFrameCallback(ActivityMain.this);
    private RunnableSoundGenerator runnableSoundGenerator = new RunnableSoundGenerator(ActivityMain.this);
    private ClassInterfaceParameters interfaceParameters;
    private ClassRenderer renderer;
    private ClassHelper helper = new ClassHelper(ActivityMain.this);
    private ClassMetrics metrics;

    private int displayRotation = 0, n = 0, regressionOrder = 1;
    private double errorCum = 0.0, error = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        textViewAccuracy = (TextView) findViewById(R.id.text_accuracy);
        textViewAdaptedPitch = (TextView)findViewById(R.id.text_adapted_pitch);
        textViewOrigPitch = (TextView)findViewById(R.id.text_orig_pitch);
        switchAdaptivePitch = (Switch)findViewById(R.id.switch_use_adaptive_pitch);

        renderer = new ClassRenderer(this);
        metrics = new ClassMetrics(this);

        interfaceParameters = new ClassInterfaceParameters(ActivityMain.this);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null)
        {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener()
            {
                @Override
                public void onDisplayAdded(int displayId) {  }

                @Override
                public void onDisplayChanged(final int displayId)
                {
                    synchronized (this)
                    {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) { }
            }, null);
        }

        renderer.getCurrentScene().registerFrameCallback(sceneFrameCallback);
        surfaceView.setSurfaceRenderer(renderer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        boolean alInit = JNINativeInterface.init();

        if(checkAndRequestPermissions())
        {
            initialiseTango();
        }

        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause()
    {
        boolean alKill = JNINativeInterface.kill();

        if(tangoConnected)
        {
            synchronized (ActivityMain.this)
            {
                try
                {
                    tango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    connectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    tango.disconnect();

                    tangoConnected = false;
                }
                catch(TangoException e)
                {
                    Log.e(TAG, "Tango disconnect error: " + e);
                }
            }
        }

        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();

        switch(action)
        {
            case MotionEvent.ACTION_DOWN:
                this.n += 1;
                this.errorCum += Math.abs(error);
                double[] currentTarget = helper.selectRandomTarget();
                renderer.updateTarget(currentTarget);
                metrics.updateTargetPosition(currentTarget);
                break;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(hasPermissions())
        {
            initialiseTango();
        }
    }

    public void initialiseTango()
    {
        tango = new Tango(ActivityMain.this, new Runnable()
        {
            @Override
            public void run()
            {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (ActivityMain.this)
                {
                    TangoConfig tangoConfig = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_SMOOTH_POSE, true);
                    tangoConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

                    try
                    {
                        ArrayList<TangoCoordinateFramePair> framePairList = new ArrayList<>();
                        framePairList.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));

                        tango.connectListener(framePairList, new ClassTangoUpdateCallback(ActivityMain.this));

                        tango.connect(tangoConfig);
                        tangoConnected = true;

                        TangoSupport.initialize(tango);

                        setDisplayRotation();
                    }
                    catch (TangoOutOfDateException e)
                    {
                        Log.e(TAG, "Tango core out of date, please update: " + e);
                    }

                    catch (TangoErrorException e)
                    {
                        Log.e(TAG, "Could not connect to Tango service: " + e);
                    }
                }
            }
        });
    }

    public boolean checkAndRequestPermissions()
    {
        if(hasPermissions())
        {
            ActivityCompat.requestPermissions(ActivityMain.this, new String[]{Manifest.permission.CAMERA}, 0);

            return false;
        }
        return true;
    }

    public boolean hasPermissions()
    {
        return ContextCompat.checkSelfPermission(ActivityMain.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    public void setDisplayRotation()
    {
        Display display = getWindowManager().getDefaultDisplay();
        displayRotation = display.getRotation();

        /* We also need to update the camera texture UV coordinates. This must be run in the OpenGL thread */
        surfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                if (tangoConnected)
                {
                    renderer.updateColorCameraTextureUvGlThread(displayRotation);
                }
            }
        });
    }

    public void setPitchText(final float oPitch, final float aPitch)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                textViewAdaptedPitch.setText(String.valueOf(aPitch));
                textViewOrigPitch.setText(String.valueOf(oPitch));
            }
        });
    }

    public void setAccuracyText(double error)
    {
        this.error = error;
        final double average = errorCum / n;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                textViewAccuracy.setText(new DecimalFormat("##.##").format(average));
            }
        });
    }

    public void radioButtonClick(View view)
    {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId())
        {
            case R.id.radio_order_1: if(checked)regressionOrder = 1; interfaceParameters.setInitialCoefficients(1); break;
            case R.id.radio_order_2: if(checked)regressionOrder = 2; interfaceParameters.setInitialCoefficients(2); break;
            case R.id.radio_order_3: if(checked)regressionOrder = 3; interfaceParameters.setInitialCoefficients(3); break;
        }
    }

    public Tango getTango(){ return this.tango; }
    public ClassRenderer getRenderer() { return  this.renderer; }
    public boolean getTangoConnected() { return this.tangoConnected; }
    public AtomicBoolean getFrameAvailableTangoThread() { return this.frameAvailableTangoThread; }
    public int getConnectedTextureIdGlThread() { return this.connectedTextureIdGlThread; }
    public void setConnectedTextureIdGlThread(int connectedTextureIdGlThread) { this.connectedTextureIdGlThread = connectedTextureIdGlThread; }
    public SurfaceView getSurfaceView() { return this.surfaceView; }
    public ClassInterfaceParameters getInterfaceParameters() { return this.interfaceParameters; }
    public RunnableSoundGenerator getRunnableSoundGenerator() { return this.runnableSoundGenerator; }
    public int getDisplayRotation() { return this.displayRotation; }
    public ClassMetrics getMetrics() { return this.metrics; }
    public boolean usingAdaptivePitch() { return this.switchAdaptivePitch.isChecked(); }
    public int getRegressionOrder() { return this.regressionOrder; }
}
