package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

import java.io.IOException;
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

    private static final String TAG = "CameraActivity";

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

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

        Intent serviceIntent = new Intent(CameraActivity.this,SensorRecordService.class);
        startService(serviceIntent);
    }

    //控制按钮的两个方法
    public void collectionStart(){
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
        FileUtil.writeStrToPath(sensorInfoList.toString().replace(",","\n"), Constant.COLLECTION_DATA_PATH);
        FileUtil.writeStrToPath(textDetectionInfoList.toString().replace(",","\n"), Constant.COLLECTION_DATA_PATH);
        indoorLocation(textDetectionInfoList,sensorInfoList);
        //Log.d(TAG, "textDetectionInfoAll:"+textDetectionInfoAll.toString());
        //Log.d(TAG, "sensorInfoAll:"+sensorInfoAll.toString());
        this.finish();
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

    //-----------------------------------------------------------------------------------------
    public void indoorLocation(List<String> textDetectionList, List<String> sensorInfoList){
        //获取本地的平面图信息
        Map<String, StandardLocationInfo> floorPlanMap = FileUtil.getPOILocation(CameraActivity.this);
        System.out.println("floorPlanMap:" + floorPlanMap.toString());
        //获取时间戳和方向角的对应关系
        Map<String,Double> oriMap = new LinkedHashMap<>();
        Map<String,Double> gyroOriMap = new LinkedHashMap<>();
        Map<String,Double> magAccOriMap = new LinkedHashMap<>();
        LocationInfoUtil.getOriInfo(oriMap,gyroOriMap,magAccOriMap,sensorInfoList);
        System.out.println("oriMap:" + oriMap.toString());
        //获取文字识别结果与真实poi的关系
        Size previewSize = cameraSource.getPreviewSize();
        System.out.println("previewSizeWidth:" + previewSize.getWidth()+"");
        Map<String, TextDetectionAndPoi> textDetectionInfoMap = new LinkedHashMap<>();
        LocationInfoUtil.getTextDetectionInfo(previewSize,floorPlanMap,textDetectionList,textDetectionInfoMap);
        System.out.println("textDetectionInfoMap:"+textDetectionInfoMap.toString());
    }
}
