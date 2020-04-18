package com.mmoush.covid;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class HomeActivity extends AppCompatActivity {

    private EditText editTextInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        editTextInput = findViewById(R.id.edit_text_input);
    }

    public void startService(View v){
        String input = editTextInput.getText().toString();
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("interval", input);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View v){
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }
}
