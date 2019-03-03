package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.Map;

public class AutoSettingActivity extends AppCompatActivity {

    private Spinner spShop;
    private TextView tvFloorPlanInfo;
    private TextView tvFloor;

    public AMapLocationClient mLocationClient = null;
    public AMapLocationClientOption mLocationOption = null;

    private static final String TAG = "AutoSettingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_setting);
        //开始定位
        startLocaion();
        tvFloorPlanInfo = findViewById(R.id.tv_floor_plan_info);
        spShop = findViewById(R.id.sp_shop);
        tvFloor = findViewById(R.id.tv_floor);
        ArrayAdapter<String> shopAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Constant.SHOP_NAMES);
        spShop.setAdapter(shopAdapter);
        //spShop.setSelection(FileUtil.getSPInt(AutoSettingActivity.this,"shopSelection"));
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

    public void startLocaion(){

        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationClient.setLocationListener(mLocationListener);

        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }


    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation !=null ) {
                if (amapLocation.getErrorCode() == 0) {
                    String floorInfo = amapLocation.getFloor();
                    if(floorInfo!=null && !floorInfo.equals("")){
                        tvFloor.setText(floorInfo);
                    }
                    //定位成功回调信息，设置相关消息
//                    Log.i(TAG,"当前定位结果来源-----"+amapLocation.getLocationType());//获取当前定位结果来源，如网络定位结果，详见定位类型表
//                    Log.i(TAG,"纬度 ----------------"+amapLocation.getLatitude());//获取纬度
//                    Log.i(TAG,"经度-----------------"+amapLocation.getLongitude());//获取经度
//                    Log.i(TAG,"精度信息-------------"+amapLocation.getAccuracy());//获取精度信息
//                    Log.i(TAG,"地址-----------------"+amapLocation.getAddress());//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//                    Log.i(TAG,"国家信息-------------"+amapLocation.getCountry());//国家信息
//                    Log.i(TAG,"省信息---------------"+amapLocation.getProvince());//省信息
//                    Log.i(TAG,"城市信息-------------"+amapLocation.getCity());//城市信息
//                    Log.i(TAG,"城区信息-------------"+amapLocation.getDistrict());//城区信息
//                    Log.i(TAG,"街道信息-------------"+amapLocation.getStreet());//街道信息
//                    Log.i(TAG,"街道门牌号信息-------"+amapLocation.getStreetNum());//街道门牌号信息
//                    Log.i(TAG,"城市编码-------------"+amapLocation.getCityCode());//城市编码
//                    Log.i(TAG,"地区编码-------------"+amapLocation.getAdCode());//地区编码
//                    Log.i(TAG,"当前定位点的信息-----"+amapLocation.getAoiName());//获取当前定位点的AOI信息
//                    Log.i(TAG,"当前定位点的楼层-----"+amapLocation.getFloor());//获取当前楼层
                    //39.993915,116.34668
                    double latitude = amapLocation.getLatitude();
                    double longitude = amapLocation.getLongitude();
                    int shopSelection = Constant.getShopSelectionByDis(latitude,longitude);
                    spShop.setSelection(shopSelection);
                    toast("latitude:"+latitude+",longitude:"+longitude);
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                    spShop.setSelection(FileUtil.getSPInt(AutoSettingActivity.this,"shopSelection"));
                }
            }
        }
    };

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
