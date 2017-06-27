package cn.lpwxs.ftpd.ftpd.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

/**
 *
 * Created by duke on 2017/6/26.
 *
 */

public class FtpService extends Service {

    private FtpThread ftpThread;

    public FtpService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        startFtp();
        return new MyBinder();
    }

    private void startFtp() {
        if (ftpThread == null) {
            ftpThread = new FtpThread(this);
            ftpThread.start();
            long tid = ftpThread.getId();
            Log.i("FtpService:", "ftpserver start done ... tid="+tid);
        } else {
            Log.i("FtpService:", "ftpserver started ... ");
        }
    }

    private void stopFtp(){
        if (ftpThread != null) {
            ftpThread.stopFtpServer();
            long tid = ftpThread.getId();
            try {
                Runtime.getRuntime().exec("kill -9 "+tid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ftpThread = null;
            Log.i("FtpService:", "mFtpServer stop done ...  kill tid="+tid);
        } else {
            Log.i("FtpService:", "mFtpServer stoped ... ");
        }
    }

    private class MyBinder extends Binder implements IMyBinder {

        @Override
        public void startFtpServer() {
            startFtp();
        }

        @Override
        public void stopFtpServer() {
            stopFtp();
        }

    }

}
