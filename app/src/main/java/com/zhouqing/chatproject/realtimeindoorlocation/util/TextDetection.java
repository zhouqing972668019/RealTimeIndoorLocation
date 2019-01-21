
package com.zhouqing.chatproject.realtimeindoorlocation.util;

import java.util.ArrayList;
import java.util.List;

public class TextDetection {

    static  boolean isFinite(double d){
        return (!Double.isInfinite(d) && !Double.isNaN(d));
    }

    public static void drawCircle(double x, double y, double r){
        double a=20d, b=200d;

        double rw = r * a *2d;
        double x1 = x * a +b - r * a;
        double y1 = y * a +b - r * a;


//        fileCommon.g.drawLine((int)(b-a), (int)(b-a), (int)(b+a), (int)(b-a));
//        fileCommon.g.drawLine( (int)(b+a), (int)(b+a), (int)(b-a), (int)(b+a));
//        fileCommon.g.drawLine(0, (int)b, 1000, (int)b);
//        fileCommon.g.drawLine( (int)b, 0,(int)b, 1000);

//        fileCommon.g.drawOval((int)x1, (int)y1, (int)rw, (int)rw);

//        System.out.println("draw"+x1 + " " + y1 + " " + rw);
    }

    public static boolean lineCross(Double[] a, Double[] b, Double[] c, Double[] d) {
        if(!(Math.min(a[0],b[0])<= Math.max(c[0],d[0]) && Math.min(c[1],d[1])<= Math.max(a[1],b[1])&& Math.min(c[0],d[0])<= Math.max(a[0],b[0]) && Math.min(a[1],b[1])<= Math.max(c[1],d[1])))//这里的确如此，这一步是判定两矩形是否相交
            //1.线段ab的低点低于cd的最高点（可能重合） 2.cd的最左端小于ab的最右端（可能重合）
            //3.cd的最低点低于ab的最高点（加上条件1，两线段在竖直方向上重合） 4.ab的最左端小于cd的最右端（加上条件2，两直线在水平方向上重合）
            //综上4个条件，两条线段组成的矩形是重合的
            /*特别要注意一个矩形含于另一个矩形之内的情况*/
            return false;
    	       /*
    	       跨立实验：
    	       如果两条线段相交，那么必须跨立，就是以一条线段为标准，另一条线段的两端点一定在这条线段的两段
    	       也就是说a b两点在线段cd的两端，c d两点在线段ab的两端
    	       */
        double u,v,w,z;//分别记录两个向量
        u=(c[0]-a[0])*(b[1]-a[1])-(b[0]-a[0])*(c[1]-a[1]);
        v=(d[0]-a[0])*(b[1]-a[1])-(b[0]-a[0])*(d[1]-a[1]);
        w=(a[0]-c[0])*(d[1]-c[1])-(d[0]-c[0])*(a[1]-c[1]);
        z=(b[0]-c[0])*(d[1]-c[1])-(d[0]-c[0])*(b[1]-c[1]);
        return (u*v<=0.00000001 && w*z<=0.00000001);
    }

    private static final String TAG = "TextDetection";
    public static Double[] cal_corrdinate(List<Double> anglelist0, List<Double[]> corrdinatelistall, List<Integer> direction) {
        if(corrdinatelistall.size()<3) {
            System.out.println("ERROR: Not enough POIs!");
            return null;

        } else
//        image = imread(Constant.INITIAL_INDOOR_PHOTO_PATH + "initialization.jpg");

            if(corrdinatelistall.size()==3) {
                Double[] answer = work( anglelist0, corrdinatelistall, direction);
//            fileCommon.g.setColor(Color.BLUE);
                drawCircle(answer[0], answer[1],0.2);
//            imwrite(Constant.INITIAL_INDOOR_PHOTO_PATH + "finalResult.jpg",image);
                return answer;
            } else {
                final int numOfPois = corrdinatelistall.size();
                double diffp[][]=new double[numOfPois][numOfPois];
                double diffp2[]=new double[numOfPois];
                ArrayList<Double[]> answers = new ArrayList<>();
                ArrayList<Double> answerweight = new ArrayList<>();
                //构建虚拟坐标点表 店中心的位置

                //四个坐标点的情况:依次去除一个坐标点, 重复四次
                Double[] answer={0d, 0d};
                for (int j1 = 0; j1 < corrdinatelistall.size()-2; j1++) {
                    for (int j2 = j1+1; j2 < corrdinatelistall.size()-1; j2++) {
                        for (int j3 = j2 + 1; j3 < corrdinatelistall.size(); j3++) {
                            ArrayList<Double[]> corrdinatelist2 = new ArrayList<>();
                            corrdinatelist2.add(corrdinatelistall.get(j1));
                            corrdinatelist2.add(corrdinatelistall.get(j2));
                            corrdinatelist2.add(corrdinatelistall.get(j3));

                            double firstangle = 0d;
                            List<Double> anglelist1 = new ArrayList<>();
                            for (int j0 = 0; j0 < corrdinatelist2.size(); ++j0) {
                                double angle = corrdinatelist2.get(j0)[2];
                                if (firstangle != 0.0) {
                                    double diffangle = angle - firstangle;
                                    if (diffangle < 0.0) diffangle += 360.0;
                                    anglelist1.add(diffangle);
                                }
                                firstangle = angle;
                            }


                            findDiff(anglelist1, corrdinatelist2, direction, answers,answerweight, diffp);

                            //复原虚拟坐标点表

                        }
                    }

                }
/*
            //综合四个点: 显示一个普通平均值 颜色:粉色
            double dx0=0.0, dy0=0.0, da0=0.0;
            for(int j=0;j<4;++j){
                double ds = 1;
                dx0+=answers[j][0]*ds;
                dy0+=answers[j][1]*ds;
                da0+=ds;
            }
            dx0/=da0;
            dy0/=da0;
*/
                //显示加权平均值 颜色:红色
                double dx=0.0, dy=0.0, da=0.0;
                for(int j=0;j<corrdinatelistall.size();++j){
                    //    System.out.println(answers[j][0] + " " + answers[j][1]);
                    Double[] temp = answers.get(j);
                    if(temp==null || !(isFinite(temp[0])) || !(isFinite(temp[1]))) continue;
                    dx+=temp[0]*answerweight.get(j);
                    dy+=temp[1]*answerweight.get(j);
                    da+=answerweight.get(j);
                }
//            System.out.println(da + ",da " + dx + ",dx " + dy);
                dx/=da;
                dy/=da;
//            TextDetection3.drawCircle(dx, dy, 0.1, g);
                double olddx=dx, olddy=dy;
//            g.setColor(Color.BLACK);
//            TextDetection3.drawCircle(dx,dy, 0.1, g);
//            g.setColor(Color.RED);
//不知道要不要加:相当于将基准点从"加权平均值"移动到"普通平均值"


                //两两比较三点定位的结果,计算偏转向量
//            for(int j=0;j<corrdinatelistall.size();++j){
//                if(answers[j]==null || !(isFinite(answers[j][0])) || !(isFinite(answers[j][1]))) continue;
//                for(int j1=0;j1<corrdinatelistall.size();++j1){//双方被去掉的点既是对方的弱点
//                    if(answers[j1]==null || !(isFinite(answers[j1][0])) || !(isFinite(answers[j1][1]))) continue;
//                    if(j==j1)continue;
//                    double diff1=diffp[j1][j];//去掉j1后 j的不确定度 也就是第j1组中第j个点
//                    double diff2=diffp[j][j1];
////                            double mid = -(1/diff1-1/diff2)/(1/diff1+1/diff2)/8;
//                    double mid = (diff1-diff2)/(diff1+diff2)/8;
//                    if(!(isFinite(mid))) continue;
//                    dx+=(answers[j][0]-answers[j1][0])*mid;
//                    dy+=(answers[j][1]-answers[j1][1])*mid;
////                    System.out.println(dx + ",dx " + dy);
//                }
//            }
                //最早的定位方法:"反向加权"加偏转向量
//            circle(image,new Point(dx, dy),5,new Scalar(0xFF,0x00,0x00),-1,8,0);   //设置为-1时，画实心圆
//            imwrite(Constant.INITIAL_INDOOR_PHOTO_PATH + "finalResult.jpg",image);
//            TextDetection3.drawCircle(dx, dy, 0.1, g);
//            TextDetection3.drawLine(dx, dy, olddx,olddy, g);
//            fileCommon.g.setColor(Color.BLUE);
                drawCircle(dx,dy, 0.1);
                drawCircle(dx,dy,0.1*corrdinatelistall.size());
                System.out.println("ans:" + dx+" "+dy);
                return new Double[]{dx,dy};

            }
    }
    private static void findDiff(List<Double> anglelist0, List<Double[]> corrdinatelist, List<Integer> direction, List<Double[]> answers, List<Double> answersweight, double[][] diffp){
        //构建夹角表:复制并合并一个夹角

        //对(三个点中)每个点旋转一周,计算误差
        Double[] temp = work(anglelist0,corrdinatelist,  direction);
        answers.add(temp);
        double sum=0d;
        int count=0;

        if(temp!=null && isFinite(temp[0]) && isFinite(temp[1]) ){
//                circle(image,new Point(answers[j][0], answers[j][1]),5,new Scalar(0x66,0xCC,0xFF),-1,8,0);   //设置为-1时，画实心圆
            for (int j1 = 0; j1 < corrdinatelist.size(); j1++) {
                double maxdiff = 0;
                for (int j2 = 0; j2 < 100; ++j2) {
                    ArrayList<Double[]> corrdinatelist2 = new ArrayList<>();
                    for (Double[] point : corrdinatelist) {
                        corrdinatelist2.add(new Double[]{point[0], point[1]});
                    }
                    double angle = Math.PI * j2 / 50;
                    corrdinatelist2.get(j1)[0] += Math.cos(angle);
                    corrdinatelist2.get(j1)[1] += Math.sin(angle);
                    Double[] answer = work(anglelist0, corrdinatelist2, direction);
                    if (answer != null && isFinite(answer[0]) && isFinite(answer[1])) {
                        double difflength = dis(temp, answer);
                        maxdiff = Math.max(maxdiff, difflength);
                        count+=1;
                    }
                }
                sum+= maxdiff;
            }
        }
        answersweight.add(sum/count);


    }

    private static Double[] work(List<Double> anglelist0, List<Double[]> corrdinatelist, List<Integer> direction0) {
        ArrayList<Integer> direction = new ArrayList<>(direction0);
        ArrayList<Double> anglelist = new ArrayList<>(anglelist0);
//        System.out.println(anglelist);
//        for(Double[] point:corrdinatelist)
//            System.out.println(point[0]+ " "+point[1]);
//        System.out.println(direction);
        //计算scale，估计误差可接受范围
        /*
        double smin = 0.0, smax = 0.0;
        for ( Double[] i : corrdinatelist)
            for ( double j : i) {
                smin = Math.min(smin, j);
                smax = Math.max(smax, j);
            }

        double scale = smax - smin;
*/
        //补上最后一个夹角
        double totangle = 0.0;
        for ( double angle : anglelist) {
            totangle += angle;
//            System.out.println(angle);
        }
        anglelist.add(360 - totangle);
//        System.out.println("tot: " + (360 - totangle));

        final int numOfPois = corrdinatelist.size();
        //大于180°的-180
        for ( int i = 0; i < numOfPois; ++i) {
            if (anglelist.get(i) > 180.0) {
                anglelist.set(i, 360 - anglelist.get(i));
                direction.set(i, -1 * (direction.get(i)));
            }
        }

        List<Double[]> circles = new ArrayList<>();//存放圆心。一共有poi个元素，每个元素为[3][2],第一维代表两个圆心，第二维代表xy,[2][0]代表半径
        List<Double[]> results = new ArrayList<>();//存放交点。一共有poi个元素，每个元素为[2][2][2][2],
        //第一,二维代表相交圆的形态，第三维代表两个圆的两个交点，第四维代表xy


        //求出圆心
        for (int poi = 0; poi < numOfPois; ++poi) {
            int nextPoi = (poi + 1) % numOfPois;

            //取中点
            double midPointX = (corrdinatelist.get(poi)[0] + corrdinatelist.get(nextPoi)[0]) / 2;
            double midPointY = (corrdinatelist.get(poi)[1] + corrdinatelist.get(nextPoi)[1]) / 2;
            Double[] midPoint = {midPointX, midPointY};

            //中点指向圆心的向量（正负）
            double tangent = Math.abs(Math.tan(Math.toRadians(anglelist.get(poi))));//圆心角钝角
            double toCircleX = -(corrdinatelist.get(nextPoi)[1] - corrdinatelist.get(poi)[1]) / 2 / tangent;
            double toCircleY = (corrdinatelist.get(nextPoi)[0] - corrdinatelist.get(poi)[0]) / 2 / tangent;

            //两个对称的圆心:通过给定的方向,选择一个正确的圆心
            Double[] circle = new Double[3];
            circle[0] = midPointX + toCircleX;//圆心0 当a2>a1,b2>b1时，在上面
            circle[1] = midPointY + toCircleY;
            double x = circle[0], y = circle[1];
            double x1 = corrdinatelist.get(poi)[0], y1 = corrdinatelist.get(poi)[1];
            double x2 = corrdinatelist.get(nextPoi)[0], y2 = corrdinatelist.get(nextPoi)[1];
            double ans = (y - y1) * (x2 - x1) - (x - x1) * (y2 - y1);
            if (ans * direction.get(poi) > 0 ^ anglelist.get(poi) < 90.0) {
                circle[0] = midPointX - toCircleX;//圆心1 当a2>a1,b2>b1时，在下面
                circle[1] = midPointY - toCircleY;
            }
            //TODO:why abs?
            circle[2] = Math.abs(dis(midPoint, corrdinatelist.get(poi)) / Math.sin(Math.toRadians(anglelist.get(poi))));
//            System.out.println("poi: " + poi + " circle: " + Arrays.deepToString(circle));

            circles.add(circle);
            if(!(isFinite(circle[0]) && isFinite(circle[1]) && isFinite(circle[2]))) {
                System.out.println("circle cannot be drawn at cal_corrdinate > work : ");
                for (int i = 0; i < 3; i++) {
                    //        System.out.println("corrdinate: " + corrdinatelist.get(poi)[0] + " " + corrdinatelist.get(poi)[1]);
                }
                return null;
            }
        }

        //两两相交求出焦点
        for ( int poi = 0; poi < numOfPois; ++poi) {
            int nextPoi = (poi + 1) % numOfPois;

            //取圆心
            Double[] result = new Double[2];
            double a1 = circles.get(poi)[0];
            double b1 = circles.get(poi)[1];
            double r1 = circles.get(poi)[2];
            double a2 = circles.get(nextPoi)[0];
            double b2 = circles.get(nextPoi)[1];
            double r2 = circles.get(nextPoi)[2];

            //		System.out.println("a1: "+a1+" b1: "+b1+" r1: "+r1+" a2: "+a2+" b2: "+b2+" r2: "+r2);

            //方程(x-a1)2+(y-b1)2=r12和(x-a2)2+(y-b2)2=r22相减得到Ax+By+C=0的式子，其中A,B,C为
            double A = -2 * (a1 - a2);
            double B = -2 * (b1 - b2);
            double C = a1 * a1 - a2 * a2 + b1 * b1 - b2 * b2 - r1 * r1 + r2 * r2;
            //		System.out.println("ABC: "+A+" "+B+" "+C);
            //代入方程(x-a1)2+(y-b1)2=r12后得到形如AAx2+BBx+CC=0的式子，其中
            double AA = 1 + A * A / B / B;
            double BB = -2 * a1 + 2 * A / B * (C / B + b1);
            double CC = a1 * a1 + Math.pow(C / B + b1, 2) - r1 * r1;
            //		System.out.println("AABBCC: "+AA+" "+BB+" "+CC);

            //解出delta=BB2-4AACC
            double delta = BB * BB - 4 * AA * CC;

            if (delta < 0) {
                //返回单值：Ax+By+C=0和(x-a1)/(a2-a1)=(y-b1)/(b2-b1)联立的值
                //由于至少有一个相邻点的交点,不应该返回delta<0
                //当然, 这是仅有相邻点做交点的情况.4个点以上,加上不相邻点的情况,则另当别论
                System.out.println("error:delta<0");
            }
            double sqrtdelta = Math.sqrt(delta);
            result[0] = (-BB + sqrtdelta) / 2 / AA;
            result[1] = -(A * result[0] + C) / B;//两个交点,找那个不是公用交点的那个.
            if (dis_s(result, corrdinatelist.get(nextPoi)) < 1E-5) {
                result[0] = (-BB - sqrtdelta) / 2 / AA;
                result[1] = -(A * result[0] + C) / B;
                if(!(isFinite(result[0]) && isFinite(result[1]))){
                    System.out.println("circles crosspoint isnot finite at cal_corrdinate > work : ");
                    System.out.println(result[0] + " " + result[1]);
                    return null;
                }
            }


            results.add(result);
//            circle(image,new Point(result[0], result[1]),5,new Scalar(255,0,0),-1,8,0);   //设置为-1时，画实心圆

        }

        /*
        double max = 0;
//        double midPointX = 0;
        double midPointY = 0;

        for ( int poi = 0; poi < numOfPois; ++poi)//计算最大边
        {
            int nextPoi = (poi + 1) % numOfPois;
            Double[] crosspoint = results.get(poi);
            Double[] nextpoint = results.get(nextPoi);
//            drawCircle(crosspoint[0], crosspoint[1], 0.2, g);

            max = Math.max(max, dis(crosspoint, nextpoint));
            midPointX += crosspoint[0];
            midPointY += crosspoint[1];
//	System.out.println(Arrays.toString(crosspoint)+"     "+dis_s(crosspoint,nextpoint));
        }

//        Double[] answer = {midPointX / numOfPois, midPointY / numOfPois};
*/
//        for (Double[] result : results) {
//            System.out.println(result[0] + " " + result[1]);
//        }
        return results.get(0);
//        System.out.println("dist:" + max + " pos: " + Arrays.toString(answer));

//        drawCircle(answer[0], answer[1], 0.2, g);

    }

    //两点距离
    private static Double dis(Double[] a, Double[] b) {

        return Math.sqrt(dis_s(a, b));
    }

    //两点距离的平方
    private static Double dis_s(Double[] a, Double[] b) {
        return ((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1]));
    }



    public static void main(String[] args){
        ArrayList<Double[]> truecorrdinatelist = new ArrayList<>();
        Double[] location3 = {1122d, 910d, 135.53};
        truecorrdinatelist.add(location3);//DIOR
        Double[] location5 = {874d, 200d, 237.7};
        truecorrdinatelist.add(location5);//STAR
        Double[] location4 = {445d, 200d, 296.57};
        truecorrdinatelist.add(location4);//NIKE
        Double[] location2 = {198.5d, 548.5d, 358.35};
        truecorrdinatelist.add(location2);//GAP
        Double[] location1 = {199d, 789d, 23.99};
        truecorrdinatelist.add(location1);//SONY
        Double[] location7 = {505d, 912.5d, 63.3};
        truecorrdinatelist.add(location7);//UGG

        mainWork(truecorrdinatelist);
    }
    public static void mainWork(ArrayList<Double[]> truecorrdinatelist){

        final List<Integer> direction = new ArrayList<>();
        for (int i = 0; i < truecorrdinatelist.size(); i++) {
            direction.add(-1);
        }

        List<Double[]> newcorrdinatelist = new ArrayList<>(truecorrdinatelist);
//        for (int i = 0; i < truecorrdinatelist.size(); i++) {
//            newcorrdinatelist.clear();
//            newcorrdinatelist.add(truecorrdinatelist.get(i));
//            for (int j = i+1; j < truecorrdinatelist.size(); j++) {
//                newcorrdinatelist.add(truecorrdinatelist.get(j));
//                for (int k = j+1; k < truecorrdinatelist.size(); k++) {
//                    newcorrdinatelist.add(truecorrdinatelist.get(k));

        final ArrayList<Double> anglelist0 = new ArrayList<>();
        Double[] truth = {674d, 531d};
        double firstangle=0d;
        for(int j0=0;j0<newcorrdinatelist.size();++j0){
            Double[] point = newcorrdinatelist.get(j0);
            double diffx = point[0] - truth[0];
            double diffy = point[1] - truth[1];
            double angle = Math.toDegrees(Math.atan(diffy / diffx));
            if (diffx < 0) angle += 180;
            if (diffy < 0 && diffx > 0) angle += 360;
            if (firstangle!=0.0) {
                double diffangle = -angle + firstangle;
                if (diffangle < 0.0) diffangle += 360.0;
                anglelist0.add(diffangle);
            }
            firstangle = angle;
        }
        System.out.println("0:"+anglelist0);
        anglelist0.clear();

        firstangle=0d;
        for(int j0=0;j0<newcorrdinatelist.size();++j0){
            Double[] point = newcorrdinatelist.get(j0);
            double diffy = 1000d * Math.sin(Math.toRadians(point[2]));
            double diffx = 1000d * Math.cos(Math.toRadians(point[2]));
//            TextDetection3.drawLine(point[0]-diffx, point[1]-diffy, point[0]+diffx, point[1]+diffy, g);
            diffx=-diffx;

            double angle = point[2];
            if (firstangle!=0.0) {
                double diffangle = angle - firstangle;
                if (diffangle < 0.0) diffangle += 360.0;
                anglelist0.add(diffangle);
            }
            firstangle = angle;
        }
        System.out.println("1:"+anglelist0);


        Double[] d = cal_corrdinate(anglelist0, newcorrdinatelist,  direction);
//                    try{ Thread.sleep(5000);} catch (Exception e) {e.printStackTrace();}
//                    g.setColor(Color.white);
//                    cal_corrdinate(anglelist0, newcorrdinatelist,  direction, g);

        for(Double[] t : newcorrdinatelist)
        {
            System.out.println(t[0] + " " + t[1] + " " + t[2]);
        }
        if(d==null) return;
        System.out.println(d[0] + " " +d[1]);
        anglelist0.clear();
        truth = d;
        firstangle=0d;
        for(int j0=0;j0<truecorrdinatelist.size();++j0){
            Double[] point = truecorrdinatelist.get(j0);
            double diffx = point[0] - truth[0];
            double diffy = point[1] - truth[1];
            double angle = Math.toDegrees(Math.atan(diffy / diffx));
            if (diffx < 0) angle += 180;
            if (diffy < 0 && diffx > 0) angle += 360;
            if (firstangle!=0.0) {
                double diffangle = angle - firstangle;
                if (diffangle < 0.0) diffangle += 360.0;
                anglelist0.add(diffangle);
            }
            firstangle = angle;
        }
        System.out.println("2:"+anglelist0);

//        newcorrdinatelist.remove(2);
//                }
//                newcorrdinatelist.remove(1);
//            }
//            newcorrdinatelist.remove(0);
//        }
    }


}
