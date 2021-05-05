package com.example.socketdemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private EditText ipAddress;
    private EditText sortAddress;
    private ImageView imageView;
    private TextView textView;
    private Button button1;
    private Button button2;

    private int socket_flag = 0;

    Socket socket;

    InputStream inputStream;
    OutputStream outputStream;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,

            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;


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
        sortAddress = (EditText) findViewById(R.id.port_text);
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

    public int isServerClose(Socket socket){
        try{
            socket.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
            return 0;
        }catch(Exception se){
            return 1;
        }
    }

    //socket连接
    private void initSocket() {
        String ip = ipAddress.getText().toString();
        String temp = sortAddress.getText().toString();
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

                    Message socket_msg = new Message();
                    socket_msg.what = 1;
                    socket_flag = 1;
                    socket_msg.obj = socket_flag;
                    socket_handler.sendMessage(socket_msg);

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
                        if (responseInfo.indexOf("show_") == 0) {
                            pic_msg.what = 1;
                            pic_msg.obj = responseInfo;
                            pic_handler.sendMessage(pic_msg);
                            Log.i("有效输入", responseInfo);
                        }
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

    Handler pic_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                String imgStr = msg.obj.toString();
                String Pic_name = imgStr.substring(5, msg.obj.toString().length());
                Log.i("MainActivity", "这是图片名：" + Pic_name);
                String headPath = android.os.Environment.getExternalStorageDirectory()
                        + "/" + "DCIM" + "/" + "测试软件图片放置路径/";
//                String UtfPath = null;
//                try {
//                    UtfPath = new String(headPath.getBytes("UTF-8"),"UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
                File file=new File(headPath +Pic_name + ".png");
                if(!file.exists())
                {
                    Log.i("MainActivity", "不存在这个图片" );
                    Thread Th = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                OutputStream outputStream = socket.getOutputStream();
                                String tempStr = "to_PC_01_0";
                                outputStream.write(tempStr.getBytes("utf-8"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    Th.start();
                }else{
                    Bitmap bmpDefaultPic;
                    bmpDefaultPic = BitmapFactory.decodeFile(
                            headPath +Pic_name + ".png", null);
                    imageView.setImageBitmap(bmpDefaultPic);
                    Log.i("MainActivity", "存在这个图片" );
                    Thread Th = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                OutputStream outputStream = socket.getOutputStream();
                                String tempStr = "to_PC_01_1";
                                outputStream.write(tempStr.getBytes("utf-8"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    Th.start();
                    try {
                        Th.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                imageView.setClickable(true);
            }
        }
    };

    Handler socket_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);  // todo : Message What
            int temp = Integer.parseInt(String.valueOf(msg.obj));
            if(temp == 1) {
                textView.setText("连接状态：已连接！\n"+ "LocalAddress"+socket.getLocalAddress()+"\nLocalPort/"+socket.getLocalPort());
            }else{
                textView.setText("连接状态：未连接！");
            }
        }
    };

}