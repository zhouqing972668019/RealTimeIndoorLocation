package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnFloorPlanAuto;
    private Button btnFloorPlanManual;
    private Button btnCollectionData;
    private Button btnShowResult;
    private TextView tvContent;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnFloorPlanAuto = findViewById(R.id.btn_floor_plan_auto);
        btnFloorPlanManual = findViewById(R.id.btn_floor_plan_manual);
        btnCollectionData = findViewById(R.id.btn_collection_data);
        btnCollectionData = findViewById(R.id.btn_collection_data);
        btnShowResult = findViewById(R.id.btn_show_result);
        tvContent = findViewById(R.id.tv_content);
        btnFloorPlanManual.setOnClickListener(this);
        btnFloorPlanAuto.setOnClickListener(this);
        btnCollectionData.setOnClickListener(this);
        btnShowResult.setOnClickListener(this);

        //第一个activity中创建图片资源
        if(Constant.rightBitmap == null){
            Constant.rightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.right_arrow, null);
        }
        if(Constant.leftBitmap == null){
            Constant.leftBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.left_arrow,null);
        }
        if(Constant.centerBitmap == null){
            Constant.centerBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.center,null);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_floor_plan_auto:
                startActivity(new Intent(MainActivity.this,AutoSettingActivity.class));
                break;
            case R.id.btn_floor_plan_manual:
                startActivity(new Intent(MainActivity.this,ManualSettingActivity.class));
                break;
            case R.id.btn_collection_data:
                startActivityForResult(new Intent(MainActivity.this,CameraActivity.class),1);
                break;
            case R.id.btn_show_result:
                startActivity(new Intent(MainActivity.this,ShowResultActivity.class));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    String showInfo = data.getStringExtra("showInfo");
                    tvContent.setText(showInfo);
                }
                break;
            default:
        }
    }

}
