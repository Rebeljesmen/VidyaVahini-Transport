package com.example.vidya_vahinitransportationassistance;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class VidyaVahiniApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase persistence for offline dynamic tracking
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}