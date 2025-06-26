package com.example.localfoodfinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mVendorsRef;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Location lastKnownLocation;
    private static final float DEFAULT_ZOOM = 15f;
    private double searchRadiusKm = 10.0;
    private SeekBar seekBarRadius;
    private TextView textViewSearchRadius;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize search radius controls
        seekBarRadius = view.findViewById(R.id.seekBarRadius);
        textViewSearchRadius = view.findViewById(R.id.textViewSearchRadius);

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                searchRadiusKm = progress > 0 ? progress : 1; // Minimum 1km
                textViewSearchRadius.setText("Search Radius: " + (int)searchRadiusKm + "km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Reload vendors with new radius
                loadVendors();
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable my location button if permission is granted
        enableMyLocation();

        // Set up map UI settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Set up map click listener for user interaction feedback
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    // User is interacting with the map
                    Toast.makeText(requireContext(), "Exploring the area...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up marker click listener
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // Show vendor details when marker is clicked
                Toast.makeText(requireContext(), "Selected: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
                return false; // Return false to allow default behavior (info window)
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                lastKnownLocation = location;
                                LatLng currentLocation = new LatLng(
                                        location.getLatitude(),
                                        location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        currentLocation, DEFAULT_ZOOM));

                                // Load vendors after getting location
                                loadVendors();

                                // Update user status in Firebase (optional)
                                updateUserViewingStatus(true);
                            } else {
                                // If location is null, load vendors anyway but don't filter by distance
                                loadVendors();
                                Toast.makeText(requireContext(),
                                        "Unable to get your location. Showing all vendors.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied. Some features may be limited.",
                        Toast.LENGTH_SHORT).show();
                // Load vendors anyway, but don't filter by distance
                loadVendors();
            }
        }
    }

    private void loadVendors() {
        mVendorsRef = FirebaseDatabase.getInstance().getReference().child("vendors");
        mVendorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear previous markers
                mMap.clear();

                int vendorCount = 0;
                int nearbyCount = 0;

                // Add markers for each approved vendor
                for (DataSnapshot vendorSnapshot : dataSnapshot.getChildren()) {
                    Boolean approved = vendorSnapshot.child("approved").getValue(Boolean.class);

                    if (approved != null && approved) {
                        String businessName = vendorSnapshot.child("businessName").getValue(String.class);
                        Double latitude = vendorSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = vendorSnapshot.child("longitude").getValue(Double.class);
                        String description = vendorSnapshot.child("description").getValue(String.class);

                        if (businessName != null && latitude != null && longitude != null) {
                            LatLng vendorLocation = new LatLng(latitude, longitude);

                            // Check if vendor is nearby (if we have user location)
                            boolean isNearby = false;
                            float[] distance = new float[1];

                            if (lastKnownLocation != null) {
                                Location.distanceBetween(
                                        lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                        latitude, longitude, distance);

                                // Convert meters to kilometers
                                isNearby = distance[0] / 1000 <= searchRadiusKm;

                                if (isNearby) {
                                    nearbyCount++;
                                }
                            }

                            // Create marker with snippet (description)
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(vendorLocation)
                                    .title(businessName);

                            if (description != null) {
                                markerOptions.snippet(description);
                            }

                            // Add marker to map
                            mMap.addMarker(markerOptions);
                            vendorCount++;
                        }
                    }
                }

                // Show toast with vendor count
                if (lastKnownLocation != null && nearbyCount > 0) {
                    Toast.makeText(requireContext(),
                            "Found " + nearbyCount + " vendors within " + searchRadiusKm + "km",
                            Toast.LENGTH_SHORT).show();
                } else if (vendorCount > 0) {
                    Toast.makeText(requireContext(),
                            "Showing " + vendorCount + " vendors",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(),
                            "No vendors found in your area",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireContext(),
                        "Error loading vendors: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update user status in Firebase (optional)
    private void updateUserViewingStatus(boolean isViewing) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUser.getUid()).child("isViewingMap");
            userStatusRef.setValue(isViewing);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserViewingStatus(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        updateUserViewingStatus(false);
    }
}
