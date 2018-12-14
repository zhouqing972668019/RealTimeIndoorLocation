package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.zhouqing.chatproject.realtimeindoorlocation.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnFloorPlanAuto;
    private Button btnFloorPlanManual;
    private Button btnCollectionData;
    private Button btnShowResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnFloorPlanAuto = findViewById(R.id.btn_floor_plan_auto);
        btnFloorPlanManual = findViewById(R.id.btn_floor_plan_manual);
        btnCollectionData = findViewById(R.id.btn_collection_data);
        btnShowResult = findViewById(R.id.btn_show_result);
        btnFloorPlanManual.setOnClickListener(this);
        btnFloorPlanAuto.setOnClickListener(this);
        btnCollectionData.setOnClickListener(this);
        btnShowResult.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_collection_data:
                Intent intent = new Intent(MainActivity.this,CameraActivity.class);
                startActivity(intent);
                break;
        }
    }
}
