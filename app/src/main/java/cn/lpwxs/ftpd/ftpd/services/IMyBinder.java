package cn.lpwxs.ftpd.ftpd.services;

import android.os.IBinder;

/**
 * Created by duke on 2017/6/26.
 */

public interface IMyBinder extends IBinder{
    void startFtpServer();
    void stopFtpServer();
}
