package com.zhouqing.chatproject.realtimeindoorlocation.model;

public class TextDetectionInfo {
    public long timeStamp;
    public double left;
    public double top;
    public double right;
    public double bottom;
    public String textContent;

    public TextDetectionInfo(long timeStamp, double left, double top, double right, double bottom, String textContent) {
        this.timeStamp = timeStamp;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.textContent = textContent;
    }

    @Override
    public String toString() {
        String result = "timeStamp:"+timeStamp+",("+left+","+top+"),("+right+","+bottom+"),"+textContent;
        return result;
    }
}
