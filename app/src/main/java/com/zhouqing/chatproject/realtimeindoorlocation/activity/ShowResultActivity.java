package com.zhouqing.chatproject.realtimeindoorlocation.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import com.zhouqing.chatproject.realtimeindoorlocation.R;
import com.zhouqing.chatproject.realtimeindoorlocation.model.Coordinate;
import com.zhouqing.chatproject.realtimeindoorlocation.util.Constant;
import com.zhouqing.chatproject.realtimeindoorlocation.util.FileUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShowResultActivity extends AppCompatActivity {

    private CanvasView canvasView;

    private SensorManager sensorManagerOrientationOld;
    private SensorEventListener listenerOrientationOld;
    //记录当前的角度
    float currentDegree;
    //基准角度
    float startAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_show_result);

        //存入定位结果与初始角度，用于测试
//        FileUtil.saveSpFloat(ShowResultActivity.this,"answerX",158.5f);
//        FileUtil.saveSpFloat(ShowResultActivity.this,"answerY",129.3f);
//        FileUtil.saveSpFloat(ShowResultActivity.this,"startAngle",185f);

        canvasView =new CanvasView(this);
        setContentView(canvasView);

        //1.获取传感器管理对象SensorManager
        sensorManagerOrientationOld = (SensorManager) getSystemService(SENSOR_SERVICE);
        //2.获取指定类型的传感器
        Sensor sensorOrientation = sensorManagerOrientationOld.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //3.注册监听,通过实时监听即可获取传感器传回来的数据
        listenerOrientationOld = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //绕Z轴旋转的角度，0度代表为正北
                //z是指向地心的方角，其角度是表指南针的方向，其中180表示正南方
                float zValue = Math.abs(event.values[0]);
                float xValue = Math.abs(event.values[2]);
                float yValue = Math.abs(event.values[1]);
                //转换角度
                currentDegree = zValue - startAngle < 0 ? zValue - startAngle + 360f : zValue - startAngle;
                canvasView.invalidate();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        //由于方向传感器的精确度要求通常都比较高,使用的是 SENSOR_DELAY_GAME
        sensorManagerOrientationOld.registerListener(listenerOrientationOld, sensorOrientation, SensorManager.SENSOR_DELAY_FASTEST);
    }

    //取消注册
    @Override
    protected void onPause() {
        super.onPause();
        sensorManagerOrientationOld.unregisterListener(listenerOrientationOld);
    }

    //取消注册
    @Override
    protected void onStop() {
        super.onStop();
        sensorManagerOrientationOld.unregisterListener(listenerOrientationOld);
    }


    class CanvasView extends View {
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
            //获取定位结果相关信息
            Coordinate locAnswer = null;
            float answerX = FileUtil.getSPFloat(ShowResultActivity.this,"answerX") * Constant.DECIMAL_FACTOR;
            float answerY = FileUtil.getSPFloat(ShowResultActivity.this,"answerY") * Constant.DECIMAL_FACTOR;
            if(answerX != 0f || answerY != 0f){
                locAnswer = new Coordinate(answerX,answerY);
            }
            startAngle = FileUtil.getSPFloat(ShowResultActivity.this,"startAngle");
            List<String> POINameList = FileUtil.getPOINames(mContext);

            if(locAnswer!=null) System.out.println("locAnswer initial:"+locAnswer);
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
            p.setStyle(Paint.Style.STROKE);
            for(String shopName: shapeMap.keySet()){
                if(POINameList != null && POINameList.size() != 0){
                    if(POINameList.contains(shopName)){
                        p.setColor(Color.RED);
                    }
                    else{
                        p.setColor(Color.BLUE);
                    }
                }
                else{
                    p.setColor(Color.BLUE);
                }
                List<Coordinate> coordinates = shapeMap.get(shopName);
                //绘制商店的形状信息
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

            if(locAnswer != null){
                //如果当前已被定位 则显示定位结果
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location30);
                Matrix matrix = new Matrix();
                //设置旋转角度
                matrix.postRotate(currentDegree,
                        bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                //设置左边距和上边距
                matrix.postTranslate(locAnswer.x - bitmap.getWidth() / 2, locAnswer.y - bitmap.getHeight()/2);
                //绘制旋转图片
                canvas.drawBitmap(bitmap, matrix, p);
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
            if(locAnswer != null){
                locAnswer.x -= xValue;
                locAnswer.y -= yValue;
            }
            System.out.println("locationMap:"+locationMap.toString());
            System.out.println("shapeMap:"+shapeMap.toString());
            if(locAnswer!=null) System.out.println("locAnswer subscribe Value:"+locAnswer);

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
                    coordinate.x = (float)(coordinate.x * scaleRate + Constant.MARGIN);
                    coordinate.y = canvasHeight - (float)(coordinate.y * scaleRate + Constant.MARGIN);
                }
            }
            for(String shopName: locationMap.keySet()){
                List<Coordinate> coordinates = locationMap.get(shopName);
                for(Coordinate coordinate:coordinates){
                    coordinate.x = (float)(coordinate.x * scaleRate + Constant.MARGIN);
                    coordinate.y = canvasHeight - (float)(coordinate.y * scaleRate + Constant.MARGIN);
                }
            }
            if(locAnswer != null){
                locAnswer.x = (float) (locAnswer.x * scaleRate + Constant.MARGIN);
                locAnswer.y = canvasHeight - (float)(locAnswer.y * scaleRate + Constant.MARGIN);
            }

            System.out.println("locationMap:"+locationMap.toString());
            System.out.println("shapeMap:"+shapeMap.toString());
            if(locAnswer!=null) System.out.println("locAnswer final:"+locAnswer);
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

}


