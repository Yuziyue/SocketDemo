package com.example.socketdemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.opencv.core.CvType.CV_8UC1;

public class MainActivity extends AppCompatActivity {

    private EditText ipAddress;
    private EditText portAddress;
    private ImageView imageView;
    private TextView textView;
    private Button button1;
    private Button button2;

    private int socket_flag = 0;

    private static int SHOW_FLAG = 1;
    private static int GENERATE_FLAG = 2;
    private static int LINE_FLAG = 3;
    private static int DOT_FLAG = 4;
    private static int JSON_INSERT_FLAG = 5;
    private static int JSON_SAVE_FLAG = 6;
    private static int JSON_GENERATE_FLAG = 7;
    private static int OTHER_FLAG = 8;

    private int W;
    private int H;

    Socket socket;

    InputStream inputStream;
    OutputStream outputStream;
    String send_feedback_string = null;

    JSONObject object = new JSONObject();


    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,

            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;


    //Load OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("test", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("test", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("test", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }



    //隐藏虚拟按键，并且全屏
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void hideBottomMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Android保持屏幕常亮，不锁屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Display mDisplay = getWindowManager().getDefaultDisplay();
        W = mDisplay.getWidth();
        H = mDisplay.getHeight();
        Log.i("Main", "Width = " + W);
        Log.i("Main", "Height = " + H);
        hideBottomMenu();
        setContentView(R.layout.activity_main);
        //Request Permissions
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }
        button1 = (Button) findViewById(R.id.button1);
        button1.setEnabled(true);
        button2 = (Button) findViewById(R.id.button2);
        button2.setEnabled(false);
        ipAddress = (EditText) findViewById(R.id.ip_text);
        portAddress = (EditText) findViewById(R.id.port_text);
        imageView = (ImageView) findViewById(R.id.img);
        textView = (TextView) findViewById(R.id.status);


        imageView.setClickable(false);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageResource(0);
                imageView.setClickable(false);
            }
        });

        //连接socket
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress.clearFocus();
                portAddress.clearFocus();
                hideKeyBoard(MainActivity.this);
                initSocket();
                button1.setEnabled(false);
                button2.setEnabled(true);
            }
        });

        //断开连接
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("连接状态：未连接！");
                button1.setEnabled(true);
                button2.setEnabled(false);
                try {
                    socket.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //关闭软键盘
    private void hideKeyBoard(Activity context) {
        if(context == null){
            return;
        }
        final View v = ((Activity) context).getWindow().peekDecorView();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v != null && v.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    //  需要动态申请文件读写权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }


    //String是否纯数字
    public static boolean isNumeric(String str)
    {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))){
                return false;
            }
        }
        return true;
    }

    public boolean isLine(String str[]){
        if(str[0].equals("orient")&&str[2].equals("width")&&str[4].equals("grayscale")&&str[6].equals("location")&&isNumeric(str[3])&&isNumeric(str[5])&&isNumeric(str[7])){
            int width = Integer.parseInt(str[3]);
            int grayscale = Integer.parseInt(str[5]);
            int location = Integer.parseInt(str[7]);
            if(str[1].equals("R")){
                if(location<=H&&location>=0&&location+width<=H+1&&width>0&&grayscale<256&&grayscale>=0){
                    return true;
                }else{
                    return false;
                }
            }else if(str[1].equals("C")){
                if(location<=W&&location>=0&&location+width<=W+1&&width>0&&grayscale<256&&grayscale>=0){
                    return true;
                }else{
                    return false;
                }
            } else{
                return false;
            }
        }else{
            return false;
        }
    }

    public boolean isDot(String str[]){
        if(str[0].equals("radius")&&str[2].equals("grayscale")&&str[4].equals("X")&&str[6].equals("Y")&&isNumeric(str[1])&&isNumeric(str[3])&&isNumeric(str[5])&&isNumeric(str[7])){
            int radius = Integer.parseInt(str[1]);
            int grayscale = Integer.parseInt(str[3]);
            int x = Integer.parseInt(str[5]);
            int y = Integer.parseInt(str[7]);
            if(grayscale<256&&grayscale>=0&&x>=0&&y>=0&&radius>=0&&x-radius>=0&&x+radius<=W&&y-radius>=0&&y+radius<=H){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    // 将字符串写入到json文件中
    public void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;
        // 每次写入时，都换行写
        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                //Toast.makeText(this, "Create the file:"+strFilePath, Toast.LENGTH_SHORT).show();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }


    public Bitmap generateJson(String jsonStr){
        Mat mat = Mat.zeros(H,W, CvType.CV_8UC1);
        Bitmap bitmap = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
        String result=null;
        try {
            File f=new File(Environment.getExternalStorageDirectory().getPath()+"/DCIM/json/"+jsonStr+".json");
            int length=(int)f.length();
            byte[] buff=new byte[length];
            FileInputStream fin=new FileInputStream(f);
            fin.read(buff);
            fin.close();
            result=new String(buff,"UTF-8");
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(result);
            int grayscale = Integer.parseInt(jsonObject.getString("grayscale"));
            int radius = Integer.parseInt(jsonObject.getString("radius"));
            int num = Integer.parseInt(jsonObject.getString("num"));
            for(int i=1;i<=num;i++){
                String No = String.format("%03d",i);
                int temp = Integer.parseInt(jsonObject.getString(No));
                int x = temp/10000;
                int y = temp%10000;
                Imgproc.circle(mat,new Point(x,y),radius,new Scalar(grayscale),-1);
            }
            Utils.matToBitmap(mat,bitmap);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return bitmap;
    }



    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }


    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }



    //socket连接
    private void initSocket() {
        String ip = ipAddress.getText().toString();
        String temp = portAddress.getText().toString();
        int port = Integer.parseInt(temp);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, port);
                    socket.setTcpNoDelay(true);
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    outputStream.write("PH".getBytes("utf-8"));
                    socket_flag = 1;
                    socket_handler.sendEmptyMessage(socket_flag);
                    Log.i("Android", "与服务器建立连接:" + socket);
                    while (true) {
                        while (inputStream.available() == 0) ;
                        //缓冲区
                        byte[] buffer = new byte[inputStream.available()];
                        //读取缓冲区
                        inputStream.read(buffer);
                        //转换为字符串
                        String responseInfo = new String(buffer);
                        Log.i("输入", responseInfo);
                        Message pic_msg = new Message();
                        Message json_msg = new Message();
                        if (responseInfo.indexOf("show_") == 0) {
                            pic_msg.what = SHOW_FLAG;
                            pic_msg.obj = responseInfo;
                            pic_handler.sendMessage(pic_msg);
                            Log.i("有效输入", responseInfo);
                            while(send_feedback_string == null);  // 确保图片已完成显示
                            outputStream.write(send_feedback_string.getBytes("utf-8"));
                            send_feedback_string = null;
                            continue;
                        }
                        if(responseInfo.indexOf("generate_")==0){
                            if(responseInfo.indexOf("grayscale_")==9){
                                pic_msg.what = GENERATE_FLAG;
                                pic_msg.obj = responseInfo;
                                pic_handler.sendMessage(pic_msg);
                                Log.i("有效输入", responseInfo);
                                while(send_feedback_string == null);  // 确保图片已完成显示
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                            if(responseInfo.indexOf("single_line_")==9){
                                pic_msg.what = LINE_FLAG;
                                pic_msg.obj = responseInfo;
                                pic_handler.sendMessage(pic_msg);
                                Log.i("有效输入", responseInfo);
                                while(send_feedback_string == null);
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                            if(responseInfo.indexOf("Json_")==9){
                                pic_msg.what = JSON_GENERATE_FLAG;
                                pic_msg.obj = responseInfo;
                                pic_handler.sendMessage(pic_msg);
                                Log.i("有效输入", responseInfo);
                                while(send_feedback_string == null);
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                            if(responseInfo.indexOf("single_dot_")==9){
                                pic_msg.what = DOT_FLAG;
                                pic_msg.obj = responseInfo;
                                pic_handler.sendMessage(pic_msg);
                                Log.i("有效输入", responseInfo);
                                while(send_feedback_string == null);
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                        }
                        if(responseInfo.indexOf("Json_") == 0){
                            if(responseInfo.indexOf("insert_")==5){
                                json_msg.what = JSON_INSERT_FLAG;
                                json_msg.obj = responseInfo;
                                json_handler.sendMessage(json_msg);
                                Log.i("有效输入",responseInfo);
                                while(send_feedback_string == null);
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                            if(responseInfo.indexOf("save_")==5){
                                json_msg.what = JSON_SAVE_FLAG;
                                json_msg.obj = responseInfo;
                                json_handler.sendMessage(json_msg);
                                Log.i("有效输入",responseInfo);
                                while(send_feedback_string == null);
                                outputStream.write(send_feedback_string.getBytes("utf-8"));
                                send_feedback_string = null;
                                continue;
                            }
                        }
                        pic_msg.what = OTHER_FLAG;
                        pic_msg.obj = responseInfo;
                        pic_handler.sendMessage(pic_msg);
                        Log.i("有效输入", responseInfo);
                        while(send_feedback_string == null);  // 确保图片已完成显示
                        outputStream.write(send_feedback_string.getBytes("utf-8"));
                        send_feedback_string = null;
                        continue;
                    }
                } catch (UnknownHostException e) {
                    Log.e("Android", "连接错误");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e("Android", "连接服务器IO错误");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e("Android", "连接服务器错误");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    Handler json_handler = new Handler(){
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == JSON_INSERT_FLAG) {
                String jsonString = msg.obj.toString().substring(12);
                Log.i("Json处理","这是输入：" + jsonString);
                String str[] = jsonString.split("_");
                if(str[0].equals("key")  && str[2].equals("value")){
                    try {
                        object.put(str[1],str[3]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.i("这是生成的object:",object.toString());
                    send_feedback_msg_handler.sendEmptyMessage(1);
                }else{
                    send_feedback_msg_handler.sendEmptyMessage(0);
                }
            }else if(msg.what == JSON_SAVE_FLAG){
                String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/json/";
                String fileName = msg.obj.toString().substring(10)+".json";
                Log.i("Json文件名：", fileName);
                writeTxtToFile(object.toString()
                        , filePath, fileName);
                object = new JSONObject();
                send_feedback_msg_handler.sendEmptyMessage(1);
            }else if(msg.what == OTHER_FLAG){
                send_feedback_msg_handler.sendEmptyMessage(0);
            }
        }
    };

    Handler pic_handler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SHOW_FLAG) {
                String imgStr = msg.obj.toString();
                String Pic_name = imgStr.substring(5, msg.obj.toString().length());
                Log.i("MainActivity", "这是图片名：" + Pic_name);

                String headPath = android.os.Environment.getExternalStorageDirectory()
                        + "/" + "DCIM" + "/" + "测试软件图片放置路径/";

                File file = new File(headPath + Pic_name + ".png");
                if (!file.exists()) {
                    Log.i("MainActivity", "不存在这个图片");
                    send_feedback_msg_handler.sendEmptyMessage(0);
                } else {
                    try {
                        Bitmap bmpDefaultPic;
                        bmpDefaultPic = BitmapFactory.decodeFile(
                                headPath + Pic_name + ".png", null);
                        imageView.setImageBitmap(bmpDefaultPic);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.i("MainActivity", "存在这个图片");
                    send_feedback_msg_handler.sendEmptyMessage(1);
                }
                imageView.setClickable(true);
            }else if(msg.what == GENERATE_FLAG){
                String imgStr = msg.obj.toString();
                String Pic_name = imgStr.substring(9, msg.obj.toString().length());
                String grayscale = Pic_name.substring(10);
                if(!isNumeric(grayscale)){
                    send_feedback_msg_handler.sendEmptyMessage(0);
                }else{
                    Log.i("MainActivity", "这是灰度值：" + grayscale);
                    int pic_grayscale = Integer.parseInt(grayscale);
                    if(pic_grayscale > 255 || pic_grayscale < 0){
                        send_feedback_msg_handler.sendEmptyMessage(0);
                    }else{
                        Mat mat = new Mat(H,W,CV_8UC1, new Scalar(pic_grayscale));
                        Bitmap bitmap = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mat,bitmap);
                        imageView.setImageBitmap(bitmap);
                        send_feedback_msg_handler.sendEmptyMessage(1);
                    }
                }
                imageView.setClickable(true);
            }else if(msg.what == LINE_FLAG){
                String Single_line = msg.obj.toString().substring(21);
                String str[] = Single_line.split("_");
                if(isLine(str)){
                    Mat mat = Mat.zeros(H,W, CvType.CV_8UC1);
                    if(str[1].equals("R")){
                        Imgproc.line(mat,new Point(0,Integer.parseInt(str[7])),new Point(W,Integer.parseInt(str[7])),new Scalar(Integer.parseInt(str[5])),Integer.parseInt(str[3]));
                    }else if(str[1].equals("C")){
                        Imgproc.line(mat,new Point(Integer.parseInt(str[7]),0),new Point(Integer.parseInt(str[7]),H),new Scalar(Integer.parseInt(str[5])),Integer.parseInt(str[3]));
                    }
                    Bitmap bitmap = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat,bitmap);
                    imageView.setImageBitmap(bitmap);
                    send_feedback_msg_handler.sendEmptyMessage(1);
                }else {
                    send_feedback_msg_handler.sendEmptyMessage(0);
                }
                imageView.setClickable(true);
            }else if(msg.what == DOT_FLAG){
                String str[] = msg.obj.toString().substring(20).split("_");
                if(isDot(str)){
                    Mat mat = Mat.zeros(H,W, CvType.CV_8UC1);
                    Imgproc.circle(mat,new Point(Integer.parseInt(str[5]),Integer.parseInt(str[7])),Integer.parseInt(str[1]),new Scalar(Integer.parseInt(str[3])),-1);
                    Bitmap bitmap = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat,bitmap);
                    imageView.setImageBitmap(bitmap);
                    send_feedback_msg_handler.sendEmptyMessage(1);
                }else {
                    send_feedback_msg_handler.sendEmptyMessage(0);
                }
                imageView.setClickable(true);
            }else if(msg.what == JSON_GENERATE_FLAG){
                String jsonFileName = msg.obj.toString().substring(14);
                Bitmap bitmap = generateJson(jsonFileName);
                imageView.setImageBitmap(bitmap);
                if(bitmap != null){
                    send_feedback_msg_handler.sendEmptyMessage(1);
                }else{
                    send_feedback_msg_handler.sendEmptyMessage(0);
                }
                imageView.setClickable(true);
            }else if(msg.what == OTHER_FLAG){
                send_feedback_msg_handler.sendEmptyMessage(0);
                imageView.setClickable(true);
            }
        }
    };

    Handler send_feedback_msg_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (msg.what == 0) {
                send_feedback_string = "to_PC_01_0";
                Log.i("send_handler","0");
            } else if (msg.what == 1) {
                send_feedback_string = "to_PC_01_1";
                Log.i("send_handler","1");
            }
        }
    };

    Handler socket_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                textView.setText("连接状态：已连接！\n" + "LocalAddress" + socket.getLocalAddress() + "\nLocalPort/" + socket.getLocalPort());
            } else {
                textView.setText("连接状态：未连接！");
            }
        }
    };

}



