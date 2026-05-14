package com.example.vidya_vahinitransportationassistance;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupEditableName();
        setupBottomNavigation();
        setupListeners();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation_profile);
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void setupEditableName() {
        TextView tvName = findViewById(R.id.tv_profile_name);

        SharedPreferences prefs = getSharedPreferences("vidya_prefs", MODE_PRIVATE);
        String savedName = prefs.getString("student_name", "Student Name");
        tvName.setText(savedName);

        tvName.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setText(tvName.getText());
            input.setSelectAllOnFocus(true);

            new AlertDialog.Builder(this)
                .setTitle("Enter Your Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        tvName.setText(name.toUpperCase());
                        prefs.edit().putString("student_name", name.toUpperCase()).apply();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_routes) {
                startActivity(new Intent(this, RouteSelectionActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        // "Logout" → back to Route Selection (no login flow anymore)
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            Toast.makeText(this, "Route changed!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RouteSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.btn_emergency_parent).setOnClickListener(v ->
            Toast.makeText(this, "Calling Parent...", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btn_emergency_transport).setOnClickListener(v ->
            Toast.makeText(this, "Calling Transport Dept...", Toast.LENGTH_SHORT).show()
        );
    }
}
