package cn.lpwxs.ftpd.ftpd;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import cn.lpwxs.ftpd.ftpd.services.FtpService;
import cn.lpwxs.ftpd.ftpd.services.IMyBinder;
import cn.lpwxs.ftpd.ftpd.services.Utils;

import static android.content.res.AssetManager.*;
import static android.text.format.Formatter.formatIpAddress;

/**
 * 1、小文件没问题，大文件会出现主线程操作过多异常. 已解决
 * 解决方法：开启线程
 */
public class FtpServerActivity extends AppCompatActivity {

    public static final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String ACTION_REMOVE_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 动态申请权限
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
//        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//        // We don't have permission so prompt the user
//            ActivityCompat.requestPermissions(
//                    activity,
//                    PERMISSIONS_STORAGE,
//                    REQUEST_EXTERNAL_STORAGE
//            );
//        }
        int permission = ActivityCompat.checkSelfPermission(activity,Manifest.permission_group.STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
        }
    }

    static {
//        System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    private Context mContext;
    private TextView tv;
    private FtpServiceConn conn;
    private Intent intent;
    private IMyBinder myBinder;
    private boolean isBinded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_server);
        tv = (TextView) findViewById(R.id.tips);
        mContext = this;
//        Intent lauchIntent = new Intent(getApplicationContext(), FtpServerActivity.class);
//        lauchIntent.setAction(Intent.ACTION_MAIN);
//        lauchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        addShortcut(getApplicationContext(), lauchIntent, getString(R.string.shortcut_title), false, R.mipmap.ic_launcher_round);
        verifyStoragePermissions(this);
    }


    public String getLocalIpAddress() {
        String ip = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isAvailable()){
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                //获取wifi服务
                WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if(wifiManager != null) {
                    //判断wifi是否开启
                    if (!wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(true);
                    }
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    int ipAddress = wifiInfo.getIpAddress();
                    System.out.println(ipAddress);
                    ip = Utils.formatIp2String(ipAddress);
                }
            }
        }
        return ip;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void ToggleServer(View view) {
        Button button = (Button) view;
        if (!isBinded) {
            if (conn == null) {
                conn = new FtpServiceConn();
            }
            String ip = getLocalIpAddress();
            if(ip == null){
                tv.setText("请连接WIFI重新打开");
            }else {
                //开启服务
                intent = new Intent(this, FtpService.class);
                Toast.makeText(mContext, R.string.service_opening, Toast.LENGTH_SHORT).show();
                isBinded = bindService(intent, conn, BIND_AUTO_CREATE);
                Log.i("isBinded", isBinded + "");

                if (isBinded) {

                    String tips = getString(R.string.tips) + "\n" + getString(R.string.prefix_ftp) + ip + ":" + Utils.port + "\n";
                    tv.setText(tips);
                    button.setText(R.string.close);
                } else {
                    tv.setText(R.string.service_never_open);
                }
            }
        } else {
            if (conn == null) {
                tv.setText(R.string.service_never_open);
            } else {
                if (myBinder != null) {
                    myBinder.stopFtpServer();
                }
                unbindService(conn);
                myBinder = null;
                conn = null;
                isBinded = false;
                tv.setText(R.string.service_closed);
            }
            button.setText(R.string.open);
        }
    }

    @Override
    protected void onStop() {
        if (myBinder != null) {
            myBinder.stopFtpServer();
        }
        if (conn != null) {
            unbindService(conn);
        }
        myBinder = null;
        conn = null;
        isBinded = false;
        super.onStop();
    }

    private class FtpServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (IMyBinder) service;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }


    /**
     * 添加快捷方式
     *
     * @param context      context
     * @param actionIntent 要启动的Intent
     * @param name         name
     * @param iconBitmap
     */
    public static void addShortcut(Context context, Intent actionIntent, String name,
                                   boolean allowRepeat, int iconBitmap) {
        Bitmap bitmap = BitmapFactory.decodeResource(Resources.getSystem(), iconBitmap);
        Intent addShortcutIntent = new Intent(ACTION_ADD_SHORTCUT);
        // 是否允许重复创建
        addShortcutIntent.putExtra("duplicate", allowRepeat);
        // 快捷方式的标题
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        // 快捷方式的图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        // 快捷方式的动作
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, actionIntent);
        context.sendBroadcast(addShortcutIntent);
    }
}
