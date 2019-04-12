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
import com.zhouqing.chatproject.realtimeindoorlocation.model.Text;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;
import com.zhouqing.chatproject.realtimeindoorlocation.util.HTTPUtil;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TextRecActivity extends AppCompatActivity {

    private Button btnRecognize;
    private TextView tvResult;

    private StringBuilder resultSB;

    private List<String> fileNameList;
    private String finalFileName;

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
                //runSougouTextRecognition();
            }
        });
    }

    private void runSougouTextRecognition(){
        fileNameList = FileUtil.getFileName(Constant.TEXT_DATASET);
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder finalResult = new StringBuilder();
                for(String fileName:fileNameList){
                    long startTime = System.currentTimeMillis();
                    String base64 = FileUtil.getImageStr(Constant.TEXT_DATASET+fileName);
                    String answer = HTTPUtil.SougoOcrRequest(base64);
                    List<Text> textList = new ArrayList<>();
                    try {
                        HTTPUtil.parseOCRResponse(answer,textList);
                        StringBuilder line = new StringBuilder();
                        for(Text text:textList){
                            line.append(text.content.replace("\n"," ")).append(" ");
                        }
                        System.out.println("answer:"+line.toString());
                        long time = System.currentTimeMillis()-startTime;
                        finalResult.append(fileName).append(",").append(line).append(",")
                                .append(time).append("\n");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                FileUtil.writeStrToPath("textResult",finalResult.toString(),Constant.TEXT_DATASET);
            }
        }).start();
    }


    private void runTextRecognition() throws FileNotFoundException {
        fileNameList = FileUtil.getFileName(Constant.TEXT_DATASET);
        finalFileName = fileNameList.get(fileNameList.size() - 1);
        String fileName = fileNameList.get(0);
        FileInputStream fis = new FileInputStream(Constant.TEXT_DATASET + fileName);
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

    private void processTextRecognitionResult(FirebaseVisionText texts, final int index) {
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
        if(index != fileNameList.size() - 1){
            startTime = System.currentTimeMillis();
            String fileName = fileNameList.get(index + 1);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(Constant.TEXT_DATASET + fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap bitmap  = BitmapFactory.decodeStream(fis);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            recognizer.processImage(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText texts) {
                                    processTextRecognitionResult(texts,index+1);
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
        else{
            btnRecognize.setEnabled(true);
            tvResult.setText(resultSB.toString());
            FileUtil.writeStrToPath("textResult",resultSB.toString(),Constant.TEXT_DATASET);
        }
    }
}
