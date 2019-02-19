package com.zhouqing.chatproject.realtimeindoorlocation.util;

import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionAndPoi;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
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
                Double angle = Double.parseDouble(elements[2]);
                if(angle < 0d){
                    angle += 360d;
                }
                gyroOriMap.put(elements[1],angle);
            }
            else if(elements[0].equals(MAG_ACC_ORI)){
                Double angle = Double.parseDouble(elements[2]);
                if(angle < 0d){
                    angle += 360d;
                }
                magAccOriMap.put(elements[1],angle);
            }
        }
    }

    //通过陀螺仪数据计算某两个时间戳之间的3轴旋转角度
    public static double calOriByGyro(List<String> sensorInfoList,String timeStampPre,String timeStampCur){
        double answer = 0d;
        List<Double> gyro_x = new ArrayList<>();
        List<Double> gyro_y = new ArrayList<>();
        List<Double> gyro_z = new ArrayList<>();
        List<Long> timestampList = new ArrayList<>();
        long timeStampPreL = Long.parseLong(timeStampPre);
        long timeStampCurL = Long.parseLong(timeStampCur);

        for(String sensorInfo:sensorInfoList)
        {
            String[] elements=sensorInfo.split(" ");
            //相应传感器数据
            if(elements[0].equals("gyro"))
            {
                long timeStampL = Long.parseLong(elements[1]);
                if(timeStampL>=timeStampPreL && timeStampL<=timeStampCurL){
                    gyro_x.add(Double.parseDouble(elements[2]));
                    gyro_y.add(Double.parseDouble(elements[3]));
                    gyro_z.add(Double.parseDouble(elements[4]));
                    timestampList.add(timeStampL);
                }
            }
        }
        //设置初值
        //通过保存的两个时间戳内的陀螺仪数据积分计算出角度
        for(int i=0;i<timestampList.size()-1;i++){
            Double timeInterval = (timestampList.get(i+1) - timestampList.get(i))/1000.0;
            double gyro_Synthesis = Math.sqrt(gyro_x.get(i)*gyro_x.get(i) + gyro_y.get(i)*gyro_y.get(i) + gyro_z.get(i)*gyro_z.get(i));
            answer += timeInterval * Math.toDegrees(gyro_Synthesis);
        }
        //close
        return answer;
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

    //获取文字识别信息--考虑同一名称POI出现多次的情况
//    public static void getTextDetectionInfo(int previewWidth, Map<String, StandardLocationInfo> floorPlanMap, List<String> textDetectionList, Map<String, TextDetectionAndPoi> textDetectionInfoMap, Map<String, Integer> POIDetectionNumMap){
//        boolean isFindPOI = false;
//        String lastPOIName = null;
//        String lastTimeStamp = null;
//        for(String textDetection:textDetectionList){
//            String[] elements = textDetection.split(" ");
//            if(elements.length != 6){
//                continue;
//            }
//            //判断当前文字识别信息是否与某个POI名称相同
//            for(String POIName:floorPlanMap.keySet()){
//                if(Constant.calculateStringDistance(POIName,elements[5])>SIMILARITY_THRESHOLD){
//                    double left = Double.parseDouble(elements[1]);
//                    double right = Double.parseDouble(elements[3]);
//                    String timeStamp = elements[0];
//                    double centerDis = calCenterDis(previewWidth, left, right);
//                    //第一个识别到的POI
//                    if(!isFindPOI && lastPOIName == null){
//                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
//                        textDetectionAndPoi.timeStampList = new ArrayList<>();
//                        textDetectionAndPoi.timeStampList.add(timeStamp);
//                        textDetectionAndPoi.timeStamp = timeStamp;
//                        textDetectionAndPoi.centerDis = centerDis;
//                        isFindPOI = true;
//                        lastPOIName = POIName;
//                        lastTimeStamp = timeStamp;
//                        textDetectionInfoMap.put(POIName,textDetectionAndPoi);
//                        //存入POI数量hash表中
//                        POIDetectionNumMap.put(POIName,1);
//                    }
//                    //连续识别时，识别到的POI与上一个相同
//                    else if(isFindPOI && lastPOIName.equals(POIName)){
//                        String realPOIName = POIName;
//                        int i=0;
//                        for(;;i++){
//                            if(!textDetectionInfoMap.containsKey(POIName+i)){
//                                break;
//                            }
//                            realPOIName = POIName+i;
//                        }
//                        TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(realPOIName);
//                        textDetectionAndPoi.timeStampList.add(timeStamp);
//                        if(centerDis < textDetectionAndPoi.centerDis){
//                            textDetectionAndPoi.timeStamp = timeStamp;
//                            textDetectionAndPoi.centerDis = centerDis;
//                        }
//                        lastTimeStamp = timeStamp;
//                    }
//                    //识别到的POI与上一个不同
//                    else if(!lastPOIName.equals(POIName)){
//                        //同一个POI名称出现多次
//                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
//                        textDetectionAndPoi.timeStampList = new ArrayList<>();
//                        textDetectionAndPoi.timeStampList.add(timeStamp);
//                        textDetectionAndPoi.timeStamp = timeStamp;
//                        textDetectionAndPoi.centerDis = centerDis;
//                        if(textDetectionInfoMap.containsKey(POIName)){
//                            for(int i=0;;i++){
//                                if(!textDetectionInfoMap.containsKey(POIName+i)){
//                                    textDetectionInfoMap.put(POIName+i,textDetectionAndPoi);
//                                    break;
//                                }
//                            }
//                            POIDetectionNumMap.put(POIName,POIDetectionNumMap.get(POIName)+1);
//                        }
//                        //同一个POI名称出现一次
//                        else{
//                            textDetectionInfoMap.put(POIName,textDetectionAndPoi);
//                            POIDetectionNumMap.put(POIName,1);
//                        }
//                        isFindPOI = true;
//                        lastPOIName = POIName;
//                        lastTimeStamp = timeStamp;
//                    }
//                    //非连续识别时，识别到的POI与上一个相同
//                    else{
//                        //同一个POI
//                        if(isSamePOI(lastTimeStamp,timeStamp)){
//                            String realPOIName = POIName;
//                            int i=0;
//                            for(;;i++){
//                                if(!textDetectionInfoMap.containsKey(POIName+i)){
//                                    break;
//                                }
//                                realPOIName = POIName+i;
//                            }
//                            TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(realPOIName);
//                            textDetectionAndPoi.timeStampList.add(timeStamp);
//                            if(centerDis < textDetectionAndPoi.centerDis){
//                                textDetectionAndPoi.timeStamp = timeStamp;
//                                textDetectionAndPoi.centerDis = centerDis;
//                            }
//                            lastTimeStamp = timeStamp;
//                        }
//                        //非同一个POI
//                        else{
//                            //同一个POI名称出现多次
//                            TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
//                            textDetectionAndPoi.timeStampList = new ArrayList<>();
//                            textDetectionAndPoi.timeStampList.add(timeStamp);
//                            textDetectionAndPoi.timeStamp = timeStamp;
//                            textDetectionAndPoi.centerDis = centerDis;
//                            if(textDetectionInfoMap.containsKey(POIName)){
//                                for(int i=0;;i++){
//                                    if(!textDetectionInfoMap.containsKey(POIName+i)){
//                                        textDetectionInfoMap.put(POIName+i,textDetectionAndPoi);
//                                        break;
//                                    }
//                                }
//                                POIDetectionNumMap.put(POIName,POIDetectionNumMap.get(POIName)+1);
//                            }
//                            //同一个POI名称出现一次
//                            else{
//                                textDetectionInfoMap.put(POIName,textDetectionAndPoi);
//                                POIDetectionNumMap.put(POIName,1);
//                            }
//                            isFindPOI = true;
//                            lastPOIName = POIName;
//                            lastTimeStamp = timeStamp;
//                        }
//                    }
//                    break;
//                }
//            }
//        }
//    }

    //对识别的文本内容进行分析 合并左右相邻的文本
    public static void processTextDetectionInfo(List<String> textDetectionList,List<TextDetectionInfo> resultList){
        //System.out.println("process before:"+textDetectionList.toString());
        List<TextDetectionInfo> textDetectionInfoList = new ArrayList<>();
        for(String textDetectionLine:textDetectionList){
            String[] elements = textDetectionLine.split(" ");
            if(elements.length != 6){
                continue;
            }
            TextDetectionInfo textDetectionInfo = new TextDetectionInfo(Long.parseLong(elements[0]),
                    Double.parseDouble(elements[1]),Double.parseDouble(elements[2]),Double.parseDouble(elements[3]),
                    Double.parseDouble(elements[4]),elements[5]);
            textDetectionInfoList.add(textDetectionInfo);
        }
        //重新构造文本识别内容列表
        for(int i=0;i<textDetectionInfoList.size();i++){
            TextDetectionInfo curInfo = textDetectionInfoList.get(i);
            if(i == textDetectionInfoList.size() - 1){
                resultList.add(curInfo);
                break;
            }
            TextDetectionInfo nextInfo = textDetectionInfoList.get(i+1);
            if(curInfo.timeStamp != nextInfo.timeStamp){
                resultList.add(curInfo);
            }
            else{
                if(isAdjacent(curInfo,nextInfo)){
                    nextInfo.textContent = curInfo.textContent + nextInfo.textContent;
                    nextInfo.left = curInfo.left;
                    nextInfo.top = curInfo.top;
                }
                else{
                    resultList.add(curInfo);
                }
            }
        }
        //复制处理后结果
//        textDetectionList.clear();
//        for(TextDetectionInfo info:resultList){
//            String line = info.timeStamp+" "+info.left+" "+info.top+" "+info.right+" "+info.bottom+" "+info.textContent;
//            textDetectionList.add(line);
//        }
        //System.out.println("process after:"+textDetectionList.toString());
    }

    //判断两个文本是否相邻
    public static boolean isAdjacent(TextDetectionInfo curInfo,TextDetectionInfo nextInfo){
        if(Math.abs(curInfo.top - nextInfo.top) >= Constant.ADJACENT_THRESHOLD){
            return false;
        }
        if(curInfo.left > nextInfo.left){
            TextDetectionInfo t = curInfo;
            curInfo = nextInfo;
            nextInfo = t;
        }
        double singleDis = (curInfo.right - curInfo.left)/curInfo.textContent.length();
        return (nextInfo.left - curInfo.right) <= singleDis;
    }

    //获取文字识别信息--只考虑出现一次的情况
    public static void getTextDetectionInfo(int previewWidth, Map<String, StandardLocationInfo> floorPlanMap, List<String> textDetectionList, Map<String, TextDetectionAndPoi> textDetectionInfoMap, Map<String, Integer> POIDetectionNumMap){
        List<TextDetectionInfo> resultList = new ArrayList<>();
        processTextDetectionInfo(textDetectionList,resultList);
        //建立时间戳与文本的hash表
        Map<Long,List<TextDetectionInfo>> resultMap = new LinkedHashMap<>();
        for(int i=0;i<resultList.size();i++){
            long timeStamp = resultList.get(i).timeStamp;
            if(!resultMap.containsKey(timeStamp)){
                List<TextDetectionInfo> textDetectionInfoList = new ArrayList<>();
                textDetectionInfoList.add(resultList.get(i));
                resultMap.put(timeStamp,textDetectionInfoList);
            }
            else{
                List<TextDetectionInfo> textDetectionInfoList = resultMap.get(timeStamp);
                textDetectionInfoList.add(resultList.get(i));
            }
        }
        /*
            遍历每个时间戳下所有识别的文本内容
            规则1：每个文本只能被识别为1个POI
            规则2：每个时间戳下只保留面积最大的POI
            规则3：只保留中心距离相机中心距离在某个阈值内的POI
         */
        for(Long timeStamp:resultMap.keySet()){
            List<TextDetectionInfo> textDetectionInfoList = resultMap.get(timeStamp);
            TextDetectionInfo resultTextDetectionInfo = null;
            double maxArea = Integer.MIN_VALUE;
            String resultPOIName = "";
            for(TextDetectionInfo textDetectionInfo:textDetectionInfoList){
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
                    double area = calcalateArea(textDetectionInfo);
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
                double centerDis = calCenterDis(previewWidth, left, right);
                //小于阈值的文本框进行判断 减少误判
                if(centerDis < Constant.CENTOR_DIS_THRESHOLD){
                    if (!textDetectionInfoMap.containsKey(resultPOIName)) {
                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
                        textDetectionAndPoi.timeStampList = new ArrayList<>();
                        textDetectionAndPoi.timeStampList.add(timeStamp+"");
                        textDetectionAndPoi.timeStamp = timeStamp+"";
                        textDetectionAndPoi.centerDis = centerDis;
                        textDetectionInfoMap.put(resultPOIName, textDetectionAndPoi);
                        //存入POI数量hash表中
                        POIDetectionNumMap.put(resultPOIName, 1);
                    } else {
                        TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(resultPOIName);
                        textDetectionAndPoi.timeStampList.add(timeStamp+"");
                        if (centerDis < textDetectionAndPoi.centerDis) {
                            textDetectionAndPoi.timeStamp = timeStamp+"";
                            textDetectionAndPoi.centerDis = centerDis;
                        }
                    }
                }
            }

        }

//        for(String textDetection:textDetectionList){
//            String[] elements = textDetection.split(" ");
//            if(elements.length != 6){
//                continue;
//            }
//            String POIName = "";
//            int similarity = Integer.MIN_VALUE;
//            //判断当前文字识别信息是否与某个POI名称相同(找出相似度最高的POI)
//            for(String floorPlanPOIName:floorPlanMap.keySet()){
//                String modifyFloorPlanPOIName = Constant.removeIllegalAlphabet(floorPlanPOIName);
//                String modifyTextDetection = Constant.removeIllegalAlphabet(elements[5]);
//                int value = Constant.calculateStringDistance(modifyFloorPlanPOIName,modifyTextDetection);
//                if(value > similarity){
//                    similarity = value;
//                    POIName = floorPlanPOIName;
//                }
//            }
//            if(similarity > SIMILARITY_THRESHOLD) {
//                double left = Double.parseDouble(elements[1]);
//                double right = Double.parseDouble(elements[3]);
//                double top = Double.parseDouble(elements[2]);
//                double bottom = Double.parseDouble(elements[4]);
//                String timeStamp = elements[0];
//                double centerDis = calCenterDis(previewWidth, left, right);
//                //小于阈值的文本框进行判断 减少误判
//                if(centerDis < Constant.CENTOR_DIS_THRESHOLD){
//                    if (!textDetectionInfoMap.containsKey(POIName)) {
//                        TextDetectionAndPoi textDetectionAndPoi = new TextDetectionAndPoi();
//                        textDetectionAndPoi.timeStampList = new ArrayList<>();
//                        textDetectionAndPoi.timeStampList.add(timeStamp);
//                        textDetectionAndPoi.timeStamp = timeStamp;
//                        textDetectionAndPoi.centerDis = centerDis;
//                        textDetectionInfoMap.put(POIName, textDetectionAndPoi);
//                        //存入POI数量hash表中
//                        POIDetectionNumMap.put(POIName, 1);
//                    } else {
//                        TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(POIName);
//                        textDetectionAndPoi.timeStampList.add(timeStamp);
//                        if (centerDis < textDetectionAndPoi.centerDis) {
//                            textDetectionAndPoi.timeStamp = timeStamp;
//                            textDetectionAndPoi.centerDis = centerDis;
//                        }
//                    }
//                }
//            }
//        }
    }
    //计算文本识别框的面积
    public static double calcalateArea(TextDetectionInfo textDetectionInfo){
        double left = textDetectionInfo.left;
        double right = textDetectionInfo.right;
        double top = textDetectionInfo.top;
        double bottom = textDetectionInfo.bottom;
        return (right - left)*(bottom - top);
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
                return true;
            }
        }
        return false;
    }

    //获取两两POI之间的夹角
    public static void getAngleOfPOIs(Map<String, TextDetectionAndPoi> textDetectionInfoMap, List<Double> angleList, List<String> POINameList,
                                      List<Double> gyroAngleList, List<Double> magAccAngleList,List<Double> complexGyroAngleList,
                                      List<String> sensorInfoList){
        List<Map.Entry<String,TextDetectionAndPoi>> calculateList = new ArrayList<>(textDetectionInfoMap.entrySet());
        for(int i=1;i<calculateList.size();i++){
            POINameList.add(calculateList.get(i-1).getKey()+"->"+calculateList.get(i).getKey());
            //方向传感器夹角
            double angle = calculateList.get(i).getValue().ori_angle - calculateList.get(i-1).getValue().ori_angle;
            if(angle<0){
                angle += 360;
            }
            angleList.add(angle);
            //陀螺仪夹角
            double lastAngle = calculateList.get(i).getValue().gyro_ori_angle;
            if(lastAngle<0)lastAngle += 360;
            double firstAngle = calculateList.get(i-1).getValue().gyro_ori_angle;
            if(firstAngle<0)firstAngle += 360;
            double gyroAngle = lastAngle - firstAngle;
            if(gyroAngle<0)gyroAngle += 360;
            gyroAngleList.add(gyroAngle);
            //磁场+重力夹角
            lastAngle = calculateList.get(i).getValue().mag_acc_angle;
            if(lastAngle<0)lastAngle += 360;
            firstAngle = calculateList.get(i-1).getValue().mag_acc_angle;
            if(firstAngle<0)firstAngle += 360;
            double magAccAngle = lastAngle - firstAngle;
            if(magAccAngle<0)magAccAngle += 360;
            magAccAngleList.add(magAccAngle);
            //合成的陀螺仪角度
            double complexGyroAngle = calOriByGyro(sensorInfoList,calculateList.get(i-1).getValue().timeStamp,
                    calculateList.get(i).getValue().timeStamp);
            complexGyroAngleList.add(complexGyroAngle);
        }
    }

    //获取已识别的角标信息
    public static void getCoordinateList(Map<String, TextDetectionAndPoi> textDetectionInfoMap,
                                         Map<String, StandardLocationInfo> floorPlanMap,
                                         List<Double[]> coordinateList,List<Double[]> gyro_coordinateList,
                                         List<Double[]> mag_acc_coordinateList,List<Double> complexGyroAngleList,
                                         List<Double[]> complex_gyro_coordinateList)
    {
        int index = -1;
        double preComplexAngle = 0d;
        for (String POIName:textDetectionInfoMap.keySet()) {
            Double[] coordinate = new Double[3];
            coordinate[0] = floorPlanMap.get(POIName).getX();
            coordinate[1] = floorPlanMap.get(POIName).getY();
            coordinate[2] = textDetectionInfoMap.get(POIName).ori_angle;
            coordinateList.add(coordinate);
            //System.out.println("coordinate：x=" + coordinate[0] + " y=" + coordinate[1]);

            Double[] gyro_coordinate = new Double[3];
            gyro_coordinate[0] = floorPlanMap.get(POIName).getX();
            gyro_coordinate[1] = floorPlanMap.get(POIName).getY();
            gyro_coordinate[2] = textDetectionInfoMap.get(POIName).gyro_ori_angle;
            gyro_coordinateList.add(gyro_coordinate);

            Double[] mag_acc_coordinate = new Double[3];
            mag_acc_coordinate[0] = floorPlanMap.get(POIName).getX();
            mag_acc_coordinate[1] = floorPlanMap.get(POIName).getY();
            mag_acc_coordinate[2] = textDetectionInfoMap.get(POIName).mag_acc_angle;
            mag_acc_coordinateList.add(gyro_coordinate);

            Double[] complex_gyro_coordinate = new Double[3];
            complex_gyro_coordinate[0] = floorPlanMap.get(POIName).getX();
            complex_gyro_coordinate[1] = floorPlanMap.get(POIName).getY();
            if(index == -1){
                complex_gyro_coordinate[2] = textDetectionInfoMap.get(POIName).ori_angle;
            }
            else{
                complex_gyro_coordinate[2] = preComplexAngle + complexGyroAngleList.get(index);
            }
            preComplexAngle = complex_gyro_coordinate[2];
            index++;
            complex_gyro_coordinateList.add(complex_gyro_coordinate);
        }
    }

    /**
     * 通过定位结果计算x轴正方向对应的基准角
     * @param textDetectionInfoMap
     * @param floorPlanMap
     * @param answer
     * @return
     */
    public static float getStartAngle(Map<String, TextDetectionAndPoi> textDetectionInfoMap,
                                      Map<String, StandardLocationInfo> floorPlanMap,
                                      Double[] answer){
        double angle = AngleCalculationUtil.getStartAngleAll(textDetectionInfoMap,floorPlanMap,answer,true);
        return (float)angle;
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

    //构造结果输出字符串 并保存到文件
    public static void getResultPrintContent(StringBuilder resultSB, Map<String, TextDetectionAndPoi> textDetectionInfoMap, Map<String, StandardLocationInfo> floorPlanMap){
        resultSB.append("floorPlanMap:\n" + floorPlanMap.toString());
        resultSB.append("Text Detection Result:\n");
        for(String POIName:textDetectionInfoMap.keySet()){
            TextDetectionAndPoi textDetectionAndPoi = textDetectionInfoMap.get(POIName);
            resultSB.append("POIName:"+POIName+",timeStamp:"+textDetectionAndPoi.timeStamp
                    +",ori:"+textDetectionAndPoi.ori_angle+",mag_acc:"+textDetectionAndPoi.mag_acc_angle
                    +",gyro:"+textDetectionAndPoi.gyro_ori_angle+"\n");
        }
    }

    //进一步构造结果输出字符串 保存定位结果
    public static void getResultPrintContentFinal(StringBuilder resultSB, Double[] answer, Double[] gyro_answer,
                                                  Double[] mag_acc_answer,Double[] complex_gyro_answer,List<String> POINameList,
                                                  List<Double> angleList,List<Double> gyroAngleList,
                                                  List<Double> magAccAngleList,List<Double> complexGyroAngleList){
        resultSB.append("Location Info:\n");
        for(int i=0;i<POINameList.size();i++){
            resultSB.append(POINameList.get(i)+":(ori)"+angleList.get(i)+",(gyro)"+
                    gyroAngleList.get(i)+",(mag_acc)"+magAccAngleList.get(i)+",(complex_gyro)"+
                    complexGyroAngleList.get(i)+"\n");
        }
        resultSB.append("Location Result:\n");
        resultSB.append("ori:("+answer[0]+","+answer[1]+")"+"\n");
        resultSB.append("gyro_answer:("+gyro_answer[0]+","+gyro_answer[1]+")"+"\n");
        resultSB.append("mag_acc_answer:("+mag_acc_answer[0]+","+mag_acc_answer[1]+")"+"\n");
        resultSB.append("complex_gyro_answer:("+complex_gyro_answer[0]+","+complex_gyro_answer[1]+")"+"\n");
    }


    //转换文字识别结果list到字符串
    public static String getStrByTextDetectionInfoList(List<String> textDetectionInfoList){
        StringBuilder sb = new StringBuilder();
        for(String textDetection:textDetectionInfoList){
            sb.append(textDetection).append("\n");
        }
        return sb.toString();
    }

    //转换传感器信息list到字符串
    public static String getStrBySensorInfoList(List<String> sensorInfoList){
        StringBuilder sb = new StringBuilder();
        for(String sensorInfo:sensorInfoList){
            sb.append(sensorInfo).append("\n");
        }
        return sb.toString();
    }

    //获取最新的采集数据
    public static void getRecentCollectionData(List<String> sensorInfoList,List<String> textDetectionInfoList) throws ParseException {
        List<String> folderList = FileUtil.getChildFolder(Constant.COLLECTION_DATA_PATH);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String resultFolder = folderList.get(0);
        Date resultDate = df.parse(resultFolder);
        if(folderList.size() > 1){
            for(int i=1;i<folderList.size();i++){
                String folder = folderList.get(i);
                Date date = df.parse(folder);
                if(date.after(resultDate)){
                    resultDate = date;
                    resultFolder = folder;
                }
            }
        }
        FileUtil.readFileToGetCollectionData(Constant.COLLECTION_DATA_PATH + resultFolder + "/",sensorInfoList,textDetectionInfoList);
    }

    //获取当前文件夹下的所有子文件夹，按时间递减顺序返回
    public static String[] getFoldersByTimeDesc(){
        List<String> folderList = FileUtil.getChildFolder(Constant.COLLECTION_DATA_PATH);
        Collections.sort(folderList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] elements1 = o1.split("_");
                String[] elements2 = o2.split("_");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                try {
                    Date date1 = df.parse(elements1[1]);
                    Date date2 = df.parse(elements2[1]);
                    if(date1.after(date2)){
                        return 1;
                    }
                    else if(date1.before(date2)){
                        return -1;
                    }
                    else{
                        return 0;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        return folderList.toArray(new String[folderList.size()]);
    }

    //获取最新的采集数据
    public static void getTargetCollectionData(List<String> sensorInfoList,List<String> textDetectionInfoList,String folderName) throws ParseException {
        FileUtil.readFileToGetCollectionData(Constant.COLLECTION_DATA_PATH + folderName + "/",sensorInfoList,textDetectionInfoList);
    }

}
