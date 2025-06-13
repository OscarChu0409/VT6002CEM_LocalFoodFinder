package com.example.localfoodfinder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mVendorsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Default location (can be updated with user's location)
        LatLng defaultLocation = new LatLng(37.7749, -122.4194);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        
        // Load vendors from Firebase
        loadVendors();
    }
    
    private void loadVendors() {
        mVendorsRef = FirebaseDatabase.getInstance().getReference().child("vendors");
        mVendorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear previous markers
                mMap.clear();
                
                // Add markers for each approved vendor
                for (DataSnapshot vendorSnapshot : dataSnapshot.getChildren()) {
                    Boolean approved = vendorSnapshot.child("approved").getValue(Boolean.class);
                    
                    if (approved != null && approved) {
                        String businessName = vendorSnapshot.child("businessName").getValue(String.class);
                        Double latitude = vendorSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = vendorSnapshot.child("longitude").getValue(Double.class);
                        
                        if (businessName != null && latitude != null && longitude != null) {
                            LatLng vendorLocation = new LatLng(latitude, longitude);
                            mMap.addMarker(new MarkerOptions()
                                    .position(vendorLocation)
                                    .title(businessName));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}