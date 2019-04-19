package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class TextRecAllActivity extends AppCompatActivity {

    private Button btnRecognize;
    private TextView tvResult;

    private StringBuilder resultSB;

    private List<String> fileNameList;
    private List<String> folderList;
    private int folderIndex;

    private long startTime;
    private long endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_rec);
        btnRecognize = findViewById(R.id.btn_recognize);
        tvResult = findViewById(R.id.tv_result);
        resultSB = new StringBuilder();
        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    runTextRecognition();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void runTextRecognition() throws FileNotFoundException {
        folderList = FileUtil.getChildFolder(Constant.TEXT_ALL_DATASET);
        folderIndex = 0;
        fileNameList = FileUtil.getFileName(Constant.TEXT_ALL_DATASET+folderList.get(folderIndex)+"/");
        String fileName = fileNameList.get(0);
        FileInputStream fis = new FileInputStream(Constant.TEXT_ALL_DATASET+folderList.get(folderIndex)+"/" + fileName);
        Bitmap bitmap  = BitmapFactory.decodeStream(fis);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        btnRecognize.setEnabled(false);
        startTime = System.currentTimeMillis();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts,0);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });


    }

    private void processTextRecognitionResult(FirebaseVisionText texts, int index) {
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        String line = fileNameList.get(index)+",";
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    FirebaseVisionText.Element element = elements.get(k);
                    //Rect rect = element.getBoundingBox();
                    //line += element.getText()+" "+rect.left+" "+rect.bottom+" "+rect.right+" "+rect.bottom;
                    line += element.getText()+" ";
                }
            }
        }
        line += ","+(System.currentTimeMillis() - startTime);
        resultSB.append(line).append("\n");
        System.out.println("index:"+index+",fileName:"+fileNameList.get(index)+",line:"+line);
        if(index == fileNameList.size() - 1){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnRecognize.setEnabled(true);
                    tvResult.setText(resultSB.toString());
                }
            });
            FileUtil.writeStrToPath("textResult",resultSB.toString(),Constant.TEXT_ALL_DATASET+folderList.get(folderIndex)+"/");
            if(folderIndex == folderList.size()-1){
                return;
            }
            else{
                folderIndex++;
                fileNameList = FileUtil.getFileName(Constant.TEXT_ALL_DATASET+folderList.get(folderIndex)+"/");
                index = 0;
                resultSB = new StringBuilder();
            }
        }
        startTime = System.currentTimeMillis();
        String fileName = fileNameList.get(index + 1);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(Constant.TEXT_ALL_DATASET+folderList.get(folderIndex)+"/" + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bitmap  = BitmapFactory.decodeStream(fis);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        final int finalIndex = index;
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts, finalIndex +1);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }
}
