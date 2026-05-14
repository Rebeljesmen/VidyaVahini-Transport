package com.example.vidya_vahinitransportationassistance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vidya_vahinitransportationassistance.adapters.NotificationAdapter;
import com.example.vidya_vahinitransportationassistance.models.Notification;
import com.example.vidya_vahinitransportationassistance.ui.views.BusTimelineView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private BusTimelineView busTimeline;
    private MaterialCardView cardPing, cardBreakdown, cardSos, cardSafeArrival, cardEmergencyAlert;
    private FrameLayout bannerBreakdown;
    private TextView tvActiveRoute, tvEta, tvNextStop, tvStatusIndicator, tvBusInfo;
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    
    private DatabaseReference routeRef;
    private DatabaseReference alertsRef;
    private String currentRouteId = "route_a"; // Default
    private int lastKnownStopIndex = -1;
    private String lastKnownStatus = "On Time";

    private GoogleMap mMap;
    private Marker busMarker;
    private LatLng[] stopCoords = {
        new LatLng(12.9716, 77.5946), // Village Square
        new LatLng(12.9720, 77.5950), // Primary School
        new LatLng(12.9730, 77.5960), // Market Jnc
        new LatLng(12.9740, 77.5970), // Forest Rd
        new LatLng(12.9750, 77.5980)  // College
    };

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get route info from Intent
        String routeName = getIntent().getStringExtra("route_name");
        if (routeName != null) {
            currentRouteId = routeName.contains("North") ? "route_a" : "route_b";
        }

        initViews();
        setupBottomNavigation();
        setupRecyclerView();
        setupFirebaseSync();
        setupListeners();
        
        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Subscribe to route updates for notifications
        FirebaseMessaging.getInstance().subscribeToTopic(currentRouteId);
    }

    private void initViews() {
        busTimeline = findViewById(R.id.bus_timeline);
        cardPing = findViewById(R.id.card_ping);
        cardBreakdown = findViewById(R.id.card_breakdown);
        cardSos = findViewById(R.id.card_sos);
        cardSafeArrival = findViewById(R.id.card_safe_arrival);
        cardEmergencyAlert = findViewById(R.id.card_emergency_alert);
        bannerBreakdown = findViewById(R.id.banner_breakdown);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        tvActiveRoute = findViewById(R.id.tv_active_route);
        tvEta = findViewById(R.id.tv_eta);
        tvNextStop = findViewById(R.id.tv_next_stop);
        tvStatusIndicator = findViewById(R.id.tv_status_indicator);
        tvBusInfo = findViewById(R.id.tv_bus_info);
        
        rvNotifications = findViewById(R.id.rv_notifications);
    }

    private void setupFirebaseSync() {
        routeRef = FirebaseDatabase.getInstance().getReference("Live_Routes").child(currentRouteId);
        alertsRef = routeRef.child("alerts");

        // 1. Sync Route Details and Live Data
        routeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update Details from Firebase
                    String name = snapshot.child("name").getValue(String.class);
                    String busNo = snapshot.child("busNumber").getValue(String.class);
                    String driver = snapshot.child("driverName").getValue(String.class);
                    
                    if (name != null) tvActiveRoute.setText(name);
                    if (busNo != null && driver != null) {
                        tvBusInfo.setText("Bus #" + busNo + " • Driver: " + driver);
                    }

                    // Update Live Tracking Data
                    Integer stopIndex = snapshot.child("currentStopIndex").getValue(Integer.class);
                    Double currentLat = snapshot.child("currentLatitude").getValue(Double.class);
                    Double currentLng = snapshot.child("currentLongitude").getValue(Double.class);
                    String status = snapshot.child("status").getValue(String.class);
                    
                    if (currentLat != null && currentLng != null) {
                        updateBusMarker(new LatLng(currentLat, currentLng));
                    }

                    if (stopIndex != null) {
                        float progress = stopIndex / 4.0f;
                        busTimeline.animateToProgress(progress);
                        
                        // If no specific lat/lng, fall back to stop coords
                        if (currentLat == null) updateBusMarker(stopCoords[stopIndex]);
                        
                        // Enable Safe Arrival button only when bus is at the last stop (College)
                        boolean isAtDestination = (stopIndex == 4);
                        cardSafeArrival.setEnabled(isAtDestination);
                        cardSafeArrival.setAlpha(isAtDestination ? 1.0f : 0.5f);
                        
                        // Detect Stop Crossed Event locally for visual feedback
                        if (lastKnownStopIndex != -1 && stopIndex > lastKnownStopIndex) {
                            Toast.makeText(MainActivity.this, "Bus crossed " + getStopName(stopIndex), Toast.LENGTH_SHORT).show();
                        }
                        lastKnownStopIndex = stopIndex;

                        // Dynamic ETA Logic based on average travel time
                        int totalRemainingTime = calculateRemainingTime(stopIndex);
                        tvEta.setText(totalRemainingTime > 0 ? totalRemainingTime + " mins" : "Arrived");
                        
                        // Update Next Stop Text
                        String[] stops = {"Village Square", "Primary School", "Market Jnc", "Forest Rd", "College"};
                        tvNextStop.setText(stopIndex < stops.length - 1 ? stops[stopIndex + 1] : "Destination");
                    }
                    
                    if (status != null) {
                        tvStatusIndicator.setText("• " + status);
                        boolean isCritical = status.equals("Critical");
                        tvStatusIndicator.setTextColor(isCritical ? android.graphics.Color.RED : android.graphics.Color.parseColor("#2E7D32"));
                        bannerBreakdown.setVisibility(isCritical ? View.VISIBLE : View.GONE);
                        
                        // Handle Emergency Alert Card
                        if (isCritical) {
                            if (cardEmergencyAlert.getVisibility() == View.GONE) {
                                cardEmergencyAlert.setVisibility(View.VISIBLE);
                                cardEmergencyAlert.setAlpha(0f);
                                cardEmergencyAlert.setTranslationY(-50f);
                                cardEmergencyAlert.animate()
                                        .alpha(1f)
                                        .translationY(0f)
                                        .setDuration(500)
                                        .start();
                            }
                        } else {
                            cardEmergencyAlert.setVisibility(View.GONE);
                        }

                        lastKnownStatus = status;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Sync failed: " + error.getMessage());
            }
        });

        // 2. Sync Recent Alerts from Firebase
        alertsRef.limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot alertSnap : snapshot.getChildren()) {
                    String title = alertSnap.child("title").getValue(String.class);
                    String message = alertSnap.child("message").getValue(String.class);
                    String time = alertSnap.child("timestamp").getValue(String.class);
                    if (title != null && message != null) {
                        notificationList.add(0, new Notification(title, message, time != null ? time : "Now"));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.simulate_movement) {
                Toast.makeText(this, "Starting Live Movement Simulation...", Toast.LENGTH_SHORT).show();
                startBusSimulation();
                return true;
            }
            return false;
        });

        cardPing.setOnClickListener(v -> {
            animateClick(v);
            routeRef.child("currentStopIndex").get().addOnSuccessListener(snapshot -> {
                int currentIndex = 0;
                if (snapshot.exists()) {
                    currentIndex = snapshot.getValue(Integer.class);
                }
                int nextIndex = (currentIndex + 1) % 5;
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStopIndex", nextIndex);
                updates.put("status", "On Time");
                updates.put("lastUpdated", ServerValue.TIMESTAMP);
                
                routeRef.updateChildren(updates);
                
                // Push alert to Firebase
                sendFirebaseAlert("ROUTE UPDATE", "Bus has crossed " + getStopName(nextIndex));
            });
        });

        cardBreakdown.setOnClickListener(v -> {
            animateClick(v);
            routeRef.child("status").setValue("Critical");
            sendFirebaseAlert("EMERGENCY", "Bus Breakdown reported for " + currentRouteId);
        });

        cardSos.setOnClickListener(v -> {
            animateClick(v);
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91112"));
            startActivity(intent);
        });

        cardSafeArrival.setOnClickListener(v -> {
            animateClick(v);
            
            // 1. Confirmation Dialog
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Safe Arrival")
                .setMessage("Clicking confirm will store your arrival timestamp and notify your registered guardians.")
                .setPositiveButton("Confirm Arrival", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("vidya_prefs", MODE_PRIVATE);
                    String studentName = prefs.getString("student_name", "Student");
                    
                    // 2. Prepare Data with double timestamps
                    DatabaseReference arrivalRef = FirebaseDatabase.getInstance().getReference("Safe_Arrival_Confirmations")
                            .child(currentRouteId)
                            .push();
                    
                    String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                    
                    Map<String, Object> arrivalData = new HashMap<>();
                    arrivalData.put("studentName", studentName);
                    arrivalData.put("status", "SAFELY_ARRIVED");
                    arrivalData.put("serverTimestamp", ServerValue.TIMESTAMP);
                    arrivalData.put("arrivalLocalTime", currentTime);
                    
                    // 3. Store in Firebase
                    arrivalRef.setValue(arrivalData).addOnSuccessListener(aVoid -> {
                        // 4. Multi-User Alert & Dashboard Update
                        sendFirebaseAlert("SAFE REACH", studentName + " reached safely at " + currentTime);
                        
                        Toast.makeText(this, "Success! Guardians have been notified.", Toast.LENGTH_LONG).show();
                        
                        // 5. Visual UI Lock
                        cardSafeArrival.setEnabled(false);
                        cardSafeArrival.setAlpha(0.3f);
                        tvStatusIndicator.setText("• Arrived @ " + currentTime);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        findViewById(R.id.btn_emergency_call).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"));
            startActivity(intent);
        });
    }

    private void sendFirebaseAlert(String title, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", title);
        alert.put("message", message);
        alert.put("timestamp", "Just Now"); // Simplified
        alertsRef.push().setValue(alert);
    }

    private void animateClick(View view) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        String[] stopNames = {"Village Square", "Primary School", "Market Jnc", "Forest Rd", "College"};
        for (int i = 0; i < stopCoords.length; i++) {
            mMap.addMarker(new MarkerOptions().position(stopCoords[i]).title(stopNames[i]));
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(stopCoords)
                .color(android.graphics.Color.parseColor("#6750A4"))
                .width(10);
        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopCoords[0], 15f));
    }

    private void updateBusMarker(LatLng position) {
        if (mMap == null || position == null) return;
        
        if (busMarker == null) {
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Live Bus")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        } else {
            busMarker.setPosition(position);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(position));
    }

    private boolean isSimulating = false;
    private android.os.Handler simHandler = new android.os.Handler();
    private int simStep = 0;

    private void startBusSimulation() {
        if (isSimulating) return;
        isSimulating = true;
        
        simHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isSimulating) return;

                // Move bus between Stop 0 and Stop 1 in 10 steps
                LatLng start = stopCoords[0];
                LatLng end = stopCoords[1];
                
                double lat = start.latitude + (end.latitude - start.latitude) * (simStep / 10.0);
                double lng = start.longitude + (end.longitude - start.longitude) * (simStep / 10.0);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentLatitude", lat);
                updates.put("currentLongitude", lng);
                updates.put("status", "Moving");
                
                routeRef.updateChildren(updates);
                
                simStep = (simStep + 1) % 11;
                simHandler.postDelayed(this, 3000); // Update every 3 seconds
            }
        });
    }

    private int calculateRemainingTime(int currentIndex) {
        int[] travelTimes = {5, 8, 7, 10}; 
        int totalMinutes = 0;
        for (int i = currentIndex; i < travelTimes.length; i++) {
            totalMinutes += travelTimes[i];
        }
        return totalMinutes;
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_routes) {
                startActivity(new Intent(this, RouteSelectionActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                Toast.makeText(this, "Alerts selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
    }

    private String getStopName(int index) {
        String[] stops = {"Village Square", "Primary School", "Market Jnc", "Forest Rd", "College"};
        if (index >= 0 && index < stops.length) return stops[index];
        return "Unknown Stop";
    }
}