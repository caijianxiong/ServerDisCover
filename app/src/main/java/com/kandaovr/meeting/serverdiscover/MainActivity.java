package com.kandaovr.meeting.serverdiscover;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements MdnsCallback {

    private MdnsDiscover jmdnsClient;
    private String TAG = this.getClass().getSimpleName();
    //    private static final String serverType = "_airplay._tcp.local.";
    private static final String serverType = "_sample._tcp.local.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        findViewById(R.id.btn_Start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: ");
                jmdnsClient = new MdnsDiscover(MainActivity.this);
                jmdnsClient.startSearch(serverType, MainActivity.this);
            }
        });


        findViewById(R.id.btn_Stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jmdnsClient.stopSearch();
            }
        });


    }


    @Override
    protected void onStop() {
        super.onStop();
        jmdnsClient.stopSearch();
    }

    @Override
    public void onDeviceFind(JSONObject jsonObject) {
        // 子线程
//            @SuppressLint("DefaultLocale") String str = String.format("onDeviceFind: %s, Ip:%s, Port:%d",
//                    jsonObject.getString("Name"), jsonObject.getString("IP"), jsonObject.getInt("Port"));
        Log.i(TAG, "onDeviceFind:" + jsonObject);
    }
}