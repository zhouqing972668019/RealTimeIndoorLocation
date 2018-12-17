package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouqing.chatproject.realtimeindoorlocation.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnFloorPlanAuto;
    private Button btnFloorPlanManual;
    private Button btnCollectionData;
    private Button btnShowResult;
    private TextView tvContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnFloorPlanAuto = findViewById(R.id.btn_floor_plan_auto);
        btnFloorPlanManual = findViewById(R.id.btn_floor_plan_manual);
        btnCollectionData = findViewById(R.id.btn_collection_data);
        btnShowResult = findViewById(R.id.btn_show_result);
        tvContent = findViewById(R.id.tv_content);
        btnFloorPlanManual.setOnClickListener(this);
        btnFloorPlanAuto.setOnClickListener(this);
        btnCollectionData.setOnClickListener(this);
        btnShowResult.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_floor_plan_manual:
                startActivity(new Intent(MainActivity.this,ManualSettingActivity.class));
                break;
            case R.id.btn_collection_data:
                startActivityForResult(new Intent(MainActivity.this,CameraActivity.class),1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    boolean isSuccess = data.getBooleanExtra("isSuccess", false);
                    if(!isSuccess){
                        tvContent.setText("Lack of POIs to locate!");
                    }
                    else{
                        double answer_x = data.getDoubleExtra("answer_x", 0d);
                        double answer_y = data.getDoubleExtra("answer_y", 0d);
                        tvContent.setText("answer:("+answer_x+","+answer_y+")");
                    }
                }
                break;
            default:
        }
    }
}
