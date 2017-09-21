package cn.lpwxs.ftpd.ftpd.services;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by duke on 2017/6/26.
 */

public class FtpThread extends Thread {
    private static String PROFILE = Environment.getExternalStorageDirectory().getPath()+File.separator+"ftpd"+File.separator+"user.properties";
    private Context mContext;
    private FtpServer mFtpServer;


    public FtpThread(Context context) {
        this.mContext = context;
    }

    @Override
    public void run() {
        if (mContext != null && mFtpServer == null) {
            mFtpServer = startFtpServer(mContext);
            Log.i("ftpThread", "ftpserver start done ...");
        } else {
            Log.i("ftpThread", "ftpserver stared ... ");
        }
    }

    public void stopFtpServer() {
        if (mFtpServer != null) {
            mFtpServer.stop();
            Log.i("ftpThread", "stop done ...");
        } else {
            Log.i("ftpThread", "stoped ...");
        }
    }


    private FtpServer startFtpServer(Context context) {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();

        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File profile = new File(PROFILE);
        copyFilesFassets(context,profile.getName(),PROFILE);
        userManagerFactory.setFile(profile);
        serverFactory.setUserManager(userManagerFactory.createUserManager());

        factory.setPort(Utils.port);

        serverFactory.addListener("default", factory.createListener());
        FtpServer server = serverFactory.createServer();
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return server;
    }


    /**
     * 从assets目录中复制整个文件夹内容
     *
     * @param context Context 使用CopyFiles类的Activity
     * @param oldPath String  原文件路径  如：/aa
     * @param newPath String  复制后路径  如：xx:/bb/cc
     */
    public void copyFilesFassets(Context context, String oldPath, String newPath) {
        try {
            File desfile = new File(newPath);
            if (!desfile.exists()) {
                desfile.createNewFile();
                Log.i("copy user.properties:", "oldpath=" + oldPath + ",newPath=" + newPath);
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(desfile);
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }else {
                Log.i("profile is exists.",newPath);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //如果捕捉到错误则通知UI线程
//            MainActivity.handler.sendEmptyMessage(COPY_FALSE);
        }
    }
}
