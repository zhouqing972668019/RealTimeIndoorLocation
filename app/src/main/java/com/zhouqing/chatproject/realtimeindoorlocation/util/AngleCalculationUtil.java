package com.zhouqing.chatproject.realtimeindoorlocation.util;

import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionAndPoi;

import java.util.Map;

public class AngleCalculationUtil {

	public static int getQuadrant(double[] baseAngles, double angle) {
		for (int i = 1; i <= 4; i++) {
			double startAngle = baseAngles[(2 + i) % 4];
			double endAngle = baseAngles[i - 1];
			if (startAngle < endAngle) {
				if (angle > startAngle && angle < endAngle) {
					return i;
				}
			} else {
				endAngle += 360;
				double angel_copy = angle;
				if (angel_copy < startAngle) {
					angel_copy += 360;
				}
				if (angel_copy > startAngle && angel_copy < endAngle) {
					return i;
				}
			}
		}
		// 1,2,3,4 表示4个象限 -1表示横轴 -2表示纵轴
		for (int i = 0; i < baseAngles.length; i++) {
			if (baseAngles[i] == angle) {
				if (i == 0 || i == 2) {
					return -1;
				} else {
					return -2;
				}
			}
		}
		return 0;

	}

	public static double getSlope(double[] baseAngles, int quadrant, double angle) {
		if (quadrant == -1) {
			return Math.tan(0.0);
		}
		if (quadrant == -2) {
			return Math.tan(Math.PI / 2.0);
		}
		double tanAngle = 0.0;
		double result = 0.0;
		switch (quadrant) {
			case 1:
				tanAngle = baseAngles[0] - angle;
				if (tanAngle < 0)
					tanAngle += 360;
				result = Math.tan((tanAngle) / 180 * Math.PI);
				break;
			case 2:
				tanAngle = angle - baseAngles[0];
				if (tanAngle < 0)
					tanAngle += 360;
				result = -Math.tan((tanAngle) / 180 * Math.PI);
				break;
			case 3:
				tanAngle = baseAngles[2] - angle;
				if (tanAngle < 0)
					tanAngle += 360;
				result = Math.tan((tanAngle) / 180 * Math.PI);
				break;
			case 4:
				tanAngle = angle - baseAngles[2];
				if (tanAngle < 0)
					tanAngle += 360;
				result = -Math.tan((tanAngle) / 180 * Math.PI);
				break;
			default:
				break;
		}
		return result;
	}

	public static double getIntercept(StandardLocationInfo locationInfo, double slope) {
		double x = locationInfo.getX();
		double y = locationInfo.getY();
		return y - slope * x;
	}

	public static StandardLocationInfo getTargetBy2Points(double slope1, double intercept1, double slope2, double intercept2) {
		double x = (intercept2 - intercept1) / (slope1 - slope2 + 0.0000001);
		double y = slope1 * x + intercept1;
		StandardLocationInfo locationInfo = new StandardLocationInfo(x, y);
		return locationInfo;
	}

	// 2个POI求坐标
	public static StandardLocationInfo getLocaionBy2Points(double startAngle, String point1, double angle1, String point2,
														   double angle2, Map<String,StandardLocationInfo> standardLocationInfoMap, boolean isClockwise) {
		if (isClockwise = true) {
			// 构建直角坐标系的4个坐标轴角度
			double[] baseAngles = new double[4];
			baseAngles[0] = startAngle;
			for (int i = 1; i < 4; i++) {
				baseAngles[i] = (baseAngles[i - 1] + 90) % 360;
			}

			// 分别判断两条直线所属象限
			int quadrant1 = getQuadrant(baseAngles, angle1);
			int quadrant2 = getQuadrant(baseAngles, angle2);

			// 已知象限的情况下计算斜率
			double slope1 = getSlope(baseAngles, quadrant1, angle1);
			double slope2 = getSlope(baseAngles, quadrant2, angle2);

			// 计算截距
			double intercept1 = getIntercept(standardLocationInfoMap.get(point1), slope1);
			double intercept2 = getIntercept(standardLocationInfoMap.get(point2), slope2);

			System.out.println("quarant1:" + quadrant1 + ",slope1:" + slope1 + ",intercept1:" + intercept1);
			System.out.println("quarant2:" + quadrant2 + ",slope2:" + slope2 + ",intercept2:" + intercept2);

			StandardLocationInfo result = getTargetBy2Points(slope1, intercept1, slope2, intercept2);
			return result;
		}
		// 逆时针旋转
		else {
			return null;
		}
	}

	public static int getDirection(StandardLocationInfo POILocation, StandardLocationInfo location) {
		double POIX = POILocation.getX();
		double POIY = POILocation.getY();
		double x = location.getX();
		double y = location.getY();
		if (x == POIX && y == POIY) {
			return 0;
		} else if (y == POIY && x > POIX) {
			return -1;
		} else if (x == POIX && y < POIY) {
			return -2;
		} else if (y == POIY && x < POIX) {
			return -3;
		} else if (x == POIX && y > POIY) {
			return -4;
		} else if (x > POIX && y > POIY) {
			return 1;
		} else if (x > POIX && y < POIY) {
			return 2;
		} else if (x < POIX && y < POIY) {
			return 3;
		} else // if(x<POIX&&y>POIY)
		{
			return 4;
		}

	}

	public static double getStartAngleByDir(StandardLocationInfo POILocation, StandardLocationInfo location, int dir, double angle) {
		double POIX = POILocation.getX();
		double POIY = POILocation.getY();
		double x = location.getX();
		double y = location.getY();
		double result = 0.0;
		double slope;
		double slopeAngle;
		switch (dir) {
			case 0:
				result = -1.0;//无法判断
				break;
			case -1:
				result=angle+180;
				if(result>360)result-=360;
				break;
			case -2:
				result=angle+90;
				if(result>360)result-=360;
				break;
			case -3:
				result=angle;
				break;
			case -4:
				result=angle+270;
				if(result>360)result-=360;
				break;
			case 1:
				slope=(y-POIY)/(x-POIX);
				slopeAngle=Math.atan(slope)/Math.PI*180.0;
				result=angle+slopeAngle+180;
				if(result>360)result-=360;
				break;
			case 2:
				slope=(POIY-y)/(x-POIX);
				slopeAngle=Math.atan(slope)/Math.PI*180.0;
				result=angle+180-slopeAngle;
				if(result>360)result-=360;
				break;
			case 3:
				slope=(POIY-y)/(POIX-x);
				slopeAngle=Math.atan(slope)/Math.PI*180.0;
				result=angle+slopeAngle;
				if(result>360)result-=360;
				break;
			case 4:
				slope=(y-POIY)/(POIX-x);
				slopeAngle=Math.atan(slope)/Math.PI*180.0;
				result=angle+360-slopeAngle;
				if(result>360)result-=360;
				break;
			default:
				break;
		}

		return result;
	}

	// 通过已知的POI坐标与定位结果的坐标推算水平向右方向的指南针角度
	public static double getStartAngle(String point, double angle,
									   Map<String, StandardLocationInfo> standardLocationInfoMap, StandardLocationInfo location,
									   boolean isClockwise) {
		if(isClockwise)
		{
			StandardLocationInfo POILocation = standardLocationInfoMap.get(point);
			// 判断poi在定位点的方向 1 右上方 2 右下方 3 左下方 4左上方 -1 右边 -2 下边 -3 左边 -4 上边 0 重合
			int dir = getDirection(POILocation, location);
			// 根据不同的方向得到水平轴的向右方向的角度
			return getStartAngleByDir(POILocation, location, dir, angle);
		}
		else
		{
			return 0.0;
		}
	}

	//通过已知的所有POI坐标与定位结果的坐标推算基准线坐标
	public static double getStartAngleAll(Map<String, TextDetectionAndPoi> textDetectionInfoMap,
										  Map<String, StandardLocationInfo> floorPlanMap,
										  Double[] coordinate, boolean isClockwise){
		if(isClockwise){
			double answer = 0.0;
			StandardLocationInfo coordinateInfo = new StandardLocationInfo(coordinate[0],coordinate[1]);
			for(String POIName: textDetectionInfoMap.keySet()){
				int dir = getDirection(floorPlanMap.get(POIName),coordinateInfo);
				double angle = getStartAngleByDir(floorPlanMap.get(POIName),coordinateInfo,dir,textDetectionInfoMap.get(POIName).ori_angle);
				System.out.println("x:"+floorPlanMap.get(POIName).getX()+",y:"+floorPlanMap.get(POIName).getY()+",POIName:"+POIName+",dir:"+dir+",angle:"+angle);
				answer += angle;
			}
			return answer/textDetectionInfoMap.size();
		}
		else{
			return 0.0;
		}
	}

	public static void main(String[] args) {
//		CalculationTest test = new CalculationTest();
//		StandardLocationInfo locationInfo = test.getLocaionBy2Points(195, "NIKE",265.60, "STAR", 216.99, true);
//		System.out.println("x:" + locationInfo.getX() + ",y:" + locationInfo.getY());
	}

}
