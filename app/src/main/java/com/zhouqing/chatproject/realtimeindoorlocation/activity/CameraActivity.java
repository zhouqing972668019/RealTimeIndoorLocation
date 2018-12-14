package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.camera.CameraSource;
import com.zhouqing.chatproject.realtimeindoorlocation.camera.CameraSourcePreview;
import com.zhouqing.chatproject.realtimeindoorlocation.model.GraphicOverlay;
import com.zhouqing.chatproject.realtimeindoorlocation.text_detection.TextRecognitionProcessor;

import java.io.IOException;

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
    }

    //控制按钮的两个方法
    public void collectionStart(){
        btnControl.setText("Stop");
        createCameraSource();
        startCameraSource();
    }

    public void collectionStop(){
        btnControl.setText("Start");
        preview.stop();
        StringBuilder textDetectionInfoAll = textRecognitionProcessor.getTextDetectionInfoAll();
        Log.d(TAG, "textDetectionInfoAll:"+textDetectionInfoAll);
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
}
