package com.zhouqing.chatproject.realtimeindoorlocation.model;

import java.util.List;

public class TextDetectionAndPoi {
    public double angle;
    public double centerDis;
    public String timeStamp;
    public List<String> timeStampList;

    @Override
    public String toString() {
        String info = "angle:"+angle+",centerDis:"+centerDis+",timeStamp:"+timeStamp+
                ",timeStampList:"+timeStampList.toString();
        return info;
    }
}
