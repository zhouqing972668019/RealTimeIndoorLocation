package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SensorCalibrateActivity extends AppCompatActivity implements SensorEventListener {

    //传感器管理器
    private SensorManager mSensorManager;
    //磁力传感器读数
    List<String> magList;
    private Button btnSensorController;
    private TextView tvContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_calibrate);
        mSensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        magList = new ArrayList<>();
        tvContent = findViewById(R.id.tv_content);
        showContent();
        btnSensorController = findViewById(R.id.btn_sensor_controller);
        btnSensorController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnSensorController.getText().equals("Sensor Calibrate Start")){
                    btnSensorController.setText("Sensor Calibrate Stop");
                    // 为磁场传感器注册监听器
                    mSensorManager.registerListener(SensorCalibrateActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
                }
                else{
                    btnSensorController.setText("Sensor Calibrate Start");
                    //取消监听
                    mSensorManager.unregisterListener(SensorCalibrateActivity.this);
                    String hardIronStr = Constant.hardIron(magList);
                    String softIronStr = Constant.softIron(magList);
                    FileUtil.saveSPString(SensorCalibrateActivity.this,"hardIron",hardIronStr);
                    FileUtil.saveSPString(SensorCalibrateActivity.this,"softIron",softIronStr);
                    tvContent.setText("(hardIron)"+hardIronStr+"\n"+"(softIron)"+softIronStr);
                    magList.clear();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;
        //获取传感器类型
        int type = sensorEvent.sensor.getType();
        System.out.println(type+","+ Arrays.toString(values));
        switch (type)
        {

            case Sensor.TYPE_MAGNETIC_FIELD:
                String mag = values[0]+","+values[1]+","+values[2];
                magList.add(mag);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void showContent(){
        String hardIronStr = FileUtil.getSPString(SensorCalibrateActivity.this,"hardIron");
        String softIronStr = FileUtil.getSPString(SensorCalibrateActivity.this,"softIron");
        tvContent.setText("(hardIron)"+hardIronStr+"\n"+"(softIron)"+softIronStr);
    }
}
