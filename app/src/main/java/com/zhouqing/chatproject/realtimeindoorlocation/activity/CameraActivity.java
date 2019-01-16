package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.images.Size;
import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.camera.CameraSource;
import com.zhouqing.chatproject.realtimeindoorlocation.camera.CameraSourcePreview;
import com.zhouqing.chatproject.realtimeindoorlocation.model.GraphicOverlay;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionAndPoi;
import com.zhouqing.chatproject.realtimeindoorlocation.service.SensorRecordService;
import com.zhouqing.chatproject.realtimeindoorlocation.text_detection.TextRecognitionProcessor;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;
import com.zhouqing.chatproject.realtimeindoorlocation.util.LocationInfoUtil;
import com.zhouqing.chatproject.realtimeindoorlocation.util.TextDetection;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CameraActivity extends AppCompatActivity {

    //region ----- Instance Variables -----

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private TextRecognitionProcessor textRecognitionProcessor;
    private Button btnControl;
    private Button btnLocalization;
    private Spinner spFolder;
    private LinearLayout llFolder;

    public static String TIMESTAMP_PATH = null;

    private static final String TAG = "CameraActivity";

    //待定位文件夹
    private static int folderIndex = 0;
    String[] folders;

    //接收服务传过来的数据并显示
    private MyReceiver receiver=null;

    private TextView tvMagAccOri;
    private TextView tvGyroOri;
    private TextView tvOri;

    DecimalFormat d = new DecimalFormat("#.##");

    //endregion

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle=intent.getExtras();
            double angle = bundle.getDouble("angle");
            double gyroAngle = bundle.getDouble("gyroAngle");
            double accMagAngle = bundle.getDouble("accMagAngle");
            //Log.d(TAG, "onReceive->angle:"+angle);
            tvOri.setText(d.format(angle) + "");
            tvGyroOri.setText(d.format(gyroAngle) + "");
            tvMagAccOri.setText(d.format(accMagAngle) + "");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tvOri = findViewById(R.id.tv_ori);
        tvGyroOri = findViewById(R.id.tv_gyro_ori);
        tvMagAccOri = findViewById(R.id.tv_mag_acc_ori);
        //选择文件夹，直接定位
        spFolder = findViewById(R.id.sp_folder);
        folders = LocationInfoUtil.getFoldersByTimeDesc();
        ArrayAdapter<String> folderAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,folders);
        spFolder.setAdapter(folderAdapter);
        spFolder.setSelection(FileUtil.getSPInt(CameraActivity.this,"folderSelection"));
        spFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //toast("you selected:"+position);
                FileUtil.saveSpInt(CameraActivity.this,"folderSelection",position);
                folderIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        llFolder = findViewById(R.id.ll_folder);

        //FirebaseApp.initializeApp(this);

        preview = (CameraSourcePreview) findViewById(R.id.camera_source_preview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.graphics_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        btnControl = findViewById(R.id.btn_control);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnControl.getText().equals("Start")){
                    collectionStart();
                }
                else{
                    collectionStop();
                }
            }
        });

        btnLocalization = findViewById(R.id.btn_localization);
        btnLocalization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localization();
            }
        });

        Intent serviceIntent = new Intent(CameraActivity.this,SensorRecordService.class);
        startService(serviceIntent);

        receiver=new MyReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.zhouqing.chatproject.realtimeindoorlocation.service.SensorRecordService");
        CameraActivity.this.registerReceiver(receiver,filter);
    }

    //控制按钮的两个方法
    public void collectionStart(){
        btnLocalization.setVisibility(View.GONE);
        llFolder.setVisibility(View.GONE);
        btnControl.setText("Stop");
        createCameraSource();
        startCameraSource();
        String timeString =System.currentTimeMillis()+"";
        SensorRecordService.instance().startLogging(timeString);
    }

    public void collectionStop(){
        btnControl.setText("Start");
        preview.stop();
        List<String> sensorInfoList = SensorRecordService.instance().stopLoggingAndReturnSensorInfo();
        List<String> textDetectionInfoList = textRecognitionProcessor.getTextDetectionInfoAll();
        //寻找文本识别区域
        Constant.findAreaOfTextDetection(textDetectionInfoList);
        int shopSelection = FileUtil.getSPInt(CameraActivity.this,"shopSelection");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        TIMESTAMP_PATH = Constant.SHOP_NAMES[shopSelection] + "_" + df.format(new Date())+"/";// new Date()为获取当前系统时间
        final String sensorContent = LocationInfoUtil.getStrBySensorInfoList(sensorInfoList);
        final String textContent = LocationInfoUtil.getStrByTextDetectionInfoList(textDetectionInfoList);
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.writeStrToPath("sensor", sensorContent, Constant.COLLECTION_DATA_PATH + TIMESTAMP_PATH);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.writeStrToPath("textDetection", textContent, Constant.COLLECTION_DATA_PATH + TIMESTAMP_PATH);
            }
        }).start();
        indoorLocation(textDetectionInfoList,sensorInfoList,0);
        //Log.d(TAG, "textDetectionInfoAll:"+textDetectionInfoAll.toString());
        //Log.d(TAG, "sensorInfoAll:"+sensorInfoAll.toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /** Stops the camera. */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
        CameraActivity.this.unregisterReceiver(receiver);
    }

    private void createCameraSource() {

        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
            cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
        }

        textRecognitionProcessor = new TextRecognitionProcessor();
        cameraSource.setMachineLearningFrameProcessor(textRecognitionProcessor);
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    //直接对上一次记录的中间文件定位（可能修改了室内平面图信息）
    public void localization(){
        List<String> sensorInfoList = new ArrayList<>();
        List<String> textDetectionInfoList = new ArrayList<>();
        try {
            //直接定位最新的数据
//            LocationInfoUtil.getRecentCollectionData(sensorInfoList,textDetectionInfoList);
            LocationInfoUtil.getTargetCollectionData(sensorInfoList,textDetectionInfoList,folders[folderIndex]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        indoorLocation(textDetectionInfoList,sensorInfoList,-1);
    }

    //-----------------------------------------------------------------------------------------
    public void indoorLocation(List<String> textDetectionList, List<String> sensorInfoList, int method){
        //获取本地的平面图信息
        Map<String, StandardLocationInfo> floorPlanMap = FileUtil.getPOILocation(CameraActivity.this);
        //System.out.println("floorPlanMap:" + floorPlanMap.toString());
        //获取时间戳和方向角的对应关系
        Map<String,Double> oriMap = new LinkedHashMap<>();
        Map<String,Double> gyroOriMap = new LinkedHashMap<>();
        Map<String,Double> magAccOriMap = new LinkedHashMap<>();
        LocationInfoUtil.getOriInfo(oriMap,gyroOriMap,magAccOriMap,sensorInfoList);
        System.out.println("oriMap:" + oriMap.toString());
        System.out.println("magAccOriMap:" + magAccOriMap);
        System.out.println("gyroOriMap:" + gyroOriMap);
        //获取文字识别结果与真实poi的关系
        int previewWidth = FileUtil.getSPInt(CameraActivity.this,"previewWidth");
        if(previewWidth == 0){
            Size previewSize = cameraSource.getPreviewSize();
            previewWidth = previewSize.getHeight();
            FileUtil.saveSpInt(CameraActivity.this,"previewWidth",previewWidth);
        }
        System.out.println("previewSizeWidth:" + previewWidth +"");
        Map<String, TextDetectionAndPoi> textDetectionInfoMap = new LinkedHashMap<>();
        Map<String, Integer> POIDetectionNumMap = new LinkedHashMap<>();
        LocationInfoUtil.getTextDetectionInfo(previewWidth,floorPlanMap,textDetectionList,textDetectionInfoMap, POIDetectionNumMap);
//        System.out.println("POIDetectionNumMap:"+POIDetectionNumMap.toString());
        //为每一个POI添加角度信息
        for(String POIName:textDetectionInfoMap.keySet()){
            TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(POIName);
            textDetectionAndPoi.ori_angle = LocationInfoUtil.getOriByTimeStamp(oriMap ,textDetectionAndPoi.timeStamp);
            textDetectionAndPoi.gyro_ori_angle = LocationInfoUtil.getOriByTimeStamp(gyroOriMap,textDetectionAndPoi.timeStamp);
            textDetectionAndPoi.mag_acc_angle = LocationInfoUtil.getOriByTimeStamp(magAccOriMap,textDetectionAndPoi.timeStamp);
        }
        System.out.println("textDetectionInfoMap:"+textDetectionInfoMap.toString());

        //将当前中间信息保存
        final StringBuilder resultSB = new StringBuilder();
        LocationInfoUtil.getResultPrintContent(resultSB,textDetectionInfoMap,floorPlanMap);

        //判断有无重复的POI出现
        String showInfo = "";
        if(!LocationInfoUtil.isPOINumMoreThanOne(POIDetectionNumMap)){//不额外处理 直接计算位置
            if(textDetectionInfoMap.size() >= 3){
                List<Double> angleList = new ArrayList<>();//方向传感器z轴读数
                List<Double> gyroAngleList = new ArrayList<>();//校正的陀螺仪结果
                List<Double> magAccAngleList = new ArrayList<>();//重力+磁场结果
                List<Double> complexGyroAngleList = new ArrayList<>();//合成陀螺仪角度
                List<String> POINameList = new ArrayList<>();
                LocationInfoUtil.getAngleOfPOIs(textDetectionInfoMap,angleList,POINameList,
                        gyroAngleList,magAccAngleList,complexGyroAngleList,sensorInfoList);
                List<Double[]> coordinateList = new ArrayList<>();// 获取已识别的角标位置信息--方向传感器
                List<Double[]> gyro_coordinateList = new ArrayList<>();// 获取已识别的角标位置信息--陀螺仪
                List<Double[]> mag_acc_coordinateList = new ArrayList<>();// 获取已识别的角标位置信息--加速度+磁场
                List<Double[]> complex_gyro_coordinateList = new ArrayList<>();// 获取已识别的角标位置信息--合成角速度
                LocationInfoUtil.getCoordinateList(textDetectionInfoMap,floorPlanMap,coordinateList,
                        gyro_coordinateList, mag_acc_coordinateList,
                        complexGyroAngleList,complex_gyro_coordinateList);
                System.out.println("angleList:"+angleList.toString());
                System.out.println("gyroAngleList:"+gyroAngleList.toString());
                System.out.println("magAccAngleList:"+magAccAngleList.toString());
                System.out.println("complexGyroAngleList:"+complexGyroAngleList);
                final List<Integer> direction = new ArrayList<>();
                for (int j = 0; j < coordinateList.size(); j++) {
                    direction.add(-1);
                }
                Double[] answer = TextDetection.cal_corrdinate(angleList, coordinateList, direction);//方向传感器
                if(answer == null){
                    answer = new Double[]{0d,0d};
                }
                Double[] gyro_answer = TextDetection.cal_corrdinate(gyroAngleList, gyro_coordinateList, direction);//陀螺仪
                if(gyro_answer == null){
                    gyro_answer = new Double[]{0d,0d};
                }
                Double[] mag_acc_answer = TextDetection.cal_corrdinate(magAccAngleList, mag_acc_coordinateList, direction);//重力+磁场
                if(mag_acc_answer == null){
                    mag_acc_answer = new Double[]{0d,0d};
                }
                Double[] complex_gyro_answer = TextDetection.cal_corrdinate(complexGyroAngleList,complex_gyro_coordinateList,direction);//陀螺仪合成
                if(complex_gyro_answer == null){
                    complex_gyro_answer = new Double[]{0d,0d};
                }
                System.out.println("answer:"+ Arrays.toString(answer));
                System.out.println("gyro_answer:"+ Arrays.toString(gyro_answer));
                System.out.println("mag_acc_answer:"+ Arrays.toString(mag_acc_answer));
                System.out.println("complex_gyro_answer:"+Arrays.toString(complex_gyro_answer));
                StringBuilder showInfoSB = new StringBuilder();
                LocationInfoUtil.getLocationResult(showInfoSB,answer,textDetectionInfoMap,
                        POINameList,angleList);
                //保存定位结果信息 保存到文件
                LocationInfoUtil.getResultPrintContentFinal(resultSB,answer,gyro_answer,mag_acc_answer,complex_gyro_answer,
                        POINameList,angleList,gyroAngleList,magAccAngleList,complexGyroAngleList);
//                if(method == -1){
//                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
//                    TIMESTAMP_PATH = df.format(new Date())+"/";// new Date()为获取当前系统时间
//                }
                FileUtil.saveLocationResult(CameraActivity.this,answer,gyro_answer,mag_acc_answer,complex_gyro_answer);
                //通过本次定位结果确定x轴基准角
                float startAngle = LocationInfoUtil.getStartAngle(textDetectionInfoMap,floorPlanMap,answer);
                FileUtil.saveSpFloat(CameraActivity.this,"startAngle",startAngle);
                //保存当前用于定位结果的POI名称
                FileUtil.savePOINames(CameraActivity.this,textDetectionInfoMap);
                showInfoSB.append("startAngle:").append(startAngle);
                resultSB.append("startAngle:").append(startAngle).append("\n");
                if(method == 0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FileUtil.writeStrToPath("result", resultSB.toString(), Constant.COLLECTION_DATA_PATH + TIMESTAMP_PATH);
                        }
                    }).start();
                }
                showInfo = showInfoSB.toString();
            }
            else{
                showInfo = "Lack of POIs to locate!";
            }
            Intent intent = getIntent();
            intent.putExtra("showInfo",showInfo);
            setResult(RESULT_OK, intent);
        }
        //某个POI出现了多次 需要特殊处理
        else{

        }
        this.finish();
    }
}
