package com.zhouqing.chatproject.realtimeindoorlocation.util;

import android.os.Environment;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DELL on 2018/1/12.
 */

public class Constant {
    //项目存储文件的路径
    public static String PROJECT_FILE_PATH
            = Environment.getExternalStorageDirectory() + "/LocEye/";

    //数据采集阶段文字识别结果与传感器信息结果存放路径
    public static String COLLECTION_DATA_PATH
            =PROJECT_FILE_PATH + "CollectionInfo/";

    //处理结果的文件存储路径
    public static String OUTPUT_FILE_PATH
            =Environment.getExternalStorageDirectory()+"/OutputPath/";

    //测试100张图片的识别率-数据集文件夹
    public static String TEXT_DATASET
            =Environment.getExternalStorageDirectory()+"/firebase_dataset/";

    public static String SENSOR_FILE_NAME = "sensor";
    public static String TEXT_DETECTION_FILE_NAME = "textDetection";

    //传感器类型常量
    public static final int TYPE_GYRO_ORI = 10001;
    public static final int TYPE_MAG_ACC_ORI = 10002;

    //高德地图坐标的小数点位数
//    public static final float DECIMAL_FACTOR = 1000f;
    //绘制平面图时平面图距离手机边框的距离
    public static final int MARGIN = 20;

    //判断两个文本是否相邻时，顶部阈值
    public static final double ADJACENT_THRESHOLD = 15d;

    //判断识别文本框到手机屏幕中心的距离阈值
    public static final double CENTOR_DIS_THRESHOLD = 120d;

    public static final String[] SHOP_FILENAMES = {"SYS_LOCATION_INFO.txt","WDK_1F_SHOP_LOCATION_INFO.txt","WDK_2F_SHOP_LOCATION_INFO.txt",
            "XZG_1F_SHOP_LOCATION_INFO.txt","XZG_2F_SHOP_LOCATION_INFO.txt","WJ_1F_SHOP_LOCATION_INFO.txt","WJ_2F_SHOP_LOCATION_INFO.txt",
            "XD_1F_SHOP_LOCATION_INFO.txt","XD_2F_SHOP_LOCATION_INFO.txt"};
    public static final String[] SHOP_NAMES = {"SYS","WDK FLOOR 1","WDK FLOOR 2","XZG FLOOR 1","XZG FLOOR 2","WJ FLOOR 1","WJ FLOOR 2",
            "XD FLOOR 1","XD FLOOR 2"};
    public static final String[] SHOP_SHAPES = {"SYS_SHAPE_INFO.txt","WDK_1F_SHOP_SHAPE_INFO.txt","WDK_2F_SHOP_SHAPE_INFO.txt",
            "XZG_1F_SHOP_SHAPE_INFO.txt","XZG_2F_SHOP_SHAPE_INFO.txt","WJ_1F_SHOP_SHAPE_INFO.txt","WJ_2F_SHOP_SHAPE_INFO.txt",
            "XD_1F_SHOP_SHAPE_INFO.txt","XD_2F_SHOP_SHAPE_INFO.txt"};

    //室内平面图对应的经纬度
    public static final double[] SYS_LATLNG = {40.006638,116.340967};
    public static final double[] WDK_LATLNG = {39.993915,116.34668};
    public static final double[] XZG_LATLNG = {39.984237,116.321751};
    public static final double[] WJ_LATLNG = {39.998656,116.47493};
    public static final double[] XD_LATLNG = {39.916967,116.379299};

    /**
     * @brief 判断两个String字符串的相似程度（因为用户输入的字符可能和兴趣点的名字不能完全匹配）
     * 即计算两个字符串的距离
     * 定义一套操作方法来把两个不相同的字符串变为相同的的方法，忽略字符串大小写
     * 修改／增加／删除
     * 返回的是字符的相似度(百分数)
     */
    public static int calculateStringDistance(String str1,String str2){
        //比较的时候过滤空格
        Pattern p = Pattern.compile("\\s*");
        Matcher m1 = p.matcher(str1);
        Matcher m2 = p.matcher(str2);
        String s1 = m1.replaceAll("").toLowerCase();
        String s2 = m2.replaceAll("").toLowerCase();
        int len1 = s1.length();
        int len2 = s2.length();

        //建立上面说的数组，比字符长度大一个空间
        int[][] dif = new int[len1 + 1][len2 + 1];
        //赋初值
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        //计算两个字符是否一样，计算左上的值
        int temp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                //取三个值中最小的
                dif[i][j] = minValue(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
                        dif[i - 1][j] + 1);
            }
        }

        //计算相似度
        float similarity =1 - (float) dif[len1][len2] / Math.max(s1.length(), s2.length());
//        System.out.println("相似度："+similarity);
        return (int)(similarity*100);
    }

    public static int minValue(int a,int b,int c)
    {
        if(a>b)
        {
            a=b;
        }
        return a<c?a:c;
    }

    public static double calculateDistance(double[][] result){
        return Math.sqrt((result[1][0]-result[0][0])*(result[1][0]-result[0][0])+(result[1][1]-result[0][1])*(result[1][1]-result[0][1]));
    }

    /**
     * 验证实验 寻找文本识别区域
     */
    public static void findAreaOfTextDetection(List<String> textDetectionInfoList){
        double areaLeft = Double.MAX_VALUE;
        double areaRight = Double.MIN_VALUE;
        double areaTop = Double.MAX_VALUE;
        double areaBottom = Double.MIN_VALUE;
        for(String textDetectionInfo:textDetectionInfoList){
            String[] elements = textDetectionInfo.split(" ");
            double left = Double.parseDouble(elements[1]);
            double top = Double.parseDouble(elements[2]);
            double right = Double.parseDouble(elements[3]);
            double bottom = Double.parseDouble(elements[4]);
            if(left < areaLeft){
                areaLeft = left;
            }
            if(right > areaRight){
                areaRight = right;
            }
            if(top < areaTop){
                areaTop = top;
            }
            if(bottom > areaBottom){
                areaBottom = bottom;
            }
        }
        System.out.println("textDetectionArea:"+areaLeft+","+areaTop+","+areaRight+","+areaBottom);
    }

    public static float calculateDisByLatiAndLong(double latitude1, double longitude1, double latitude2, double longitude2){
        LatLng latLng1 = new LatLng(latitude1,longitude1);
        LatLng latLng2 = new LatLng(latitude2,longitude2);
        return AMapUtils.calculateLineDistance(latLng1,latLng2);
    }

    public static int getShopSelectionByDis(double latitude,double longitude){
        float minDis = Float.MAX_VALUE;
        int shopSelection = -1;
        float sysDis = calculateDisByLatiAndLong(latitude,longitude,SYS_LATLNG[0],SYS_LATLNG[1]);
        if(sysDis<minDis){
            minDis = sysDis;
            shopSelection = 0;
        }
        System.out.println("sysDis:"+sysDis);
        float wdkDis = calculateDisByLatiAndLong(latitude,longitude,WDK_LATLNG[0],WDK_LATLNG[1]);
        if(wdkDis<minDis){
            minDis = wdkDis;
            shopSelection = 1;
        }
        System.out.println("wdkDis:"+wdkDis);
        float xzgDis = calculateDisByLatiAndLong(latitude,longitude,XZG_LATLNG[0],XZG_LATLNG[1]);
        if(xzgDis<minDis){
            minDis = xzgDis;
            shopSelection = 3;
        }
        System.out.println("xzgDis:"+xzgDis);
        float wjDis = calculateDisByLatiAndLong(latitude,longitude,WJ_LATLNG[0],WJ_LATLNG[1]);
        if(wjDis<minDis){
            minDis = wjDis;
            shopSelection = 5;
        }
        System.out.println("wjDis:"+wjDis);
        float xdDis = calculateDisByLatiAndLong(latitude,longitude,XD_LATLNG[0],XD_LATLNG[1]);
        if(xdDis<minDis){
            minDis = xdDis;
            shopSelection = 7;
        }
        System.out.println("xdDis:"+xdDis);

        System.out.println("shopSelection:"+shopSelection+",minDis:"+minDis);
        return shopSelection;
    }

    /**
     * 移除字符串中的非法字符
     * @param POIName
     * @return
     */
    public static String removeIllegalAlphabet(String POIName){
        String result = "";
        for(int i=0;i<POIName.length();i++){
            char c = POIName.charAt(i);
            if((c>='0' && c<='9')||(c>='a' && c<='z')||(c>='A' && c<='Z')){
                result += c;
            }
        }
        return result;
    }


}
