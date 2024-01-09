package com.kandaovr.meeting.mylibrary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetUtils {

    private static String TAG = "NetWorkUtil";

    public static String getLocalIpAddress() throws RemoteException {

        String ip = "0.0.0.0";

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration != null && enumeration.hasMoreElements()) {
                NetworkInterface intf = enumeration.nextElement();
                if (!intf.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr != null && enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.getHostAddress().equals("192.168.49.1")) {
//                        Log.i("TAG", "getLocalIpAddress: 192.168.49.1 ignore");
                        continue;
                    }
                    // wifi eth优先取以太网
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress();
                        if (intf.getName().contains("eth0")) {
                            Log.i(TAG, "getLocalIpAddress ip: " + ip + " --- name:" + intf.getName());
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getLocalIpAddress: ", e);
        }
        return ip;
    }

    public static boolean hasSupportEth() throws RemoteException {
        return true;
    }

    public static boolean hasSupportWifi() throws RemoteException {
        return true;
    }

    public static String getMacAddress() {
        String mackAddress = "00:00:00:00:00:00";
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (nif.isUp() && (nif.getName().equalsIgnoreCase("wlan0") || nif.getName().equalsIgnoreCase("eth0"))) {
                    mackAddress = getMacAddressByHardwareAddress(nif.getHardwareAddress());
                    // 优先取以太网地址
                    if (nif.getName().equalsIgnoreCase("eth0")) {
                        return mackAddress;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mackAddress;
    }

    private static String getMacAddressByHardwareAddress(byte[] bytes) {
        if (bytes == null) {
            return "00:00:00:00:00:00";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            // 10进制->16进制
            String byteToHex = Integer.toHexString(bytes[i] & 0xff);
            sb.append(byteToHex);
            if (i < bytes.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    /**
     * 获取有线mac地址
     *
     * @return
     */
    private static String getEth0MacAddress() {
        String mac = "";
        try {
            InputStream inputStream = new FileInputStream(
                    "/sys/class/net/eth0/address");
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(reader);
            mac = br.readLine();
        } catch (Exception e) {
            return mac;
        }
        return mac;
    }

    /**
     * wifi已经打开，直接读取文件
     * 获取无线的mac地址
     *
     * @return
     */
    private static String getWifiMacAddress() {
        String mac = "";
        try {
            InputStream inputStream = new FileInputStream("/sys/class/net/wlan0/address");
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(reader);
            mac = br.readLine();
        } catch (Exception e) {
            return mac;
        }
        return mac;
    }


    public static int getNetworkType(Context context) throws RemoteException {
        ConnectivityManager connectivityManager = context
                .getSystemService(ConnectivityManager.class);
        NetworkInfo networkInfo = connectivityManager
                .getNetworkInfo(connectivityManager.getActiveNetwork());
        if (networkInfo != null) {
            return networkInfo.getType();
        }
        return ConnectivityManager.TYPE_NONE;
    }

    public static boolean setNetworkEnable(Context context, int netWorkType, boolean enable) throws RemoteException {
        if (netWorkType == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifiManager = context.getSystemService(WifiManager.class);
            wifiManager.setWifiEnabled(enable);
            return true;
        } else if (netWorkType == ConnectivityManager.TYPE_ETHERNET) {
            if (enable) {
                SystemProperties.set("vendor.kandao.net", "1");
            } else {
                SystemProperties.set("vendor.kandao.net", "2");
            }
            return true;
        }
        Log.w(TAG, "Invalid network type");
        return false;
    }

    public static boolean getNetworkEnable(Context context, int netWorkType) throws RemoteException {
        if (netWorkType == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifiManager = context.getSystemService(WifiManager.class);
            return wifiManager.isWifiEnabled();
        } else if (netWorkType == ConnectivityManager.TYPE_ETHERNET) {
            // TODO: 2022/10/28
//            String status = execShellCmd("cat /sys/class/net/eth0/carrier");
//            return status != null && !status.contains("carrier");
        }
        Log.w(TAG, "Invalid network type");
        return false;
    }

    public static Map getNetworkInfo(Context context, int netWorkType) throws RemoteException {
        Map<String, Object> map = new HashMap<>();
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        // TODO: 2022/10/28
//        LinkProperties linkProperties = connectivityManager.getLinkProperties(netWorkType);
//        if (netWorkType == ConnectivityManager.TYPE_WIFI && linkProperties != null) {
//            WifiManager wifiManager = context.getSystemService(WifiManager.class);
//            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
//            if (wifiInfo != null && dhcpInfo != null) {
//                WifiConfiguration configuration = Utils.getWifiConfigurationForNetworkId(wifiManager,
//                        wifiInfo.getNetworkId());
//                if (configuration != null) {
//                    map.put("ssid", configuration.SSID);
//                    map.put("password", Utils.getPresharedKey(wifiManager, configuration));
//                }
//                map.put("mode", dhcpInfo.leaseDuration == 0 ? 1 : 0);
//                map.put("ipaddr", Utils.intToInetAddr(dhcpInfo.ipAddress));
//                map.put("gateway", Utils.intToInetAddr(dhcpInfo.gateway));
//                map.put("dns1", Utils.intToInetAddr(dhcpInfo.dns1));
//                map.put("dns2", Utils.intToInetAddr(dhcpInfo.dns2));
//            }
//            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
//                final InetAddress address = linkAddress.getAddress();
//                if (address instanceof Inet4Address) {
//                    map.put("netmask", Utils.getNetMask(linkAddress.getPrefixLength()));
//                    break;
//                }
//            }
//        } else if (netWorkType == ConnectivityManager.TYPE_ETHERNET && linkProperties != null) {
//            fixInvokeHideApiError();
//            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
//                final InetAddress address = linkAddress.getAddress();
//                if (address instanceof Inet4Address) {
//                    map.put("ipaddr", address.getHostAddress());
//                    map.put("netmask", Utils.getNetMask(linkAddress.getPrefixLength()));
//                    break;
//                }
//            }
//            for (RouteInfo routeInfo : linkProperties.getRoutes()) {
//                if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
//                    map.put("gateway", routeInfo.getGateway().getHostAddress());
//                }
//            }
//            for (InetAddress inetAddress : linkProperties.getDnsServers()) {
//                if (inetAddress instanceof Inet4Address) {
//                    if (map.get("dns1") == null) {
//                        map.put("dns1", inetAddress.getHostAddress());
//                    } else {
//                        map.put("dns2", inetAddress.getHostAddress());
//                        break;
//                    }
//                }
//            }
//        } else {
//            Log.w(TAG, "Invalid network type");
//        }
        return map;
    }


    public static void setEthStaticIp(String ipAddr, String gateWay, String mask,
                                      String dns1, String dns2) {
        // TODO: 2022/10/28

    }

    public static void setEthDhcp(Context context) {
        @SuppressLint("WrongConstant")
        EthernetManager manager = (EthernetManager) context.getSystemService("ethernet");
        IpConfiguration configuration = manager.getConfiguration("eth0");
        configuration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
        configuration.setStaticIpConfiguration(null);
        manager.setConfiguration("eth0", configuration);
    }

    public static void setWifiStaticConect(Context context, String bssid, String ssid, int security, String password,
                                           String ipAddr, String gateWay, String mask,
                                           String dns1, String dns2) {

        if (ssid == null) {
            Log.e(TAG, "Ssid can't be null");
            return;
        }
        if (ipAddr == null) {
            Log.e(TAG, "IP can't be null");
            return;
        }
        if (gateWay == null) {
            Log.e(TAG, "Gateway can't be null");
            return;
        }
        if (mask == null) {
            Log.e(TAG, "Mask can't be null");
            return;
        }
        switch (security) {
            case 0:
                security = WifiConfiguration.KeyMgmt.NONE;
                break;
            case 1:
                security = WifiConfiguration.KeyMgmt.IEEE8021X;
                break;
            case 2:
                security = WifiConfiguration.KeyMgmt.WPA_PSK;
                break;
            case 3:
                security = WifiConfiguration.KeyMgmt.WPA_EAP;
                break;
            default:
                Log.e(TAG, "invalid security type");
                return;
        }
        // TODO: 2022/10/28
//        WifiManager wifiManager = context.getSystemService(WifiManager.class);
//        wifiManager.setWifiEnabled(true);
//        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
//        for (WifiConfiguration config : configs) {
//            if (config.SSID.equals("\"" + ssid + "\"")) {
//                wifiManager.removeNetwork(config.networkId);
//                break;
//            }
//        }
//        WifiConfiguration configuration = Utils.generateWifiConfig(bssid, ssid, password, security);
//        StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
//        InetAddress inetAddress = Utils.getInetAddress(ipAddr);
//        staticIpConfiguration.ipAddress = new LinkAddress(inetAddress, Utils.getNetPrefix(mask));
//        staticIpConfiguration.gateway = Utils.getInetAddress(gateWay);
//        if (Utils.checkIpAddrValid(dns1)) {
//            staticIpConfiguration.addDnsServer(Utils.getInetAddress(dns1));
//            if (Utils.checkIpAddrValid(dns2)) {
//                staticIpConfiguration.addDnsServer(Utils.getInetAddress(dns2));
//            }
//        }
//        configuration.setStaticIpConfiguration(staticIpConfiguration);
//        configuration.setIpAssignment(IpConfiguration.IpAssignment.STATIC);
//        wifiManager.enableNetwork(wifiManager.addNetwork(configuration), true);

    }

    public static void setWifiDhcpConect(Context context, String bssid, String ssid, int security, String password)
            throws RemoteException {
        if (ssid == null) {
            Log.e(TAG, "Ssid can't be null");
            return;
        }
        switch (security) {
            case 0:
                security = WifiConfiguration.KeyMgmt.NONE;
                break;
            case 1:
                security = WifiConfiguration.KeyMgmt.IEEE8021X;
                break;
            case 2:
                security = WifiConfiguration.KeyMgmt.WPA_PSK;
                break;
            case 3:
                security = WifiConfiguration.KeyMgmt.WPA_EAP;
                break;
            default:
                Log.e(TAG, "invalid security type");
                return;
        }
        WifiManager wifiManager = context.getSystemService(WifiManager.class);
        wifiManager.setWifiEnabled(true);
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.removeNetwork(config.networkId);
                break;
            }
        }
        // TODO: 2022/10/28
//        WifiConfiguration configuration = Utils.generateWifiConfig(bssid, ssid, password, security);
//        configuration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
//        wifiManager.enableNetwork(wifiManager.addNetwork(configuration), true);
    }


}
