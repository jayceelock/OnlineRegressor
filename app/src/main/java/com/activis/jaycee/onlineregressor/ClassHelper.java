package com.activis.jaycee.onlineregressor;

import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;

import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class ClassHelper
{
    private static final String TAG = ClassHelper.class.getSimpleName();
    private static final int NUM_POINTS_PER_QUAD = 100;

    private ActivityMain activityMain;

    private ArrayList<double[]> targetList = generateRandomTargets();

    private int topLeftCounter = 0;
    private int topRightCounter = 0;
    private int bottomLeftCounter = 0;
    private int bottomRightCounter = 0;

    ClassHelper(ActivityMain activityCamera)
    {
        this.activityMain = activityMain;
    }

    static mQuaternion getQuaternionToTarget(mVector targetPosition, TangoPoseData tangoPoseData)
    {
        mQuaternion tangoOrientation = new mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3]);
        tangoOrientation.normalise();

        mVector tangoPosition = new mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2]);

        mVector tangoForwardVector = new mVector(0, 1, 0);
        mVector tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation);
        tangoForwardFacingVector.normalise();

        // Rotate the vector to allign with global coord system
        mQuaternion rotate = new mQuaternion(new mVector(0, 1, 0), Math.PI);
        rotate.normalise();
        tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate);
        tangoForwardFacingVector.normalise();

        mVector vectorToTarget = new mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y);
        vectorToTarget.normalise();

        mVector rotationAxis = tangoForwardFacingVector.crossProduct(vectorToTarget);
        rotationAxis.normalise();
        double rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget);

        mVector test = rotationAxis.crossProduct(tangoForwardFacingVector);
        if(test.dotProduct(vectorToTarget) > 0)
        {
            rotationAngle = -rotationAngle;
        }

        mQuaternion requiredQuaternion = new mQuaternion(rotationAxis, rotationAngle);
        requiredQuaternion.normalise();

        return requiredQuaternion;
    }

    static double getElevationAngle(mVector targetPosition, TangoPoseData tangoPoseData)
    {
        mQuaternion tangoOrientation = new mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3]);
        tangoOrientation.normalise();

        mVector tangoPosition = new mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2]);

        mVector tangoForwardVector = new mVector(0, 0, 1);
        mVector tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation);
        tangoForwardFacingVector.normalise();

        // Rotate the vector to align with global coord system
        mQuaternion rotate = new mQuaternion(new mVector(0, 1, 0), Math.PI);
        rotate.normalise();
        tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate);
        tangoForwardFacingVector.normalise();

        mVector vectorToTarget = new mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y);
        vectorToTarget.normalise();

        double rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget);

        rotationAngle -= Math.PI;

        return -rotationAngle;
    }

    static double getXPosition(mVector targetPosition, TangoPoseData tangoPoseData)
    {
        mQuaternion tangoOrientation = new mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3]);
        tangoOrientation.normalise();

        mVector tangoPosition = new mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2]);

        mVector tangoForwardVector = new mVector(1, 0, 0);
        mVector tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation);
        tangoForwardFacingVector.normalise();

        // Rotate the vector to align with global coord system
        mQuaternion rotate = new mQuaternion(new mVector(0, 1, 0), Math.PI);
        rotate.normalise();
        tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate);
        tangoForwardFacingVector.normalise();

        mVector vectorToTarget = new mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y);
        vectorToTarget.normalise();

        double rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget);

        double distanceToTarget = vectorToTarget.getLength();

        return distanceToTarget * cos(rotationAngle);
    }

    double[] selectRandomTarget()
    {
        double[] target = new double[3];

        Random randomNumberGenerator = new Random();

        while (topLeftCounter < 7 || topRightCounter < 7 || bottomLeftCounter < 7 || bottomRightCounter < 7)
        {
            // Log.d(TAG, "In loop");
            int quad = randomNumberGenerator.nextInt((4 - 0)) + 0;
            // Log.d(TAG, String.format("Quad number: %d", quad));
            Log.d(TAG, String.format("tLeft tRight bLeft bRight: %d %d %d %d", topLeftCounter, topRightCounter, bottomLeftCounter, bottomRightCounter));

            switch (quad)
            {
                case 0:
                    if (topLeftCounter > 6) // 8 iterations
                    {
                        continue;
                    }
                    topLeftCounter++;
                    Log.d(TAG, String.format("Spawning topLeft, num: %d", topLeftCounter));
                    return targetList.get(randomNumberGenerator.nextInt((quad + 1) * 100 - quad * 100 - 1) + quad * 100);
                case 1:
                    if (topRightCounter > 6)
                    {
                        continue;
                    }
                    topRightCounter++;
                    Log.d(TAG, String.format("Spawning topRight, num: %d", topRightCounter));
                    return targetList.get(randomNumberGenerator.nextInt((quad + 1) * 100 - quad * 100 - 1) + quad * 100);
                case 2:
                    if (bottomLeftCounter > 6)
                    {
                        continue;
                    }
                    bottomLeftCounter++;
                    Log.d(TAG, String.format("Spawning bottomLeft, num: %d", bottomLeftCounter));
                    return targetList.get(randomNumberGenerator.nextInt((quad + 1) * 100 - quad * 100 - 1) + quad * 100);
                case 3:
                    if (bottomRightCounter > 6)
                    {
                        continue;
                    }
                    bottomRightCounter++;
                    Log.d(TAG, String.format("Spawning bottomRight, num: %d", bottomRightCounter));
                    return targetList.get(randomNumberGenerator.nextInt((quad + 1) * 100 - quad * 100 - 1) + quad * 100);
            }
        }

        if (!(topLeftCounter < 7 || topRightCounter < 7 || bottomLeftCounter < 7 || bottomRightCounter < 7))
        {
            activityMain.finish();
        }

        return target;
    }

    ArrayList<double[]> generateRandomTargets()
    {
        ArrayList<double[]> targets = new ArrayList<double[]>();

        /*
        Divide screen into 4 quadrants and populate with equal amounts of random waypoints
         */

        Random randomNumberGenerator = new Random();

        for(int i = 0; i < NUM_POINTS_PER_QUAD*2; i ++)
        {
            double[] target = {0, 0.75*randomNumberGenerator.nextDouble(), -2.0};
            targets.add(target);
        }

        for(int i = 0; i < NUM_POINTS_PER_QUAD*2; i ++)
        {
            double[] target = {0, -0.75 - 0.75*randomNumberGenerator.nextDouble(), -2.0};
            targets.add(target);
        }

        return targets;
    }
}

class mVector
{
    double x, y, z;
    private double length;

    mVector(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;

        this.length = sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    void normalise()
    {
        this.x /= this.length;
        this.y /= this.length;
        this.z /= this.length;
    }

    double dotProduct(mVector v)
    {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    double invDotProduct(mVector v)
    {
        return acos(dotProduct(v));
    }

    mVector crossProduct(mVector v)
    {
        // p x v
        return new mVector(this.y * v.z - this.z * v.y,
                this.z * v.x - this.x * v.z,
                this.x * v.y - this.y * v.x);
    }

    mVector rotateVector(mQuaternion q)
    {
        double x = (1 - 2 * q.y * q.y - 2 * q.z * q.z) * this.x +
                2 * (q.x * q.y + q.w * q.z) * this.y +
                2 * (q.x * q.z - q.w * q.y) * this.z;
        double y = 2 * (q.x * q.y - q.w * q.z) * this.x +
                (1 - 2 * q.x * q.x - 2 * q.z * q.z) * this.y +
                2 * (q.y * q.z + q.w * q.x) * this.z;
        double z = 2 * (q.x * q.z + q.w * q.y) * this.x +
                2 * (q.y * q.z - q.w * q.x) * this.y +
                (1 - 2 * q.x * q.x - 2 * q.y * q.y) * this.z;

        return new mVector(x, y, z);
    }

    double getLength()
    {
        return this.length;
    }
}

class mQuaternion
{
    double x, y, z, w;
    private double length;

    mQuaternion(mVector v, double angle)
    {
        this.x = v.x * sin(angle / 2);
        this.y = v.y * sin(angle / 2);
        this.z = v.z * sin(angle / 2);
        this.w = cos(angle / 2);

        this.length = sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    mQuaternion(double x, double y, double z, double w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        this.length = sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
    }

    void normalise()
    {
        this.x /= this.length;
        this.y /= this.length;
        this.z /= this.length;
        this.w /= this.length;
    }

    public Quaternion getInverse()
    {
        return new Quaternion(-this.x / this.length, this.y / this.length, -this.z / this.length, this.w / this.length);
    }

    public Quaternion multiply(Quaternion q)
    {
        // is p * q (in that order)
        return new Quaternion(this.w * q.x + this.x * q.w - this.y * q.z + this.z * q.y,
                this.w * q.y + this.x * q.z + this.y * q.w - this.z * q.x,
                this.w * q.z - this.x * q.y + this.y * q.x + this.z * q.w,
                this.w * q.w - this.x * q.x - this.y * q.y - this.z * q.z);
    }
}



/**
 * Convenient class for calculating transformations from the Tango world to the OpenGL world,
 * using Rajawali specific classes and conventions.
 */
final class ClassScenePoseCalculator {
    private static final String TAG = ClassScenePoseCalculator.class.getSimpleName();

    /**
     * Transformation from the Tango Area Description or Start of Service coordinate frames
     * to the OpenGL coordinate frame.
     * NOTE: Rajawali uses column-major for matrices.
     */
    public static final Matrix4 OPENGL_T_TANGO_WORLD = new Matrix4(new double[]{
            1, 0,  0, 0,
            0, 0, -1, 0,
            0, 1,  0, 0,
            0, 0,  0, 1
    });

    /**
     *  Transformation from the Tango RGB camera coordinate frame to the OpenGL camera frame.
     */
    public static final Matrix4 COLOR_CAMERA_T_OPENGL_CAMERA = new Matrix4(new double[]{
            1,  0,  0, 0,
            0, -1,  0, 0,
            0,  0, -1, 0,
            0,  0,  0, 1
    });

    /**
     *  Transformation for device rotation on 270 degrees.
     */
    public static final Matrix4 ROTATION_270_T_DEFAULT = new Matrix4(new double[]{
            0, 1, 0, 0,
            -1, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 1
    });

    /**
     *  Transformation for device rotation on 180 degrees.
     */
    public static final Matrix4 ROTATION_180_T_DEFAULT = new Matrix4(new double[]{
            -1,  0, 0, 0,
            0, -1, 0, 0,
            0,  0, 1, 0,
            0,  0, 0, 1
    });

    /**
     *  Transformation for device rotation on 90 degrees.
     */
    public static final Matrix4 ROTATION_90_T_DEFAULT = new Matrix4(new double[]{
            0, -1, 0, 0,
            1,  0, 0, 0,
            0,  0, 1, 0,
            0,  0, 0, 1
    });

    /**
     *  Transformation for device rotation on default orientation.
     */
    public static final Matrix4 ROTATION_0_T_DEFAULT = new Matrix4(new double[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    });

    public static final Matrix4 DEPTH_CAMERA_T_OPENGL_CAMERA = new Matrix4(new double[]{
            1,  0,  0, 0,
            0, -1,  0, 0,
            0,  0, -1, 0,
            0,  0,  0, 1
    });

    /**
     * Up vector in the Tango start of Service and Area Description frame.
     */
    public static final Vector3 TANGO_WORLD_UP = new Vector3(0, 0, 1);

    /**
     * Avoid instantiating the class since it will only be used statically.
     */
    private ClassScenePoseCalculator() {
    }

    /**
     * Converts from TangoPoseData to a Matrix4 for transformations.
     */
    public static Matrix4 tangoPoseToMatrix(TangoPoseData tangoPose) {
        Vector3 v = new Vector3(tangoPose.translation[0],
                tangoPose.translation[1], tangoPose.translation[2]);
        Quaternion q = new Quaternion(tangoPose.rotation[3], tangoPose.rotation[0],
                tangoPose.rotation[1], tangoPose.rotation[2]);
        // NOTE: Rajawali quaternions use a left-hand rotation around the axis convention.
        q.conjugate();
        Matrix4 m = new Matrix4();
        m.setAll(v, new Vector3(1, 1, 1), q);
        return m;
    }

    /**
     * Converts a transform in Matrix4 format to TangoPoseData.
     */
    public static TangoPoseData matrixToTangoPose(Matrix4 transform) {
        // Get translation and rotation components from the transformation matrix.
        Vector3 p = transform.getTranslation();
        Quaternion q = new Quaternion();
        q.fromMatrix(transform);

        TangoPoseData tangoPose = new TangoPoseData();
        double[] t = tangoPose.translation = new double[3];
        t[0] = p.x;
        t[1] = p.y;
        t[2] = p.z;
        double[] r = tangoPose.rotation = new double[4];
        r[0] = q.x;
        r[1] = q.y;
        r[2] = q.z;
        r[3] = q.w;

        return tangoPose;
    }

    /**
     * Helper method to extract a Pose object from a transformation matrix taking into account
     * Rajawali conventions.
     */
    public static Pose matrixToPose(Matrix4 m) {
        // Get translation and rotation components from the transformation matrix.
        Vector3 p = m.getTranslation();
        Quaternion q = new Quaternion();
        q.fromMatrix(m);

        // NOTE: Rajawali quaternions use a left-hand rotation around the axis convention.
        q.conjugate();

        return new Pose(p, q);
    }

    /**
     * Given a pose in start of service or area description frame calculate the corresponding
     * position and orientation for a 3D object in the Rajawali world.
     */
    public static Pose toOpenGLPose(TangoPoseData tangoPose) {
        Matrix4 startServiceTDevice = tangoPoseToMatrix(tangoPose);

        // Get device pose in OpenGL world frame.
        Matrix4 openglWorldTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTDevice);

        return matrixToPose(openglWorldTDevice);
    }

    /**
     * Given a pose in start of service or area description frame and a screen rotaion calculate
     * the corresponding position and orientation for a 3D object in the Rajawali world.
     *
     * @param tangoPose     The input Tango Pose in start service or area description frame.
     * @param rotationIndex The screen rotation index, the index is following Android rotation enum.
     *                      see Android documentation for detail:
     *                      http://developer.android.com/reference/android/view/Surface.html#ROTATION_0 // NO_LINT
     */
    public static Pose toOpenGLPoseWithScreenRotation(TangoPoseData tangoPose, int rotationIndex) {
        Matrix4 startServiceTDevice = tangoPoseToMatrix(tangoPose);

        // Get device pose in OpenGL world frame.
        Matrix4 openglWorldTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTDevice);

        switch (rotationIndex) {
            case 0:
                openglWorldTDevice.multiply(ROTATION_0_T_DEFAULT);
                break;
            case 1:
                openglWorldTDevice.multiply(ROTATION_90_T_DEFAULT);
                break;
            case 2:
                openglWorldTDevice.multiply(ROTATION_180_T_DEFAULT);
                break;
            case 3:
                openglWorldTDevice.multiply(ROTATION_270_T_DEFAULT);
                break;
            default:
                openglWorldTDevice.multiply(ROTATION_0_T_DEFAULT);
                break;
        }

        return matrixToPose(openglWorldTDevice);
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     */
    public static Matrix4 calculateProjectionMatrix(int width, int height, double fx, double fy,
                                                    double cx, double cy) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        double near = 0.1;
        double far = 100;

        double xScale = near / fx;
        double yScale = near / fy;
        double xOffset = (cx - (width / 2.0)) * xScale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        double yOffset = -(cy - (height / 2.0)) * yScale;

        double m[] = new double[16];
        Matrix.frustumM(m, 0,
                xScale * -width / 2.0 - xOffset,
                xScale * width / 2.0 - xOffset,
                yScale * -height / 2.0 - yOffset,
                yScale * height / 2.0 - yOffset,
                near, far);
        return new Matrix4(m);
    }

    /**
     * Given the device pose in start of service frame, calculate the corresponding
     * position and orientation for a OpenGL Scene Camera in the Rajawali world.
     */
    public static Pose toOpenGlCameraPose(TangoPoseData devicePose, DeviceExtrinsics extrinsics) {
        Matrix4 startServiceTdevice = tangoPoseToMatrix(devicePose);

        // Get device pose in OpenGL world frame.
        Matrix4 openglTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTdevice);

        // Get OpenGL camera pose in OpenGL world frame.
        Matrix4 openglWorldTOpenglCamera =
                openglTDevice.multiply(extrinsics.getDeviceTColorCamera()).
                        multiply(COLOR_CAMERA_T_OPENGL_CAMERA);

        return matrixToPose(openglWorldTOpenglCamera);
    }

    /**
     * Given the device pose in start of service frame, calculate the position and orientation of
     * the depth sensor in OpenGL coordinate frame.
     */
    public static Pose toDepthCameraOpenGlPose(TangoPoseData devicePose,
                                               DeviceExtrinsics extrinsics) {
        Matrix4 startServiceTdevice = tangoPoseToMatrix(devicePose);

        // Get device pose in OpenGL world frame.
        Matrix4 openglTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTdevice);

        // Get OpenGL camera pose in OpenGL world frame.
        Matrix4 openglWorldTOpenglCamera =
                openglTDevice.multiply(extrinsics.getDeviceTDepthCamera());

        return matrixToPose(openglWorldTOpenglCamera);
    }

    /**
     * Given a point and a normal in depth camera frame and the device pose in start of service
     * frame at the time the point and normal were acquired, calculate a Pose object which
     * represents the position and orientation of the fitted plane with its Y vector pointing
     * up in the gravity vector, represented in the Tango start of service frame.
     *
     * @param point     Point in depth frame where the plane has been detected.
     * @param normal    Normal of the detected plane.
     * @param tangoPose Device pose with respect to start of service at the time the plane was
     *                  fitted.
     */
    public static TangoPoseData planeFitToTangoWorldPose(
            double[] point, double[] normal, TangoPoseData tangoPose, DeviceExtrinsics extrinsics) {
        Matrix4 startServiceTdevice = tangoPoseToMatrix(tangoPose);

        // Calculate the UP vector in the depth frame at the provided measurement pose.
        Vector3 depthUp = TANGO_WORLD_UP.clone();
        startServiceTdevice.clone().multiply(extrinsics.getDeviceTDepthCamera())
                .inverse().rotateVector(depthUp);

        // Calculate the transform in depth frame corresponding to the plane fitting information.
        Matrix4 depthTplane = matrixFromPointNormalUp(point, normal, depthUp);

        // Convert to OpenGL frame.
        Matrix4 tangoWorldTplane = startServiceTdevice.multiply(extrinsics.getDeviceTDepthCamera()).
                multiply(depthTplane);

        return matrixToTangoPose(tangoWorldTplane);
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will be Z forward, X left, Y up.
     */
    public static Matrix4 matrixFromPointNormalUp(double[] point, double[] normal, Vector3 up) {
        Vector3 zAxis = new Vector3(normal);
        zAxis.normalize();
        Vector3 xAxis = new Vector3();
        xAxis.crossAndSet(up, zAxis);
        xAxis.normalize();
        Vector3 yAxis = new Vector3();
        yAxis.crossAndSet(xAxis, zAxis);
        yAxis.normalize();

        double[] rot = new double[16];

        rot[Matrix4.M00] = xAxis.x;
        rot[Matrix4.M10] = xAxis.y;
        rot[Matrix4.M20] = xAxis.z;

        rot[Matrix4.M01] = yAxis.x;
        rot[Matrix4.M11] = yAxis.y;
        rot[Matrix4.M21] = yAxis.z;

        rot[Matrix4.M02] = zAxis.x;
        rot[Matrix4.M12] = zAxis.y;
        rot[Matrix4.M22] = zAxis.z;

        rot[Matrix4.M33] = 1;

        Matrix4 m = new Matrix4(rot);
        m.setTranslation(point[0], point[1], point[2]);

        return m;
    }

    /**
     * Converts a point, represented as a Vector3 from it's initial refrence frame to
     * the OpenGl world refrence frame. This allows various points to be depicted in
     * the OpenGl rendering.
     */
    public static Vector3 getPointInEngineFrame(
            Vector3 inPoint,
            TangoPoseData deviceTPointFramePose,
            TangoPoseData startServiceTDevicePose) {
        Matrix4 startServiceTDeviceMatrix = tangoPoseToMatrix(startServiceTDevicePose);
        Matrix4 deviceTPointFrameMatrix = tangoPoseToMatrix(deviceTPointFramePose);
        Matrix4 startServiceTDepthMatrix
                = startServiceTDeviceMatrix.multiply(deviceTPointFrameMatrix);

        // Convert the depth point to a Matrix.
        Matrix4 inPointMatrix = new Matrix4();
        inPointMatrix.setToTranslation(inPoint);

        // Transform Point from depth frame to start of service frame to OpenGl world frame.
        Matrix4 startServicePointMatrix = startServiceTDepthMatrix.multiply(inPointMatrix);
        Matrix4 openGlWorldPointMatrix
                = OPENGL_T_TANGO_WORLD.clone().multiply(startServicePointMatrix);
        return matrixToPose(openGlWorldPointMatrix).getPosition();
    }
}

class Pose
{
    private final Quaternion mOrientation;
    private final Vector3 mPosition;

    public Pose(Vector3 position, Quaternion orientation) {
        this.mOrientation = orientation;
        this.mPosition = position;
    }

    public Quaternion getOrientation() {
        return mOrientation;
    }

    public Vector3 getPosition() {
        return mPosition;
    }

    public String toString() {
        return "p:" + mPosition + ",q:" + mOrientation;
    }
}

class DeviceExtrinsics
{
    // Transformation from the position of the depth camera to the device frame.
    private Matrix4 mDeviceTDepthCamera;

    // Transformation from the position of the color Camera to the device frame.
    private Matrix4 mDeviceTColorCamera;

    public DeviceExtrinsics(TangoPoseData imuTDevicePose, TangoPoseData imuTColorCameraPose,
                            TangoPoseData imuTDepthCameraPose) {
        Matrix4 deviceTImu = ClassScenePoseCalculator.tangoPoseToMatrix(imuTDevicePose).inverse();
        Matrix4 imuTColorCamera = ClassScenePoseCalculator.tangoPoseToMatrix(imuTColorCameraPose);
        Matrix4 imuTDepthCamera = ClassScenePoseCalculator.tangoPoseToMatrix(imuTDepthCameraPose);
        mDeviceTDepthCamera = deviceTImu.clone().multiply(imuTDepthCamera);
        mDeviceTColorCamera = deviceTImu.multiply(imuTColorCamera);
    }

    public Matrix4 getDeviceTColorCamera() {
        return mDeviceTColorCamera;
    }

    public Matrix4 getDeviceTDepthCamera() {
        return mDeviceTDepthCamera;
    }
}
