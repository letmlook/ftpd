package cn.lpwxs.ftpd.ftpd.services;

import java.net.Inet4Address;

/**
 * Created by duke on 2017/6/26.
 */

public class Utils {
    public static final int port = 2321;

    public static String formatIp2String(int address){
        byte[] addr = new byte[4];

        addr[0] = (byte) ((address >>> 24) & 0xFF);
        addr[1] = (byte) ((address >>> 16) & 0xFF);
        addr[2] = (byte) ((address >>> 8) & 0xFF);
        addr[3] = (byte) (address & 0xFF);


        return (addr[3] & 0xff) + "." + (addr[2] & 0xff) + "." + (addr[1] & 0xff) + "." + (addr[0] & 0xff);
    }
}
