package com.kandaovr.meeting.server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class ServerActivity extends AppCompatActivity {

    private JmdnsServer jmdnsServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_avtivity);

        jmdnsServer = new JmdnsServer(this);

        findViewById(R.id.btn_Start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jmdnsServer.start("MyJmdnsServer");
            }
        });

        findViewById(R.id.btn_Stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jmdnsServer.close();
            }
        });

    }
}