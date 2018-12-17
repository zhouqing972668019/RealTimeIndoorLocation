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

    //读取某个时间戳前后的传感器数据
    public static double getOriByTimeStamp(Map<String,Double> oriMap,String timeStamp){
        double angle = 0d;
        for(String curTimeStamp:oriMap.keySet())
        {
            long findTime = Long.parseLong(timeStamp);
            long curTime = Long.parseLong(curTimeStamp);
            if(findTime <= curTime)
            {
                angle = (oriMap.get(curTimeStamp) + angle)/2.0;
                break;
            }
            angle = oriMap.get(curTimeStamp);
        }
        return angle;
    }

    //获取文字识别信息
    public static void getTextDetectionInfo(Size previewSize, Map<String, StandardLocationInfo> floorPlanMap, List<String> textDetectionList, Map<String, TextDetectionAndPoi> textDetectionInfoMap, Map<String, Integer> POIDetectionNumMap){
        //手机为竖屏拍摄 宽度小于高度
        int previewWidth = previewSize.getHeight();
        int previewHeight = previewSize.getWidth();
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
                        //存入POI数量hash表中
                        POIDetectionNumMap.put(POIName,1);
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
                            POIDetectionNumMap.put(POIName,POIDetectionNumMap.get(POIName)+1);
                        }
                        //同一个POI名称出现一次
                        else{
                            textDetectionInfoMap.put(POIName,textDetectionAndPoi);
                            POIDetectionNumMap.put(POIName,1);
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
                                POIDetectionNumMap.put(POIName,POIDetectionNumMap.get(POIName)+1);
                            }
                            //同一个POI名称出现一次
                            else{
                                textDetectionInfoMap.put(POIName,textDetectionAndPoi);
                                POIDetectionNumMap.put(POIName,1);
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

    //判断map中是否有POI名称出现多于1次
    public static boolean isPOINumMoreThanOne(Map<String, Integer> POIDetectionNumMap){
        for(String POINum:POIDetectionNumMap.keySet()){
            if(POIDetectionNumMap.get(POINum) > 1){
                return false;
            }
        }
        return true;
    }

    //获取两两POI之间的夹角
    public static void getAngleOfPOIs(Map<String, TextDetectionAndPoi> textDetectionInfoMap, List<Double> angleList, List<String> POINameList){
        List<Map.Entry<String,TextDetectionAndPoi>> calculateList = new ArrayList<>(textDetectionInfoMap.entrySet());
        for(int i=1;i<calculateList.size();i++){
            double angle = calculateList.get(i).getValue().ori_angle - calculateList.get(i-1).getValue().ori_angle;
            if(angle<0){
                angle += 360;
            }
            angleList.add(angle);
            POINameList.add(calculateList.get(i-1).getKey()+"->"+calculateList.get(i).getKey());
        }
    }

    //获取已识别的角标信息
    public static void getCoordinateList(Map<String, TextDetectionAndPoi> textDetectionInfoMap,
                                         Map<String, StandardLocationInfo> floorPlanMap,
                                         List<Double[]> coordinateList)
    {
        for (String POIName:textDetectionInfoMap.keySet()) {
            Double[] coordinate = new Double[3];
            coordinate[0] = floorPlanMap.get(POIName).getX();
            coordinate[1] = floorPlanMap.get(POIName).getY();
            coordinate[2] = textDetectionInfoMap.get(POIName).ori_angle;
            coordinateList.add(coordinate);
            System.out.println("coordinate：x=" + coordinate[0] + " y=" + coordinate[1]);
        }
    }

    //获取定位结果 返回给上一个界面
    public static void getLocationResult(StringBuilder showInfo, Double[] answer, Map<String, TextDetectionAndPoi> textDetectionInfoMap, List<String> POINameList, List<Double> angleList){
        showInfo.append("location answer:(").append(answer[0]).append(",").append(answer[1]).append(")\n");
        showInfo.append("Intermediate information:\n");
        for(String POIName:textDetectionInfoMap.keySet()){
            TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(POIName);
            showInfo.append("POIName:"+POIName+",angel:"+textDetectionAndPoi.ori_angle+"\n");
        }
        for(int i=0;i<angleList.size();i++){
            showInfo.append(POINameList.get(i)+":"+angleList.get(i)+"\n");
        }
    }
}
