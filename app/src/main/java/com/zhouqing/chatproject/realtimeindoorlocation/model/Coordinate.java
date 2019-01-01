package com.zhouqing.chatproject.realtimeindoorlocation.model;

public class Coordinate {
    public float x;
    public float y;
    public Coordinate(float x,float y){
        this.x = x;
        this.y = y;
    }

    public Coordinate(){

    }

    @Override
    public String toString() {
        return "("+x+","+y+")";
    }
}
