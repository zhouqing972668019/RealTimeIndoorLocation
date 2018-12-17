package com.zhouqing.chatproject.realtimeindoorlocation.model;

import java.util.List;

public class TextDetectionAndPoi {
    public double ori_angle;
    public double gyro_ori_angle;
    public double mag_acc_angle;
    public double centerDis;
    public String timeStamp;
    public List<String> timeStampList;

    @Override
    public String toString() {
        String info = "angle:"+ori_angle+","+gyro_ori_angle+","+mag_acc_angle
                +",centerDis:"+centerDis +",timeStamp:"+timeStamp+ ",timeStampList:"
                +timeStampList.toString();
        return info;
    }
}
