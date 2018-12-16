package com.zhouqing.chatproject.realtimeindoorlocation.model;

/**
 * Created by DELL on 2018/1/16.
 */

public class StandardLocationInfo {
    private double x;
    private double y;

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public StandardLocationInfo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "("+x+","+y+")";
    }
}
