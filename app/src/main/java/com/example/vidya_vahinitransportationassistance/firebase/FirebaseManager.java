package com.example.vidya_vahinitransportationassistance.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference databaseReference;

    private FirebaseManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Live_Routes");
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public DatabaseReference getRouteReference(String routeId) {
        return databaseReference.child(routeId);
    }
}