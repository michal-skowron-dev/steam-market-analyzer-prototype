package com.am.steammarketanalyzer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText editTextInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextInput = findViewById(R.id.editTextInput);
        editTextInput.setText(getSharedPreferences("sharedPreferences", MODE_PRIVATE).getString("input", ""));
    }

    @Override
    public void onStop() {
        super.onStop();
        getSharedPreferences("sharedPreferences", MODE_PRIVATE).edit().putString("input", editTextInput.getText().toString()).apply();
    }

    public void startService(View v) {
        String input = editTextInput.getText().toString();

        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra(getString(R.string.input_extra), input);

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
    }
}