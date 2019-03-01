package com.zhouqing.chatproject.realtimeindoorlocation.model;

public class Text {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public String content;

    public int pos;//-1 表示偏左，0表示合理，1表示偏右

    public Text(float left, float top, float right, float bottom, String content) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.content = content;
    }
}
