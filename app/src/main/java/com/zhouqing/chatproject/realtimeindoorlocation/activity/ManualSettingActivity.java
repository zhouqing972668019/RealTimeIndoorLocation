package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.HashMap;
import java.util.Map;

public class ManualSettingActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView tv_floor_plan_info;
    private Button btn_insert;
    private Button btn_delete;
    private Button btn_deleteAll;
    private EditText et_poiName;
    private EditText et_poiX;
    private EditText et_poiY;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_setting);
        initViews();
        //显示当前平面图信息
        showFloorPlanInfo();
    }

    public void initViews(){
        tv_floor_plan_info = (TextView) findViewById(R.id.tv_floor_plan_info);
        btn_insert = (Button) findViewById(R.id.btn_insert);
        btn_delete = (Button) findViewById(R.id.btn_delete);
        btn_deleteAll = (Button) findViewById(R.id.btn_deleteAll);
        et_poiName = (EditText) findViewById(R.id.et_poi_name);
        et_poiX = (EditText) findViewById(R.id.et_poi_x);
        et_poiY = (EditText) findViewById(R.id.et_poi_y);
        btn_insert.setOnClickListener(this);
        btn_deleteAll.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
    }

    public void showFloorPlanInfo(){
        Map<String,StandardLocationInfo> hashMap = FileUtil.getPOILocation(ManualSettingActivity.this);
        String text = "平面图信息如下：\n";
        for(String name:hashMap.keySet()){
            text += name+":"+"("+hashMap.get(name).getX()+","+hashMap.get(name).getY()+")"+"\n";
        }
        tv_floor_plan_info.setText(text);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_insert:
                insertInfo();
                break;
            case R.id.btn_delete:
                deleteInfo();
                break;
            case R.id.btn_deleteAll:
                deleteAll();
                break;
        }
    }

    public void insertInfo(){
        Map<String,StandardLocationInfo> hashMap = FileUtil.getPOILocation(ManualSettingActivity.this);
        String poiName = et_poiName.getText().toString();
        double poiX = Double.parseDouble(et_poiX.getText().toString());
        double poiY = Double.parseDouble(et_poiY.getText().toString());
        StandardLocationInfo standardLocationInfo = new StandardLocationInfo(poiX,poiY);
        hashMap.put(poiName,standardLocationInfo);
        FileUtil.saveLocationToFile(ManualSettingActivity.this,hashMap);
        showFloorPlanInfo();
        reset();
    }

    public void deleteInfo(){
        Map<String,StandardLocationInfo> hashMap = FileUtil.getPOILocation(ManualSettingActivity.this);
        String poiName = et_poiName.getText().toString();
        hashMap.remove(poiName);
        FileUtil.saveLocationToFile(ManualSettingActivity.this,hashMap);
        showFloorPlanInfo();
        reset();
    }

    public void deleteAll(){
        Map<String,StandardLocationInfo> hashMap = new HashMap<>();
        FileUtil.saveLocationToFile(ManualSettingActivity.this,hashMap);
        showFloorPlanInfo();
        reset();
    }

    public void reset(){
        et_poiY.setText("");
        et_poiX.setText("");
        et_poiName.setText("");
    }
}
