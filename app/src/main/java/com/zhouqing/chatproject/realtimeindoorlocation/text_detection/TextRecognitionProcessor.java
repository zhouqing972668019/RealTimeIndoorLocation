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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

	public TextRecognitionProcessor() {
		detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
	}



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

		List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();

		for (int i = 0; i < blocks.size(); i++) {
			List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
			for (int j = 0; j < lines.size(); j++) {
				List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
				for (int k = 0; k < elements.size(); k++) {

					//打印文本信息
					FirebaseVisionText.Element text = elements.get(k);
					RectF rect = new RectF(text.getBoundingBox());

					GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
					graphicOverlay.add(textGraphic);

					String textDetectionInfo = timeStamp+" "+rect.left+" "+rect.top+" "+rect.right+" "+rect.bottom+" "+text.getText();
					Log.d(TAG, textDetectionInfo);
					textDetectionInfoAll.add(textDetectionInfo);
				}
			}
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
