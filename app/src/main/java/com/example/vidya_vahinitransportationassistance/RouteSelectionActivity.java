package com.example.vidya_vahinitransportationassistance;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vidya_vahinitransportationassistance.adapters.RouteAdapter;
import com.example.vidya_vahinitransportationassistance.models.BusRoute;

import java.util.ArrayList;
import java.util.List;

public class RouteSelectionActivity extends AppCompatActivity {

    private RecyclerView rvRoutes;
    private RouteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_selection);

        rvRoutes = findViewById(R.id.rv_routes);
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));

        setupRoutes();
    }

    private void setupRoutes() {
        List<BusRoute> routeList = new ArrayList<>();
        routeList.add(new BusRoute("r1", "Route A: North Campus", "Bus #KA-01-1234", "15m"));
        routeList.add(new BusRoute("r2", "Route B: West Rural", "Bus #KA-01-5678", "25m"));
        routeList.add(new BusRoute("r3", "Route C: City Center", "Bus #KA-01-9012", "5m"));
        routeList.add(new BusRoute("r4", "Route D: East Suburbs", "Bus #KA-01-3456", "40m"));

        adapter = new RouteAdapter(routeList, route -> {
            Intent intent = new Intent(RouteSelectionActivity.this, MainActivity.class);
            intent.putExtra("route_name", route.getName());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        rvRoutes.setAdapter(adapter);
    }
}