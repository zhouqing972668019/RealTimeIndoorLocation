// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.zhouqing.chatproject.realtimeindoorlocation.text_detection;

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.zhouqing.chatproject.realtimeindoorlocation.model.FrameMetadata;
import com.zhouqing.chatproject.realtimeindoorlocation.model.GraphicOverlay;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.Text;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.LocationInfoUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zhouqing.chatproject.realtimeindoorlocation.util.LocationInfoUtil.SIMILARITY_THRESHOLD;

//import com.ajeetkumar.textdetectionusingmlkit.others.VisionProcessorBase;

/**
 * Processor for the text recognition demo.
 */
public class TextRecognitionProcessor {

	private static final String TAG = "TextRecProc";

	private final FirebaseVisionTextRecognizer detector;

	// Whether we should ignore process(). This is usually caused by feeding input data faster than
	// the model can handle.
	private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

	//文字识别结果
	private List<String> textDetectionInfoAll = new ArrayList<>();

	//已识别的POI列表
	private Set<String> POISet = new HashSet<>();

	//上下文
	private Context mContext;

	public TextRecognitionProcessor(Map<String, StandardLocationInfo> floorPlanMap,Context context) {
		detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
		this.floorPlanMap = floorPlanMap;
		mContext = context;
	}

	public Map<String, StandardLocationInfo> floorPlanMap;



	//region ----- Exposed Methods -----


	public void stop() {
		try {
			detector.close();
		} catch (IOException e) {
			Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
		}
	}


	public void process(long timeStamp,ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) throws FirebaseMLException {

		if (shouldThrottle.get()) {
			return;
		}
		FirebaseVisionImageMetadata metadata =
				new FirebaseVisionImageMetadata.Builder()
						.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
						.setWidth(frameMetadata.getWidth())
						.setHeight(frameMetadata.getHeight())
						.setRotation(frameMetadata.getRotation())
						.build();

		detectInVisionImage(timeStamp,FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay);
	}

	//endregion

	//region ----- Helper Methods -----

	protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
		return detector.processImage(image);
	}


	protected void onSuccess(long timeStamp,@NonNull FirebaseVisionText results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {

		System.out.println("frameData:"+frameMetadata.getWidth()+","+frameMetadata.getHeight()+","+frameMetadata.getRotation());
		graphicOverlay.clear();

		List<String> textDetectionList = new ArrayList<>();//当前这一帧画面中出现的文本列表

		List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();

		for (int i = 0; i < blocks.size(); i++) {
			List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
			for (int j = 0; j < lines.size(); j++) {
				List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
				for (int k = 0; k < elements.size(); k++) {

					//打印文本信息
					FirebaseVisionText.Element text = elements.get(k);
					RectF rect = new RectF(text.getBoundingBox());

					String textDetectionInfo = timeStamp+" "+rect.left+" "+rect.top+" "+rect.right+" "+rect.bottom+" "+text.getText();
					Log.d(TAG, textDetectionInfo);
					textDetectionList.add(textDetectionInfo);

				}
			}
		}
		int previewWidth = frameMetadata.getHeight();

		//对当前帧中的文本进行实时处理
		List<TextDetectionInfo> resultList = new ArrayList<>();//对临近文本进行合并
		LocationInfoUtil.processTextDetectionInfo(textDetectionList,resultList);
		TextDetectionInfo resultTextDetectionInfo = null;
		double maxArea = Integer.MIN_VALUE;
		String resultPOIName = "";
		for(TextDetectionInfo textDetectionInfo:resultList){
			String POIName = "";
			int similarity = Integer.MIN_VALUE;
			//判断当前文字识别信息是否与某个POI名称相同(找出相似度最高的POI)
			for(String floorPlanPOIName:floorPlanMap.keySet()){
				if(Constant.isContainChinese(floorPlanPOIName)){
					continue;
				}
				String modifyFloorPlanPOIName = Constant.removeIllegalAlphabet(floorPlanPOIName);
				String modifyTextDetection = Constant.removeIllegalAlphabet(textDetectionInfo.textContent);
				int value = Constant.calculateStringDistance(modifyFloorPlanPOIName,modifyTextDetection);
				if(value > similarity){
					similarity = value;
					POIName = floorPlanPOIName;
				}
			}
			if(similarity > SIMILARITY_THRESHOLD) {
				double area = LocationInfoUtil.calcalateArea(textDetectionInfo);
				if(area > maxArea){
					maxArea = area;
					resultTextDetectionInfo = textDetectionInfo;
					resultPOIName = POIName;
				}
			}
		}
		if(resultTextDetectionInfo != null){
			double left = resultTextDetectionInfo.left;
			double right = resultTextDetectionInfo.right;
			double top = resultTextDetectionInfo.top;
			double bottom = resultTextDetectionInfo.bottom;
			String content = resultTextDetectionInfo.textContent;
			double centerDis = LocationInfoUtil.compareCenterDis(previewWidth, left, right);


			Text detectText = new Text((float) left,(float)top,(float)right,(float)bottom,content);
			//文字中心与屏幕中央基本重合
			if(Math.abs(centerDis) < 10d){
				detectText.pos = 0;
				String textDetectionInfo = timeStamp+" "+left+" "+top+" "+right+" "+bottom+" "+content+" "+resultPOIName;
				textDetectionInfoAll.add(textDetectionInfo);
				//将已识别的POI放入哈希表
				POISet.add(resultPOIName);
				//发送广播
				Intent intent=new Intent();
				intent.putExtra("POINum", POISet.size());
				intent.setAction(Constant.BROADCASTRECEIVER_NAME);
				mContext.sendBroadcast(intent);
			}
			//文字中心在屏幕中央左侧
			else if(centerDis < 0){
				detectText.pos = -1;
			}
			//文字中心在屏幕中央右侧
			else{
				detectText.pos = 1;
			}
			GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, detectText);
			graphicOverlay.add(textGraphic);
		}



	}

	protected void onFailure(@NonNull Exception e) {
		Log.w(TAG, "Text detection failed." + e);
	}

	private void detectInVisionImage(final long timeStamp, FirebaseVisionImage image, final FrameMetadata metadata, final GraphicOverlay graphicOverlay) {

		detectInImage(image)
				.addOnSuccessListener(
						new OnSuccessListener<FirebaseVisionText>() {
							@Override
							public void onSuccess(FirebaseVisionText results) {
								shouldThrottle.set(false);
								TextRecognitionProcessor.this.onSuccess(timeStamp, results, metadata, graphicOverlay);
							}
						})
				.addOnFailureListener(
						new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								shouldThrottle.set(false);
								TextRecognitionProcessor.this.onFailure(e);
							}
						});
		// Begin throttling until this frame of input has been processed, either in onSuccess or
		// onFailure.
		shouldThrottle.set(true);
	}

	//endregion

	public List<String> getTextDetectionInfoAll(){
		return textDetectionInfoAll;
	}


}
