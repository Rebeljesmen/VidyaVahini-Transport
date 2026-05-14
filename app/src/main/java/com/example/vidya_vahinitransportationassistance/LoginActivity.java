package com.example.vidya_vahinitransportationassistance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginBtn = findViewById(R.id.btn_login);
        Button registerBtn = findViewById(R.id.btn_register);
        TextView forgotPassword = findViewById(R.id.tv_forgot_password);

        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RouteSelectionActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        registerBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Redirecting to Register...", Toast.LENGTH_SHORT).show();
        });

        forgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Redirecting to Forgot Password...", Toast.LENGTH_SHORT).show();
        });
    }
}