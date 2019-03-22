package com.zhouqing.chatproject.realtimeindoorlocation.util;

import com.zhouqing.chatproject.realtimeindoorlocation.model.Text;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HTTPUtil {

    private static String pid = "89cf72f38f569753d57cd9a51c5697a3";
    private static String service = "basicOpenOcr";
    private static String salt = "1508404016012";
    private static String key = "13bb022913b44c1b082aa67431827874";
    private static String url = "http://deepi.sogou.com:80/api/sogouService";

    /**
     * 发送搜狗OCR请求
     * @param base64
     * @return
     */
    public static String SougoOcrRequest(String base64){
        String base64Short = base64.substring(0,1024);
        String sign = MD5(pid+service+salt+base64Short+key);
        RequestBody requestBody = new FormBody.Builder().add("pid",pid)
                .add("lang","zh-CHS")
                .add("service",service)
                .add("salt",salt)
                .add("sign",sign)
                .add("image",base64)
                .build();
        Request request = new Request.Builder().url(url)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient();
        try {
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            System.out.println("response:"+responseData);
            return responseData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * MD5加密
     * @param s
     * @return
     */
    public final static String MD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析ocr结果
     * @param responseData
     * @param detectTextList
     * @throws JSONException
     */
    public static void parseOCRResponse(String responseData, List<Text> detectTextList) throws JSONException {
        JSONObject result = new JSONObject(responseData);
        int success = result.getInt("success");
        if(success == 1){
            JSONArray jsonArray = new JSONArray(result.getString("result"));
            for(int i=0;i<jsonArray.length();i++){
                String content = jsonArray.getJSONObject(i).getString("content");
                JSONArray frame = jsonArray.getJSONObject(i).getJSONArray("frame");
                String[] topLeft = frame.getString(0).split(",");
                String[] rightBottom = frame.getString(2).split(",");
                float top = Float.parseFloat(topLeft[1]);
                float left = Float.parseFloat(topLeft[0]);
                float bottom = Float.parseFloat(rightBottom[1]);
                float right = Float.parseFloat(rightBottom[0]);
                Text text = new Text(left,top,right,bottom,content);
                detectTextList.add(text);
            }
        }
    }
}
