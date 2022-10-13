package com.kandaovr.meeting.server;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class JmdnsServer {


    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final static String REMOTE_TYPE = "_sample._tcp.local.";//注册的type是这个的话，你的发现设备的代码中的type也是这个才能找到这个设备。
    private int PORT = 1025;//默认端口号，可能会与实际端口有冲突，所以代码中加了动态获取端口号的逻辑

    private static JmDNS jmdns;

    private Context mContext;
    private final WifiManager wifiManager;

    public JmdnsServer(Context context) {
        this.mContext = context;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private ServiceInfo mServiceInfo;

    public void start(String serverName) {
        /**
         动态获取端口
         */
        int localPort = 0;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            localPort = serverSocket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (localPort != 0)
            PORT = localPort;
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                /**
                 这个ip是手机或盒子的ip地址
                 */
                try {
                    InetAddress ip = getLocalIpAddress(wifiManager);
                    jmdns = JmDNS.create(ip, "jmdnsSampleName");
                    final HashMap<String, String> values = new HashMap<String, String>();
                    values.put("test", "vlaue");

                    mServiceInfo = ServiceInfo.create(REMOTE_TYPE, serverName, PORT, 0, 0, values);
                    jmdns.registerService(mServiceInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void close() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (jmdns != null) {
                    try {
                        if (mServiceInfo != null) {
                            jmdns.unregisterService(mServiceInfo);
                        }
                        jmdns.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    private InetAddress getLocalIpAddress(WifiManager wifiManager) throws UnknownHostException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int intAddr = wifiInfo.getIpAddress();
        byte[] byteaddr = new byte[]{
                (byte) (intAddr & 255),
                (byte) (intAddr >> 8 & 255),
                (byte) (intAddr >> 16 & 255),
                (byte) (intAddr >> 24 & 255)};
        return InetAddress.getByAddress(byteaddr);
    }

}
