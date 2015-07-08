package com.example.cdac.servicetouploaddata;

/**
 * Created by cdac on 23/6/15.
 */
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

public class StepService extends Service implements SensorEventListener {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();

    private final List<DataChangeListener> Listeners = new ArrayList<>();

    averaging_filter averaging_filter_x =new averaging_filter();
    averaging_filter averaging_filter_y =new averaging_filter();
    averaging_filter averaging_filter_z =new averaging_filter();
    step_detection step_detection_info=new step_detection();
    step_length_estimation step_length_estimation_info=new step_length_estimation();

    public SensorManager mSensorManager = null;

    int step_avg_filter_size=40;
    float step_z_acc_previous=0,step_z_acc_present=0,step_x_acc_previous=0,step_x_acc_present=0,step_y_acc_previous=0,step_y_acc_present=0;

    //----------------------to store sensor values-----------------------------
    float[] linear= new float[4];
    float[] linear2= new float[4];
    float[] accelero = new float[4];
    float[] gyro = new float[3];
    float[] magneto = new float[4];
    float[] magneto2 = new float[4];
    float[] gravity = new float[3];
    float[] gravity2 = new float[4];
    float[] rotation = new float[6];
    private float[] realAccValues = new float[4];
    public  float[] rotationMatrix = new float[16];
    public  float[] invertedRotationMatrix = new float[16];
    public  float[] inclinationMatrix = new float[16];
    float ang=0;

    int lagger = 0;
    //------------------------step detector-----------------------
    float step_detector_count=0;
    float[] step_values_before_step_detected={0,0,0,0,0};
    float[] step_values_after_step_detected={0,0,0,0,0};

    //----------------for step_lengths---------------
    float step_l1=0;
    float step_length=0;

    private float angle = 0f;
    private boolean prevStepDetected = false;
    private boolean currentStepDetected = false;
    private int step_count = 0;
    private int bufferStepCount = 0;
    private int maxDataCount =30;
    private String[] dataBuffer = new String[maxDataCount];
    private int counter = 1;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");


        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                gravity = event.values;
                gravity2[0] = event.values[0];
                gravity2[1] = event.values[1];
                gravity2[2] = event.values[2];
                gravity2[3] = 0;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyro = event.values;
                break;
            case Sensor.TYPE_ORIENTATION:
                ang = event.values[0];
                //   my_layout.setRotation(-ang);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneto = event.values;
                magneto2[0] = event.values[0];
                magneto2[1] = event.values[1];
                magneto2[2] = event.values[2];
                magneto2[3] = 0;

                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                rotation[0] = event.values[0];
                rotation[1] = event.values[1];
                rotation[2] = event.values[2];
                rotation[3] = event.values[3];
                break;

            case Sensor.TYPE_ACCELEROMETER:
                accelero = event.values;
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                linear2[0] = event.values[0];
                linear2[1] = event.values[1];
                linear2[2] = event.values[2];
                linear2[3] = 0;
                linear = event.values;
        }
            localization();                 // localization_using_inertial _sensor_data
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implemented. Goes unused.
    }

    private void localization()
    {
        convert_coordinate();// converting phone coordinate system to real world system
        averaging();//filtering the accelerometer sensor data
        //applying step detection algorithm
        step_detection_info.main_step_detector(step_z_acc_present, step_z_acc_previous, ang, step_values_before_step_detected, step_values_after_step_detected);
        // checking whether step is detected
        if(step_detection_info.step_detected==1) {
            //  step length estimation
            prevStepDetected = currentStepDetected;
            currentStepDetected = true;

            step_detector_count++;
            step_l1 = step_length_estimation_info.step_length(step_values_after_step_detected);

            setAllData(step_count+1, step_l1, step_detection_info.step_ang, realAccValues[0], realAccValues[1], realAccValues[2], linear2[0], linear2[1], linear2[2], step_detection.step_min1, step_detection.step_min2, step_detection.step_max1);

            step_detection_info.step_detected = 0;
        }

        else{

            setAllData(0, 0, ang, realAccValues[0], realAccValues[1], realAccValues[2], linear2[0], linear2[1], linear2[2], 0, 0, 0);
            /*if(lagger == 10) {
                setAllData(0, 0, ang, realAccValues[0], realAccValues[1], realAccValues[2], linear2[0], linear2[1], linear2[2], 0, 0, 0);
                lagger = 0;

            }
            else{
                lagger++;
            }*/
        }
     }


    private void convert_coordinate() {
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magneto);
        android.opengl.Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        android.opengl.Matrix.multiplyMV(realAccValues, 0, invertedRotationMatrix, 0, linear2, 0);

    }

    private void averaging()
    {
        step_z_acc_previous=step_z_acc_present;
        step_x_acc_previous=step_x_acc_present;
        step_y_acc_previous=step_y_acc_present;

        step_x_acc_present   = averaging_filter_x.average(realAccValues[0],step_x_acc_present,  step_avg_filter_size);
        step_y_acc_present   = averaging_filter_y.average(realAccValues[1],step_y_acc_present,  step_avg_filter_size);
        step_z_acc_present   = averaging_filter_z.average(realAccValues[2],step_z_acc_present,step_avg_filter_size);

    }

    public void initListeners(){

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);


        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);


    }



    public void postData(String[] dataToSend) {
        AsyncDataTransfer transfer = new AsyncDataTransfer();
        String data = "";
        try {
            for(int i = 0; i < dataToSend.length; i++) {
                data += "data"+i+"#"+ dataToSend[i];
                if(i != dataToSend.length-1){
                    data += ";";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception: ", "can't put request");
            return;
        }
        transfer.execute(data);
/*
        HashMap<String, String> data = new HashMap<String, String>();
        for(int i = 0;i < dataToSend.length; i++){
            data.put("stepdata" + i, dataToSend[i]);
        }
        AsyncHttpPost asyncHttpPost = new AsyncHttpPost(data);
        asyncHttpPost.execute("http://" + ipaddr + ":" + port + "/data");
*/
    }

    public void addServiceListener(DataChangeListener toadd) {
        Listeners.add(toadd);
    }


   public String getTimestamp() {
       Calendar c = Calendar.getInstance();
       SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss.SSS");
       String strDate = sdf.format(c.getTime());
return strDate;
    }

    private void setAllData (int steps, float d, float ang, float xAcc, float yAcc,   float zAcc, float phoneXAcc, float phoneYAcc, float phoneZacc, float min1, float max1, float min2) {
        step_count = steps;
        step_length = d;
        angle = ang;
        /*String value1 = "step="+Integer.toString(steps)+"&";
        value1 += "stepLength=" + Float.toString(step_length)+"&";
        value1 += "stepAngle=" + Float.toString(ang)+"&";
        value1 += "xAcc="+ Float.toString(xAcc)+"&";
        value1 += "yAcc="+ Float.toString(yAcc)+"&";
        value1 += "zAcc="+ Float.toString(zAcc)+"&";
        value1 += "phoneXAcc="+ Float.toString(phoneXAcc)+"&";
        value1 += "phoneYAcc="+ Float.toString(phoneYAcc)+"&";
        value1 += "phoneZAcc="+ Float.toString(phoneZacc)+"&";
        value1 += "min1="+ Float.toString(min1)+"&";
        value1 += "min2="+ Float.toString(min2)+"&";
        value1 += "max1="+ Float.toString(max1)+"&";
        value1 += "time="+ getTimestamp();
*/
        String value1 = Integer.toString(steps)+",";
        value1 += Float.toString(step_length)+",";
        value1 += Float.toString(ang)+",";
        value1 += Float.toString(xAcc)+",";
        value1 += Float.toString(yAcc)+",";
        value1 += Float.toString(zAcc)+",";
        value1 += Float.toString(phoneXAcc)+",";
        value1 += Float.toString(phoneYAcc)+",";
        value1 += Float.toString(phoneZacc)+",";
        value1 += Float.toString(min1)+",";
        value1 += Float.toString(min2)+",";
        value1 += Float.toString(max1)+",";
        value1 += getTimestamp();

        if(counter <= maxDataCount ){
            dataBuffer[counter-1] = value1;
            counter++;
        }
        else{
            postData(dataBuffer);
            counter = 1;

        }

        /*for (DataChangeListener ping : Listeners) {
            ping.DataChangedEvent(step_count, step_length, angle, getTimestamp());
        }*/

        step_count = 0;
        step_length = 0;
    }


    public class MyBinder extends Binder {
        StepService getService() {
            return StepService.this;
        }
    }
}