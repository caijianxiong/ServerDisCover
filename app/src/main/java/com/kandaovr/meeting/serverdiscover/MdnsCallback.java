package com.kandaovr.meeting.serverdiscover;

import org.json.JSONArray;
import org.json.JSONObject;

public interface MdnsCallback {
    void onDeviceFind(JSONObject jsonArray);
}
