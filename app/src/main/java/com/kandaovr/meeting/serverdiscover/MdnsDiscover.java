package com.kandaovr.meeting.serverdiscover;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.kandaovr.meeting.mylibrary.NetUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class MdnsDiscover {

    private String TAG = this.getClass().getSimpleName();


    private Context mContext;
    private String mServiceName;
    private JmDNS mJmdns;
    private MdnsSearchThread mSearchThread;
    private Map<String, JSONObject> jsonMap = new HashMap<>();

    public MdnsDiscover(Context context) {
        mContext = context;
    }

    public void startSearch(String serviceName, MdnsCallback callback) {
        if (mSearchThread != null && mSearchThread.isRunning()) {
            if (Objects.equals(mServiceName, serviceName)) {
                return;
            }
            mSearchThread.interrupt();
        }
        Log.i(TAG, "startSearch: ");
        mServiceName = serviceName;
        mSearchThread = new MdnsSearchThread(callback);
        mSearchThread.start();
    }

    public void stopSearch() {
        jsonMap.clear();
        if (mSearchThread != null && mSearchThread.isRunning()) {
            mSearchThread.interrupt();
            mSearchThread = null;
        }
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

    private class MdnsSearchThread extends Thread {
        private MdnsCallback mCallback;

        public MdnsSearchThread(MdnsCallback mCallback) {
            this.mCallback = mCallback;
        }

        private volatile boolean running = false;

        @Override
        public void run() {
            Log.i(TAG, "run: ");
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wifiManager != null;
            // wifi默认过滤多播
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock(getClass().getName());
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();//to receive multicast packets
            try {
                ServiceListener listener = new JmdnsListener(mCallback);
                boolean crash = false;
                int netPort = 0;
                String localIp = NetUtils.getLocalIpAddress();
                // 不用循环查询，一次搜索所有结果
                try {
                    if (crash) {
                        Thread.sleep(3000L);
                    }
                    InetAddress localAddr = InetAddress.getByName(localIp);
                    Log.i(TAG, "try run addr: " + localAddr);
                    mJmdns = JmDNS.create(localAddr);
                    mJmdns.addServiceListener(mServiceName, listener);
                    Thread.sleep(3000L);
                    crash = false;
                } catch (Exception e) {
                    Log.w(TAG, "run inner: ", e);
                    crash = true;
                } finally {
                    mJmdns.removeServiceListener(mServiceName, listener);
                    try {
                        mJmdns.close();
                    } catch (IOException e) {
                        Log.w(TAG, "run inner 02: ", e);
                        crash = false;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "run: ", e);
            } finally {
                Log.i(TAG, "run: search end");
                multicastLock.release();
            }
        }

        @Override
        public synchronized void start() {
            running = true;
            super.start();
        }

        @Override
        public void interrupt() {
            running = false;
            mCallback = null;
            super.interrupt();
        }

        public boolean isRunning() {
            return running;
        }
    }

    private class JmdnsListener implements ServiceListener {
        private MdnsCallback mCallback;

        public JmdnsListener(MdnsCallback mCallback) {
            this.mCallback = mCallback;
        }

        public void serviceAdded(ServiceEvent ev) {
            Log.i(TAG, String.format("serviceAdded type:%s,name:%s ", ev.getType(), ev.getName()));
//            mJmdns.requestServiceInfo(ev.getType(), ev.getName(), 1);
        }

        public void serviceRemoved(ServiceEvent ev) {
            Log.i(TAG, "serviceRemoved type: " + ev.getType() + " name:" + ev.getName());
            jsonMap.remove(ev.getName());
        }

        public void serviceResolved(ServiceEvent ev) {
            // 新设备
            JSONObject jsonObj = toJsonObject(ev.getInfo());
            Log.i(TAG, "serviceResolved: add");
            if (mCallback != null) {
                if (jsonObj == null) {
                    Log.w(TAG, "serviceResolved: jsonObj is null");
                    return;
                }
                // 重开线程回调
                Log.i(TAG, "serviceResolved: ");
                mCallback.onDeviceFind(jsonObj);
            }
        }
    }


    /**
     * mDNS数据格式解析
     */
    private JSONObject toJsonObject(ServiceInfo sInfo) {
        JSONObject jsonObj;
        try {
            jsonObj = new JSONObject();
            String ipv4 = "";
            if (sInfo.getInet4Addresses().length > 0) {
                ipv4 = sInfo.getInet4Addresses()[0].getHostAddress();
            }

            jsonObj.put("Name", sInfo.getName());
            jsonObj.put("IP", ipv4);
            jsonObj.put("Port", sInfo.getPort());

            byte[] allInfo = sInfo.getTextBytes();
            int allLen = allInfo.length;
            byte fLen;
            for (int index = 0; index < allLen; index += fLen) {
                fLen = allInfo[index++];
                byte[] fData = new byte[fLen];
                System.arraycopy(allInfo, index, fData, 0, fLen);

                String fInfo = new String(fData, StandardCharsets.UTF_8);
                if (fInfo.contains("=")) {
                    String[] temp = fInfo.split("=");
                    jsonObj.put(temp[0], temp[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonObj = null;
        }
        return jsonObj;
    }

}


