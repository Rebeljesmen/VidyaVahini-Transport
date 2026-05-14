package com.example.vidya_vahinitransportationassistance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vidya_vahinitransportationassistance.R;
import com.example.vidya_vahinitransportationassistance.models.BusRoute;
import com.google.android.material.chip.Chip;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {

    private List<BusRoute> routes;
    private OnRouteSelectedListener listener;

    public interface OnRouteSelectedListener {
        void onRouteSelected(BusRoute route);
    }

    public RouteAdapter(List<BusRoute> routes, OnRouteSelectedListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRoute route = routes.get(position);
        holder.tvName.setText(route.getName());
        holder.tvBusNumber.setText(route.getBusNumber());
        holder.chipEta.setText(route.getEta() + " ETA");
        
        // Entrance Animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(50f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(position * 100L)
                .start();

        holder.btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteSelected(route);
            }
        });
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBusNumber;
        Chip chipEta;
        Button btnSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_route_name);
            tvBusNumber = itemView.findViewById(R.id.tv_bus_number);
            chipEta = itemView.findViewById(R.id.chip_eta);
            btnSelect = itemView.findViewById(R.id.btn_select_route);
        }
    }
}