package com.zhouqing.chatproject.realtimeindoorlocation.util;

import com.google.android.gms.common.images.Size;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionAndPoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocationInfoUtil {
    public static final String ORI = "ori";
    public static final String GYRO_ORI = "gyro_ori";
    public static final String MAG_ACC_ORI = "mag_acc_ori";
    public static final int SIMILARITY_THRESHOLD = 50;
    public static final long TIMESTAMP_THRESHOLD = 1500;

    //获取相关方向角信息
    public static void getOriInfo(Map<String,Double> oriMap, Map<String,Double> gyroOriMap, Map<String,Double> magAccOriMap, List<String> sensorInfoList){
        for(String sensorInfo:sensorInfoList){
            String[] elements = sensorInfo.split(" ");
            if(elements.length != 5){
                continue;
            }
            if(elements[0].equals(ORI)){
                oriMap.put(elements[1],Double.parseDouble(elements[2]));
            }
            else if(elements[0].equals(GYRO_ORI)){
                gyroOriMap.put(elements[1],Double.parseDouble(elements[2]));
            }
            else if(elements[0].equals(MAG_ACC_ORI)){
                magAccOriMap.put(elements[1],Double.parseDouble(elements[2]));
            }
        }
    }

    //获取文字识别信息
    public static void getTextDetectionInfo(Size previewSize, Map<String, StandardLocationInfo> floorPlanMap, List<String> textDetectionList, Map<String, TextDetectionAndPoi> textDetectionInfoMap){
        int previewWidth = previewSize.getWidth();
        int previewHeight = previewSize.getHeight();
        boolean isFindPOI = false;
        String lastPOIName = null;
        String lastTimeStamp = null;
        for(String textDetection:textDetectionList){
            String[] elements = textDetection.split(" ");
            if(elements.length != 6){
                continue;
            }
            //判断当前文字识别信息是否与某个POI名称相同
            for(String POIName:floorPlanMap.keySet()){
                if(Constant.calculateStringDistance(POIName,elements[5])>SIMILARITY_THRESHOLD){
                    double left = Double.parseDouble(elements[1]);
                    double right = Double.parseDouble(elements[3]);
                    String timeStamp = elements[0];
                    double centerDis = calCenterDis(previewWidth, left, right);
                    //第一个识别到的POI
                    if(!isFindPOI && lastPOIName == null){
                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
                        textDetectionAndPoi.timeStampList = new ArrayList<>();
                        textDetectionAndPoi.timeStampList.add(timeStamp);
                        textDetectionAndPoi.timeStamp = timeStamp;
                        textDetectionAndPoi.centerDis = centerDis;
                        isFindPOI = true;
                        lastPOIName = POIName;
                        lastTimeStamp = timeStamp;
                        textDetectionInfoMap.put(POIName,textDetectionAndPoi);
                    }
                    //连续识别时，识别到的POI与上一个相同
                    else if(isFindPOI && lastPOIName.equals(POIName)){
                        String realPOIName = POIName;
                        int i=0;
                        for(;;i++){
                            if(!textDetectionInfoMap.containsKey(POIName+i)){
                                break;
                            }
                            realPOIName = POIName+i;
                        }
                        TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(realPOIName);
                        textDetectionAndPoi.timeStampList.add(timeStamp);
                        if(centerDis < textDetectionAndPoi.centerDis){
                            textDetectionAndPoi.timeStamp = timeStamp;
                            textDetectionAndPoi.centerDis = centerDis;
                        }
                        lastTimeStamp = timeStamp;
                    }
                    //识别到的POI与上一个不同
                    else if(!lastPOIName.equals(POIName)){
                        //同一个POI名称出现多次
                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
                        textDetectionAndPoi.timeStampList = new ArrayList<>();
                        textDetectionAndPoi.timeStampList.add(timeStamp);
                        textDetectionAndPoi.timeStamp = timeStamp;
                        textDetectionAndPoi.centerDis = centerDis;
                        if(textDetectionInfoMap.containsKey(POIName)){
                            for(int i=0;;i++){
                                if(!textDetectionInfoMap.containsKey(POIName+i)){
                                    textDetectionInfoMap.put(POIName+i,textDetectionAndPoi);
                                    break;
                                }
                            }
                        }
                        //同一个POI名称出现一次
                        else{
                            textDetectionInfoMap.put(POIName,textDetectionAndPoi);
                        }
                        isFindPOI = true;
                        lastPOIName = POIName;
                        lastTimeStamp = timeStamp;
                    }
                    //非连续识别时，识别到的POI与上一个相同
                    else{
                        //同一个POI
                        if(isSamePOI(lastTimeStamp,timeStamp)){
                            String realPOIName = POIName;
                            int i=0;
                            for(;;i++){
                                if(!textDetectionInfoMap.containsKey(POIName+i)){
                                    break;
                                }
                                realPOIName = POIName+i;
                            }
                            TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(realPOIName);
                            textDetectionAndPoi.timeStampList.add(timeStamp);
                            if(centerDis < textDetectionAndPoi.centerDis){
                                textDetectionAndPoi.timeStamp = timeStamp;
                                textDetectionAndPoi.centerDis = centerDis;
                            }
                            lastTimeStamp = timeStamp;
                        }
                        //非同一个POI
                        else{
                            //同一个POI名称出现多次
                            TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
                            textDetectionAndPoi.timeStampList = new ArrayList<>();
                            textDetectionAndPoi.timeStampList.add(timeStamp);
                            textDetectionAndPoi.timeStamp = timeStamp;
                            textDetectionAndPoi.centerDis = centerDis;
                            if(textDetectionInfoMap.containsKey(POIName)){
                                for(int i=0;;i++){
                                    if(!textDetectionInfoMap.containsKey(POIName+i)){
                                        textDetectionInfoMap.put(POIName+i,textDetectionAndPoi);
                                        break;
                                    }
                                }
                            }
                            //同一个POI名称出现一次
                            else{
                                textDetectionInfoMap.put(POIName,textDetectionAndPoi);
                            }
                            isFindPOI = true;
                            lastPOIName = POIName;
                            lastTimeStamp = timeStamp;
                        }
                    }
                    break;
                }
            }
        }
    }

    //计算文字识别框的中心与相机预览窗口中心的距离
    public static double calCenterDis(int previewWidth,double left,double right){
        double textDetectionCenter = (right + left)/2.0;
        return Math.abs(textDetectionCenter - (double)previewWidth/2.0);
    }

    //判断断开的两段POI是否是同一个POI
    public static boolean isSamePOI(String lastTimeStamp, String timeStamp){
        long last = Long.parseLong(lastTimeStamp);
        long current = Long.parseLong(timeStamp);
        return  current - last <= TIMESTAMP_THRESHOLD;
    }
}
