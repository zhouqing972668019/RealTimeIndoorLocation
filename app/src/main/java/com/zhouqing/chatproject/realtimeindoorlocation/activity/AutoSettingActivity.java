package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.Map;

public class AutoSettingActivity extends AppCompatActivity {

    private Spinner spShop;
    private TextView tvFloorPlanInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_setting);
        tvFloorPlanInfo = findViewById(R.id.tv_floor_plan_info);
        spShop = findViewById(R.id.sp_shop);
        ArrayAdapter<String> shopAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Constant.SHOP_NAMES);
        spShop.setAdapter(shopAdapter);
        spShop.setSelection(FileUtil.getSPInt(AutoSettingActivity.this,"shopSelection"));
        spShop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //toast("you selected:"+position);
                FileUtil.loadFloorPlan(AutoSettingActivity.this,position);
                showFloorPlanInfo();
                FileUtil.saveSpInt(AutoSettingActivity.this,"shopSelection",position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        showFloorPlanInfo();
    }

    //显示当前本地的平面图信息
    public void showFloorPlanInfo(){
        Map<String,StandardLocationInfo> hashMap = FileUtil.getPOILocation(AutoSettingActivity.this);
        String text = "平面图信息如下：\n";
        for(String name:hashMap.keySet()){
            text += name+":"+"("+hashMap.get(name).getX()+","+hashMap.get(name).getY()+")"+"\n";
        }
        tvFloorPlanInfo.setText(text);
    }

    public void toast(String content){
        Toast.makeText(AutoSettingActivity.this,content,Toast.LENGTH_SHORT).show();
    }
}
