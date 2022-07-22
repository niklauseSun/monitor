package com.quick.jsbridge.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;

import quick.com.jsbridge.R;

public class UpdateIP extends FrmBaseActivity {
    private Button updateButton;
    private EditText localIp;
    private EditText remoteIp;
    private EditText socketIp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayout(R.layout.update_ip_view);

        SharedPreferences sp = getSharedPreferences("ipAddress", MODE_PRIVATE);
        String lP = sp.getString("localIp", "");
        String rP = sp.getString("remoteIp", "");
        String sP = sp.getString("socketIp", "");


        localIp = findViewById(R.id.textView);
        remoteIp = findViewById(R.id.textView2);
        socketIp = findViewById(R.id.socket_ip);

        localIp.setText(lP);
        remoteIp.setText(rP);
        socketIp.setText(sP);

        updateButton = findViewById(R.id.button2);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String local = localIp.getText().toString();
                String remote = remoteIp.getText().toString();
                String socket = socketIp.getText().toString();

                SharedPreferences.Editor editor = getSharedPreferences("ipAddress", MODE_PRIVATE).edit();
                if (local.length() != 0) {
                    editor.putString("localIp", local);
                }

                if (remote.length() != 0) {
                    editor.putString("remoteIp", remote);
                }

                if (socket.length() != 0) {
                    editor.putString("socketIp", socket);
                }


                editor.commit();
                SharedPreferences sp = getSharedPreferences("ipAddress", MODE_PRIVATE);
                String localUrl = sp.getString("localIp", "");
                nomalInit(localUrl);

            }
        });
    }

    private void nomalInit(String url) {
        Intent mintent = new Intent(UpdateIP.this, QuickWebLoader.class);

        QuickBean bean = new QuickBean(url);
        bean.pageStyle = -1;
        mintent.putExtra("bean", bean);
        mintent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        startActivity(mintent);
        this.finish();
    }
}
