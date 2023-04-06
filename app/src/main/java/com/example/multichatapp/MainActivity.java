package com.example.multichatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void buttonServerPressed(View v){
        Intent serverIntent = new Intent(this, ServerActivity.class);
        startActivity(serverIntent);
        finish();
    }

    public void buttonClientPressed(View v){
        Intent clientIntent = new Intent(this, ClientActivity.class);
        startActivity(clientIntent);
        finish();
    }
}