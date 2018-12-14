package com.zhouqing.chatproject.realtimeindoorlocation.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.zhouqing.chatproject.realtimeindoorlocation.model.ComparableSensorEvent;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.SensorLoggingAsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SensorRecordService extends Service implements SensorEventListener {

    private static final String TAG = "SensorRecordService";
    private SensorManager mSensorManager;
    boolean isLogging = false;
    private static List<ComparableSensorEvent> sensorEventList = new ArrayList<ComparableSensorEvent>();
    String timeString;//传感器截止记录时间戳

    // angular speeds from gyro
    private float[] gyro = new float[3];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // magnetic field vector
    private float[] magnet = new float[3];
    // accelerometer vector
    private float[] accel = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;


    private int sensorSampleRate = SensorManager.SENSOR_DELAY_GAME;

    private static SensorRecordService instance;//当前类的实例

    private static String SENSOR_RECORD_PATH = Environment.getExternalStorageDirectory() + "/AndroidCamera/Sensor/";

    public static SensorRecordService instance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;//初始化当前类实例
        //初始化传感器管理器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;
    }

    public void startLogging(String timestr) {
        sensorEventList.clear();
        timeString = timestr;
        isLogging = true;
    }

    public void stopLogging() {
        isLogging = false;
        logToFile(timeString);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorSampleRate = SensorManager.SENSOR_DELAY_GAME;
        // 为加速度传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorSampleRate);
        // 为方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), sensorSampleRate);
        // 为陀螺仪传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensorSampleRate);
        // 为磁场传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), sensorSampleRate);
        // 为重力传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), sensorSampleRate);
        // 为线性加速度传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), sensorSampleRate);
        // 为温度传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), sensorSampleRate);
        // 为光传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), sensorSampleRate);
        // 为压力传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), sensorSampleRate);
        //为计步传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), sensorSampleRate);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (isLogging) {
            //将变化的传感器值以及时间戳记录
            long timeStamp = System.currentTimeMillis();
            sensorEventList.add(new ComparableSensorEvent(sensorEvent.values, timeStamp, sensorEvent.sensor.getType()));
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    // copy new accelerometer data into accel array and calculate orientation
                    System.arraycopy(sensorEvent.values, 0, accel, 0, 3);
                    calculateAccMagOrientation(timeStamp);
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    // process gyro data
                    gyroFunction(sensorEvent, timeStamp);
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    // copy new magnetometer data into magnet array
                    System.arraycopy(sensorEvent.values, 0, magnet, 0, 3);
                    break;
            }
        }
    }

    // 通过磁力计读数与加速度读数计算朝向
    public void calculateAccMagOrientation(long timestamp) {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
        //构造结果存入文件
        float[] values = new float[3];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) (accMagOrientation[i] * 180 / Math.PI);
        }
        ComparableSensorEvent comparableSensorEvent = new ComparableSensorEvent(values, timestamp, Constant.TYPE_MAG_ACC_ORI);
        sensorEventList.add(comparableSensorEvent);
    }

    //通过陀螺仪数据计算朝向
    public void gyroFunction(SensorEvent event, long currentTimestamp) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);

        //构造结果存入文件
        float[] values = new float[3];
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) (gyroOrientation[i] * 180 / Math.PI);
        }
        ComparableSensorEvent comparableSensorEvent = new ComparableSensorEvent(gyroOrientation, currentTimestamp, Constant.TYPE_GYRO_ORI);
        sensorEventList.add(comparableSensorEvent);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    private void getRotationVectorFromGyro(float[] gyroValues, float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];
        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float) Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    //写文件
    public static void logToFile(String string) {
        Set<ComparableSensorEvent> eventSet = new LinkedHashSet<ComparableSensorEvent>();
        eventSet.addAll(sensorEventList);
        String machineName = android.os.Build.MODEL.replace(" ", "");
        if (!new File(SENSOR_RECORD_PATH).exists()) {
            new File(SENSOR_RECORD_PATH).mkdirs();
        }
        File outputFile = new File(SENSOR_RECORD_PATH, string + "_sensor_" + machineName + ".txt");
        new SensorLoggingAsyncTask().execute(eventSet, outputFile);
    }


    public StringBuilder stopLoggingAndReturnSensorInfo() {
        isLogging = false;
        return constructSensorInfo();
    }

    public StringBuilder constructSensorInfo() {
        StringBuilder sensorInfoSb = new StringBuilder();
        Set<ComparableSensorEvent> sensorEventSet = new LinkedHashSet<ComparableSensorEvent>();
        sensorEventSet.addAll(sensorEventList);
        for (ComparableSensorEvent event : sensorEventSet) {
            StringBuilder sb = new StringBuilder();
            switch (event.type) {
                case Sensor.TYPE_ACCELEROMETER:
                    sb.append("acc").append(" ");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sb.append("mag").append(" ");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sb.append("gyro").append(" ");
                    break;
                case Sensor.TYPE_ORIENTATION:
                    sb.append("ori").append(" ");
                    break;
                case Sensor.TYPE_GRAVITY:
                    sb.append("grav").append(" ");
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    sb.append("linear_acc").append(" ");
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    sb.append("ambi").append(" ");
                    break;
                case Sensor.TYPE_LIGHT:
                    sb.append("light").append(" ");
                    break;
                case Sensor.TYPE_PRESSURE:
                    sb.append("press").append(" ");
                    break;
                case Sensor.TYPE_STEP_COUNTER:
                    sb.append("step").append(" ");
                    break;
                case Constant.TYPE_GYRO_ORI:
                    sb.append("gyro_ori").append(" ");
                    break;
                case Constant.TYPE_MAG_ACC_ORI:
                    sb.append("mag_acc_ori").append(" ");
                    break;

            }
            sb.append(event.timestamp).append(" ");
            for (int i = 0; i < 3; i++) {
                if (i < event.values.length) {
                    sb.append(event.values[i]).append(" ");
                } else {
                    sb.append(0).append(" ");
                }
            }
            Log.d(TAG, "doInBackground:content->" + sb.toString());
            sensorInfoSb.append(sb.append("\n"));
        }
        return sensorInfoSb;
    }
}