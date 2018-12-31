package com.zhouqing.chatproject.realtimeindoorlocation.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CanvasView extends LinearLayout {
    public static class Coordinate{
        float x;
        float y;
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
    Context mContext;
    public CanvasView(Context context) {
        super(context);
        mContext = context;
        setWillNotDraw(false);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWillNotDraw(false);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setWillNotDraw(false);
    }

    // TODO: Canvas绘图
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //获取平面图中商店信息
        Map<String,List<Coordinate>> locationMap = new LinkedHashMap<>();
        Map<String,List<Coordinate>> shapeMap = new LinkedHashMap<>();
        readShopInfo(locationMap,shapeMap);
        //获取定位结果
        float answerX = FileUtil.getSPFloat(mContext,"answerX");
        float answerY = FileUtil.getSPFloat(mContext,"answerY");
        Coordinate locAnswer = null;
        if(answerX != 0f || answerY != 0f){
            locAnswer = new Coordinate(answerX,answerY);
        }
        //将读取的平面图坐标映射到当前的绘图空间中
        coordinateMapping(locationMap,shapeMap,canvas.getWidth(),canvas.getHeight(),locAnswer);
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.FILL);
        //绘制商店门信息
        for(String shopName: locationMap.keySet()){
            List<Coordinate> coordinates = locationMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                canvas.drawCircle(coordinate.x, coordinate.y, 5, p);
            }
        }
        //绘制商店的形状信息
        p.setColor(Color.BLUE);
        p.setStyle(Paint.Style.STROKE);
        for(String shopName: shapeMap.keySet()){
            List<Coordinate> coordinates = shapeMap.get(shopName);
            Path path = new Path();
            for(int i=0;i<coordinates.size()-1;i++){
                Coordinate coordinate = coordinates.get(i);
                if(i == 0){
                    // 此点为多边形的起点
                    path.moveTo(coordinates.get(i).x, coordinates.get(i).y);
                }
                else{
                    path.lineTo(coordinate.x,coordinate.y);
                }
            }
            // 使这些点构成封闭的多边形
            path.close();
            canvas.drawPath(path, p);
            //绘制商店名称
            p.setTextSize(20);
            Coordinate centerLocation = getShopCenterLocation(coordinates);
            canvas.drawText(shopName, centerLocation.x, centerLocation.y, p);
        }
    }

    /**
     * 读取商店信息，包括商店门的位置以及形状信息
     * @param shopNameLocationMap
     * @param shopNameShapeMap
     */
    public void readShopInfo(Map<String,List<Coordinate>> shopNameLocationMap,
                             Map<String,List<Coordinate>> shopNameShapeMap){

        int position = FileUtil.getSPInt(mContext,"shopSelection");
        FileUtil.loadShopInfoAsList(mContext,position,shopNameLocationMap,shopNameShapeMap);
        //System.out.println("locationMap:"+shopNameLocationMap.toString());
        //System.out.println("shapeMap:"+shopNameShapeMap.toString());
    }

    public void coordinateMapping(Map<String,List<Coordinate>> locationMap,
                                  Map<String,List<Coordinate>> shapeMap,
                                  int canvasWidth,int canvasHeight,Coordinate locAnswer){
        int width = canvasWidth;
        int height = canvasHeight;
        System.out.println("canvas width:"+width+",canvas height:"+height);
        //寻找x和y坐标中的最大最小值
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for(String shopName: shapeMap.keySet()){
            List<Coordinate> coordinates = shapeMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                if(coordinate.x > maxX){
                    maxX = coordinate.x;
                }
                else if(coordinate.x < minX){
                    minX = coordinate.x;
                }
                if(coordinate.y > maxY){
                    maxY = coordinate.y;
                }
                else if(coordinate.y < minY){
                    minY = coordinate.y;
                }
            }
        }
        for(String shopName: locationMap.keySet()){
            List<Coordinate> coordinates = locationMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                if(coordinate.x > maxX){
                    maxX = coordinate.x;
                }
                else if(coordinate.x < minX){
                    minX = coordinate.x;
                }
                if(coordinate.y > maxY){
                    maxY = coordinate.y;
                }
                else if(coordinate.y < minY){
                    minY = coordinate.y;
                }
            }
        }
        //将每一个坐标映射到0到max-min之间
        float xValue = minX;
        float yValue = minY;
        minX = 0;
        maxX -= xValue;
        minY = 0;
        maxY -= yValue;
        System.out.println("maxX:"+maxX+",maxY:"+maxY);
        for(String shopName: shapeMap.keySet()){
            List<Coordinate> coordinates = shapeMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                coordinate.x -= xValue;
                coordinate.y -= yValue;
            }
        }
        for(String shopName: locationMap.keySet()){
            List<Coordinate> coordinates = locationMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                coordinate.x -= xValue;
                coordinate.y -= yValue;
            }
        }
        locAnswer.x -= xValue;
        locAnswer.y -= yValue;
//        System.out.println("locationMap:"+locationMap.toString());
//        System.out.println("shapeMap:"+shapeMap.toString());

        //将平面图坐标范围压缩到当前绘图空间中（除去边界）
        width -= Constant.MARGIN * 2;
        height -= Constant.MARGIN * 2;
        double canvasRate = (double)width / (double)height;
        double floorPlanRate = (double)maxX / (double)maxY;
        double scaleRate = (double)width / (double)maxX;
        if(canvasRate>floorPlanRate){
            scaleRate = (double)height / (double)maxY;
        }
        for(String shopName: shapeMap.keySet()){
            List<Coordinate> coordinates = shapeMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                coordinate.x = (int)(coordinate.x * scaleRate + Constant.MARGIN);
                coordinate.y = canvasHeight - (int)(coordinate.y * scaleRate + Constant.MARGIN);
            }
        }
        for(String shopName: locationMap.keySet()){
            List<Coordinate> coordinates = locationMap.get(shopName);
            for(Coordinate coordinate:coordinates){
                coordinate.x = (int)(coordinate.x * scaleRate + Constant.MARGIN);
                coordinate.y = canvasHeight - (int)(coordinate.y * scaleRate + Constant.MARGIN);
            }
        }
        locAnswer.x = (int)(locAnswer.x * scaleRate + Constant.MARGIN);
        locAnswer.y = canvasHeight - (int)(locAnswer.y * scaleRate + Constant.MARGIN);

        System.out.println("locationMap:"+locationMap.toString());
        System.out.println("shapeMap:"+shapeMap.toString());
    }

    //寻找商店区域中心位置，用于填充文本
    public Coordinate getShopCenterLocation(List<Coordinate> coordinates){
        Coordinate centerLocation = new Coordinate();
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for(Coordinate coordinate:coordinates){
            if(coordinate.x > maxX){
                maxX = coordinate.x;
            }
            else if(coordinate.x < minX){
                minX = coordinate.x;
            }
            if(coordinate.y > maxY){
                maxY = coordinate.y;
            }
            else if(coordinate.y < minY){
                minY = coordinate.y;
            }
        }
        centerLocation.x = (maxX + 7 * minX) / 8;
        centerLocation.y = (maxY + minY) / 2;
        return centerLocation;
    }

}
