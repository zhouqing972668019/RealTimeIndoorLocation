package com.zhouqing.chatproject.realtimeindoorlocation.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.zhouqing.chatproject.realtimeindoorlocation.model.Coordinate;
import com.zhouqing.chatproject.realtimeindoorlocation.model.StandardLocationInfo;
import com.zhouqing.chatproject.realtimeindoorlocation.model.TextDetectionAndPoi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileUtil {
	private static final String TAG = "FileUtil";
	private static final String LOG_FILE_SUFFIX = ".log";
	private static String sLogBasePath;

	/**
	 * 读写文件的线程池，单线程模型
	 */
	private static ExecutorService sExecutorService;

	static {
		sExecutorService = Executors.newSingleThreadExecutor();
	}

	/**
	 * 设置Log存放位置，同时删除超过存放时长的Log
	 *
	 * @param basePath
	 */
	public static void initBasePath(String basePath, int maxSaveDays) {
		sLogBasePath = basePath;
		if (!new File(basePath).exists()) {
			new File(basePath).mkdirs();
		}
		delOldFiles(new File(basePath), maxSaveDays);
	}

	/**
	 * 删除文件夹下所有的 N 天前创建的文件
	 * 注意: 由于拿不到文件的创建时间，这里暂且拿最后修改时间比较
	 *
	 * @param dir
	 * @param days
	 */
	public static void delOldFiles(File dir, int days) {
		long daysMillis = days * 24 * 60 * 60 * 1000L;
		if (dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isFile() && System.currentTimeMillis() - files[i].lastModified() > daysMillis) {
						files[i].delete();
					}
				}
			}
		}
	}

	/**
	 * 把文本写入文件中
	 *
	 * @param file       目录文件
	 * @param content    待写内容
	 * @param isOverride 写入模式，true - 覆盖，false - 追加
	 */
	public static void write(@NonNull final File file, @NonNull final String content, final boolean isOverride) {
		sExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				FileOutputStream fos = null;
				try {
					boolean isExist = file.exists();
					fos = new FileOutputStream(file, !(!isExist || isOverride));
					fos.write(content.getBytes("UTF-8"));
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							Log.e(TAG, e.getMessage());
						}
					}
				}
			}
		});
	}

	public static void writeLog(String content) {
		write(getLogFile(), "\n[" + getFormattedSecond() + "] " + content + "\n\n", false);
	}

	/**
	 * 拿到最新的Log文件
	 *
	 * @return
	 */
	public static File getLogFile() {
		File dir = new File(sLogBasePath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File logFile = new File(dir, getFormattedDay() + LOG_FILE_SUFFIX);
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return logFile;
	}

	//==================================== TimeUtil =============================================//
	public static final String FORMATTER_DAY = "yy_MM_dd";
	public static final String FORMATTER_SECOND = "yy-MM-dd HH:mm:ss";

	public static SimpleDateFormat sSecondFormat = new SimpleDateFormat(FORMATTER_SECOND);

	public static String getFormattedDay() {
		return new SimpleDateFormat(FORMATTER_DAY).format(new Date());
	}

	public static String getFormattedSecond() {
		return sSecondFormat.format(new Date());
	}


	//==================================== new add =============================================//
	//将指定字符串写入文件
	public static void writeStrToFile(String fileContent)
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss");//设置日期格式
		String outputFileName="输出结果："+df.format(new Date());// new Date()为获取当前系统时间
		if (!new File(Constant.OUTPUT_FILE_PATH).exists()) {
			new File(Constant.OUTPUT_FILE_PATH).mkdirs();
		}
		FileOutputStream fos= null;
		try {
			fos = new FileOutputStream(Constant.OUTPUT_FILE_PATH+outputFileName);
			fos.write(fileContent.getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	//获取某个文件夹下所有文件的名称
	public static List<String> getFileName(String path) {
		List<String> fileNameList=new ArrayList<>();
		File f = new File(path);
		if (!f.exists()) {
			return null;
		}
		File fa[] = f.listFiles();
		for (int i = 0; i < fa.length; i++) {
			File fs = fa[i];
			if (!fs.isDirectory()) {
				fileNameList.add(fs.getName());
			}
		}
		return fileNameList;
	}

	//获取某个文件夹下某张照片后面的照片
	public  static String getNextFileName(String path,String fileName)
	{
		List<String> fileNameList=getFileName(path);
		String answer=null;
		for(int i=0;i<fileNameList.size();i++)
		{
			if(fileNameList.get(i).equals(fileName))
			{
				answer = fileNameList.get(i+1);
			}
		}
		return answer;
	}
	//读取某个时间戳前后的传感器数据
	public static String getSensorInfo(String path,String fileName,String timeStamp) throws IOException {
		String answer="";
		File file=new File(path,fileName);
		//BufferedReader是可以按行读取文件
		FileInputStream inputStream = new FileInputStream(file);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

		String lastStr="";
		String str = null;
		while((str = bufferedReader.readLine()) != null)
		{
			String[] elements=str.split(" ");
			//方向传感器数据
			if(elements[0].equals("ori"))
			{
				BigInteger findTime=new BigInteger(timeStamp);
				BigInteger curTime=new BigInteger(elements[1]);
				if(findTime.compareTo(curTime)<=0)
				{
					answer=lastStr+","+str;
					break;
				}
				lastStr=str;
			}
		}

		//close
		inputStream.close();
		bufferedReader.close();
		return answer;
	}

	//读取某个时间戳前后的传感器数据
	public static Double[] getSensorInfoByName(String path,String timeStamp,String sensorName) throws IOException {
		Double[] answer = new Double[3];
		for(int i=0;i<answer.length;i++){
			answer[i] = 0.0;
		}
		File file=new File(path);
		//BufferedReader是可以按行读取文件
		FileInputStream inputStream = new FileInputStream(file);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

		String str = null;
		while((str = bufferedReader.readLine()) != null)
		{
			String[] elements=str.split(" ");
			//相应传感器数据
			if(elements[0].equals(sensorName))
			{
				BigInteger findTime=new BigInteger(timeStamp);
				BigInteger curTime=new BigInteger(elements[1]);
				if(findTime.compareTo(curTime)<=0)
				{
					System.out.println("elements:"+ Arrays.toString(elements)+"elements[2]:"+elements[2]);
					answer[0] = (Double.parseDouble(elements[2]) + answer[0])/2.0;
					System.out.println("answer[0]:"+Double.parseDouble(elements[2]));
					answer[1] = (Double.parseDouble(elements[3]) + answer[1])/2.0;
					answer[2] = (Double.parseDouble(elements[4]) + answer[2])/2.0;
					break;
				}
				answer[0] = Double.parseDouble(elements[2]);
				answer[1] = Double.parseDouble(elements[3]);
				answer[2] = Double.parseDouble(elements[4]);
			}
		}


		//close
		inputStream.close();
		bufferedReader.close();
		return answer;
	}

	//删除某个文件夹下所有文件（包括子文件夹及文件）
	public static boolean deleteDir(File dir)
	{
		if(dir.isDirectory())
		{
			String[] children=dir.list();
			for(int i=0;i<children.length;i++)
			{
				boolean isSuccess=deleteDir(new File(dir,children[i]));
				if(!isSuccess)
				{
					return false;
				}
			}
		}
		return dir.delete();
	}



	//bitmap转成文件存储用于拍照或者相册选取的时候
	public static void saveImageByBitmap(Bitmap bmp, String path, String fileName) {
		File appDir = new File(path);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}
		File file = new File(appDir, fileName);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static final String SPNAME = "initializationInfo";

	//写初始化信息-POILocation到sharedPrefereneces
	public static void saveLocationToFile(Context context, Map<String,StandardLocationInfo> locationInfoHashMap){
		//存储室内平面图中识别的POI及位置信息
		JSONArray mJsonArray = new JSONArray();
		for(String key:locationInfoHashMap.keySet()){
			try {
				JSONObject object = new JSONObject();
				StandardLocationInfo standardLocationInfo = locationInfoHashMap.get(key);
				object.put("name", key);
                object.put("x", standardLocationInfo.getX()+"");
                object.put("y", standardLocationInfo.getY()+"");
				mJsonArray.put(object);
			} catch (JSONException e) {

			}
		}
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("POILocation", mJsonArray.toString());
		editor.commit();
	}

	//从sharedPreferences中读取初始化信息-POI位置
	public static Map<String, StandardLocationInfo> getPOILocation(Context context) {
		Map<String, StandardLocationInfo> data = new HashMap<>();
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		String result = sp.getString("POILocation", "");
		if(result.equals(""))return data;
		try {
			JSONArray array = new JSONArray(result);
			for (int i = 0; i < array.length(); i++) {
				JSONObject itemObject = array.getJSONObject(i);
                String name = itemObject.getString("name");
                double x = Double.parseDouble(itemObject.getString("x"));
                double y = Double.parseDouble(itemObject.getString("y"));
                StandardLocationInfo standardLocationInfo = new StandardLocationInfo(x,y);
                data.put(name,standardLocationInfo);
			}
		} catch (JSONException e) {

		}
		return data;
	}

	//写int类型值到sharedPreferences
	public static void saveSpInt(Context context, String name, Integer value){
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		if(value != null)editor.putInt(name,value);
		editor.commit();
	}

	//从sharedPreferences中读取int类型值
	public static int getSPInt(Context context,String name){
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		return sp.getInt(name,0);
	}

	//写float类型值到sharedPreferences
	public static void saveSpFloat(Context context, String name, Float value){
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		if(value != null)editor.putFloat(name,value);
		editor.commit();
	}

	//从sharedPreferences中读取float类型值
	public static float getSPFloat(Context context,String name){
		SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
		return sp.getFloat(name,0f);
	}

    /**
     * 保存定位时用到的POI的名称
     * @param context
     * @param textDetectionInfoMap
     */
    public static void savePOINames(Context context, Map<String, TextDetectionAndPoi> textDetectionInfoMap){
        SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        StringBuilder sb = new StringBuilder();
        for(String POIName: textDetectionInfoMap.keySet()){
            sb.append(POIName).append(",");
        }
        String POINames = sb.toString();
        editor.putString("POINames",POINames.substring(0,POINames.length()-1));
        editor.commit();
    }

    public static List<String> getPOINames(Context context){
        SharedPreferences sp = context.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
        List<String> POINameList = new ArrayList<>();
        String POINames = sp.getString("POINames","");
        for(String POIName:POINames.split(",")){
            POINameList.add(POIName);
        }
        return POINameList;
    }

	//判断文件是否存在
	public static boolean fileIsExists(String strFile)
	{
		try {
			File f=new File(strFile);
			if(!f.exists()) {
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	//按行读取文件，构造室内平面图
	public static void readFileByLineToGetFloorPlan(String filePath,Context context){
		//删除当前平面图信息
		Map<String,StandardLocationInfo> hashMap = new HashMap<>();
		FileUtil.saveLocationToFile(context,hashMap);
		try {
			File file = new File(filePath);
			if (file.isFile() && file.exists()) {  //文件存在的前提
				InputStreamReader isr = new InputStreamReader(new FileInputStream(file),"utf-8");
				BufferedReader br = new BufferedReader(isr);
				String lineTxt = null;
				while ((lineTxt = br.readLine()) != null) {  //
					if (!"".equals(lineTxt)) {
						String[] elements = lineTxt.split(",");
						String poiName = elements[0].trim();
						double poiX = Double.parseDouble(elements[1].trim());
						double poiY = Double.parseDouble(elements[2].trim());
						StandardLocationInfo standardLocationInfo = new StandardLocationInfo(poiX,poiY);
						hashMap.put(poiName,standardLocationInfo);
					}
				}
				//写新的平面图信息到系统
				FileUtil.saveLocationToFile(context,hashMap);
				isr.close();
				br.close();
			}else {
				System.out.println("file not exists.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//获取指定文件夹下的一级子文件夹
	public static List<String> getChildFolder(String path) {
		List<String> folderList = new ArrayList<>();
		File f = new File(path);
		if (!f.exists()) {
			return null;
		}
		File fa[] = f.listFiles();
		for (int i = 0; i < fa.length; i++) {
			File fs = fa[i];
			if (fs.isDirectory()) {
				folderList.add(fs.getName());
			}
		}
		return folderList;
	}

	//将指定字符串写入文件
	public static void writeStrToPath(String fileName, String fileContent,String path)
	{
		String outputFileName = fileName + ".txt";
		if (!new File(path).exists()) {
			new File(path).mkdirs();
		}
		FileOutputStream fos= null;
		try {
			fos = new FileOutputStream(path+outputFileName);
			fos.write(fileContent.getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 将定位结果写到sharedPreferences中
	 * @param answer
	 * @param gyro_answer
	 * @param mag_acc_answer
	 * @param complex_gyro_answer
	 */
	public static void saveLocationResult(Context context,Double[] answer,Double[] gyro_answer,Double[] mag_acc_answer,Double[] complex_gyro_answer){
		saveSpFloat(context,"answerX",Float.parseFloat(String.valueOf(answer[0])));
		saveSpFloat(context,"answerY",Float.parseFloat(String.valueOf(answer[1])));
		saveSpFloat(context,"gyro_answerX",Float.parseFloat(String.valueOf(gyro_answer[0])));
		saveSpFloat(context,"gyro_answerY",Float.parseFloat(String.valueOf(gyro_answer[1])));
		saveSpFloat(context,"mag_acc_answerX",Float.parseFloat(String.valueOf(mag_acc_answer[0])));
		saveSpFloat(context,"mag_acc_answerY",Float.parseFloat(String.valueOf(mag_acc_answer[1])));
		saveSpFloat(context,"complex_gyro_answerX",Float.parseFloat(String.valueOf(complex_gyro_answer[0])));
		saveSpFloat(context,"complex_gyro_answerY",Float.parseFloat(String.valueOf(complex_gyro_answer[1])));
	}

	/**
	 * 按行读取txt
	 *
	 * @param context
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static List<String> readTextFromAssets(Context context,String fileName) throws Exception {
		InputStream is = context.getAssets().open(fileName);
		InputStreamReader reader = new InputStreamReader(is);
		BufferedReader bufferedReader = new BufferedReader(reader);
		List<String> answerList = new ArrayList<>();
		String str;
		while ((str = bufferedReader.readLine()) != null) {
			answerList.add(str);
		}
		return answerList;
	}

	/**
	 * 加载平面图信息，门的位置为多个门位置的平均
	 * @param context
	 * @param position
	 */
	public static void loadFloorPlan(Context context, int position){
		String fileName = Constant.SHOP_FILENAMES[position];
		try {
			List<String> fileContentList = readTextFromAssets(context,fileName);
			Map<String,StandardLocationInfo> locationInfoHashMap = new HashMap<>();
			for(String fileContent:fileContentList){
				String[] elements = fileContent.split(",");
				String POIName = elements[0];
				List<Double> xList = new ArrayList<>();
				List<Double> yList = new ArrayList<>();
				for(int i=1;i<elements.length-1;i+=2){
					xList.add(Double.parseDouble(elements[i]));
					yList.add(Double.parseDouble(elements[i+1]));
				}
				double xValue = 0d;
				for(Double x:xList){
					xValue += x;
				}
				xValue /= xList.size();
				double yValue = 0d;
				for(Double y:yList){
					yValue += y;
				}
				yValue /= yList.size();
				StandardLocationInfo standardLocationInfo = new StandardLocationInfo(xValue,yValue);
				locationInfoHashMap.put(POIName,standardLocationInfo);
			}
			FileUtil.saveLocationToFile(context,locationInfoHashMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 以map的形式加载平面图中商店的门信息以及形状信息
	 * @param context
	 * @param position
	 * @param shopNameLocationMap
	 * @param shopShapeMap
	 */
	public static void loadShopInfoAsList(Context context, int position, Map<String,List<Coordinate>> shopNameLocationMap, Map<String,List<Coordinate>> shopShapeMap){
		String shopLocationFileName = Constant.SHOP_FILENAMES[position];
		String shopShapeFileName = Constant.SHOP_SHAPES[position];
		try {
			List<String> shopLocationList = readTextFromAssets(context,shopLocationFileName);
			List<String> shopShapeList = readTextFromAssets(context,shopShapeFileName);
			//构造商店门位置信息
			for(String shopLocation: shopLocationList){
				String[] elements = shopLocation.split(",");
				String shopName = elements[0];
				List<Coordinate> coordinates = new ArrayList<>();
				for(int i=1;i<elements.length-1;i+=2){
					float x = Float.parseFloat(elements[i]);
					float y = Float.parseFloat(elements[i+1]);
					Coordinate coordinate = new Coordinate(x,y);
					coordinates.add(coordinate);
				}
				shopNameLocationMap.put(shopName,coordinates);
			}
			//构造商店形状信息
			for(String shopShape: shopShapeList){
				String[] elements = shopShape.split(",");
				String shopName = elements[0];
				List<Coordinate> coordinates = new ArrayList<>();
				for(int i=1;i<elements.length-1;i+=2){
					float x = Float.parseFloat(elements[i]);
					float y = Float.parseFloat(elements[i+1]);
					Coordinate coordinate = new Coordinate(x,y);
					coordinates.add(coordinate);
				}
				shopShapeMap.put(shopName,coordinates);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	//读文件，构造采集的文字识别和传感器数据
	public static void readFileToGetCollectionData(String filePath,List<String> sensorInfoList,List<String> textDetectionInfoList){
		//读取传感器信息
		try {
			File file = new File(filePath + Constant.SENSOR_FILE_NAME + ".txt");
			if (file.isFile() && file.exists()) {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(file),"utf-8");
				BufferedReader br = new BufferedReader(isr);
				String lineTxt = null;
				while ((lineTxt = br.readLine()) != null) {  //
					if (!"".equals(lineTxt)) {
						sensorInfoList.add(lineTxt);
					}
				}
				isr.close();
				br.close();
			}else {
				System.out.println("file not exists.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//读取文字识别结果信息
		try {
			File file = new File(filePath + Constant.TEXT_DETECTION_FILE_NAME + ".txt");
			if (file.isFile() && file.exists()) {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(file),"utf-8");
				BufferedReader br = new BufferedReader(isr);
				String lineTxt = null;
				while ((lineTxt = br.readLine()) != null) {  //
					if (!"".equals(lineTxt)) {
						textDetectionInfoList.add(lineTxt);
					}
				}
				isr.close();
				br.close();
			}else {
				System.out.println("file not exists.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
