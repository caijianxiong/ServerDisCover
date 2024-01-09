package com.kandaovr.meeting.server;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.kandaovr.meeting.mylibrary.NetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

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
        mExecutorService.execute(() -> {
            /**
             这个ip是手机或盒子的ip地址
             */
            try {
                String ip = NetUtils.getLocalIpAddress();
                InetAddress inetAddress = InetAddress.getByName(ip);
                Log.i("TAG", "run: ip:" + inetAddress.getHostAddress());
                jmdns = JmDNS.create(inetAddress, "jmdnsSampleName");
                final HashMap<String, String> values = new HashMap<String, String>();
                values.put("test", "vlaue");
                values.put("isUsing", "true");
                jmdns.addServiceListener(REMOTE_TYPE, new ServiceListener() {
                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        Log.i("TAG", "serviceAdded: ");
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {
                        Log.i("TAG", "serviceRemoved: ");
                    }

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        Log.i("TAG", "serviceResolved: ");
                    }
                });
                mServiceInfo = ServiceInfo.create(REMOTE_TYPE, serverName, PORT, 0, 0, false, values);
                jmdns.registerService(mServiceInfo);

            } catch (Exception e) {
                Log.w("TAG", "start: ", e);
            }
        });
    }


    public void close() {
        mExecutorService.execute(() -> {
            if (jmdns != null) {
                try {
                    if (mServiceInfo != null) {
                        jmdns.unregisterService(mServiceInfo);
                    }
                    jmdns.close();
                } catch (IOException e) {
                    Log.w("TAG", "close: ", e);
                }
            }
        });
    }

}
