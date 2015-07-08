package com.example.cdac.servicetouploaddata;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.cdac.servicetouploaddata.StepService.MyBinder;

public class MainActivity extends ActionBarActivity implements DataChangeListener {
    StepService mStepService;
    boolean mServiceBound = false;
    TextView timestampText = null;
    private float cumulativeDistance = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        timestampText = (TextView) findViewById(R.id.timestamp_text);
        Button captureButton = (Button) findViewById(R.id.capture);
        Button stopServiceButon = (Button) findViewById(R.id.stop_service);
        captureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCaptureSignal();
            }
        });

        stopServiceButon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mServiceBound) {
                    unbindService(mServiceConnection);
                    mServiceBound = false;
                }
                Intent intent = new Intent(MainActivity.this,
                        StepService.class);
                stopService(intent);
            }
        });

    }

    private void clearAll() {
        cumulativeDistance = 0;
    }

    public void sendCaptureSignal(){
        AsyncDataTransfer transfer = new AsyncDataTransfer();
        transfer.execute("capture");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, StepService.class);
        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBinder myBinder = (MyBinder) service;
            mStepService = myBinder.getService();
            mStepService.addServiceListener(MainActivity.this);

            mServiceBound = true;
        }
    };

    @Override
    public void DataChangedEvent(int newCount, float newDistance, float newAngle, String newTime) {
        cumulativeDistance += newDistance;
        timestampText.setText(newTime + "\nAngle: " + Float.toString(newAngle) + "\n StepDistance(ft): " + Float.toString((newDistance)) + "\nTotalDistance(ft): " + Float.toString(cumulativeDistance)+ "\nStepDistance(m)" + Float.toString(newDistance/3.2808f)+ "\nTotalDistance(m): " + Float.toString(cumulativeDistance/3.2808f));
        FileOperation fOp = new FileOperation();
//        fOp.write("");
    }
}