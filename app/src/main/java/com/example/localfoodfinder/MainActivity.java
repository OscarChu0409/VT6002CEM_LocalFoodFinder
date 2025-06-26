package com.example.localfoodfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private TextView textViewWelcome;
    private String userRole;
    private Button buttonUpdateLocation;
    private Button buttonManageMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        textViewWelcome = findViewById(R.id.textViewWelcome);
        buttonUpdateLocation = findViewById(R.id.buttonUpdateLocation);
        buttonManageMenu = findViewById(R.id.buttonManageMenu);

        // Initially hide the update location and manage menu buttons
        buttonUpdateLocation.setVisibility(View.GONE);
        buttonManageMenu.setVisibility(View.GONE);

        // Set click listener for update location button
        buttonUpdateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VendorSetupActivity.class));
            }
        });

        // Set click listener for manage menu button
        buttonManageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MenuUploadActivity.class));
            }
        });

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to login
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Get user data from database
        mUserRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUser.getUid());

        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    userRole = dataSnapshot.child("role").getValue(String.class);

                    if (name != null) {
                        String welcomeText = "Welcome, " + name + "!";
                        if (userRole != null && userRole.equals("vendor")) {
                            welcomeText += " (Vendor)";
                        }
                        textViewWelcome.setText(welcomeText);
                    }

                    // Setup UI based on role
                    setupUIForRole();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Database error: " +
                        databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUIForRole() {
        if (userRole != null && userRole.equals("vendor")) {
            // Show vendor-specific UI elements
            buttonUpdateLocation.setVisibility(View.VISIBLE);

            // Add QR code button for vendors
            Button buttonGenerateQR = findViewById(R.id.buttonGenerateQR);
            buttonGenerateQR.setVisibility(View.VISIBLE);
            buttonGenerateQR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, VendorQRCodeActivity.class));
                }
            });
        } else {
            // Show map for regular users
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contentFrame, new MapFragment())
                    .commit();

            // Add scan QR code button for users
            Button buttonScanQR = findViewById(R.id.buttonScanQR);
            buttonScanQR.setVisibility(View.VISIBLE);
            buttonScanQR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, QRScannerActivity.class));
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // Navigate to search activity
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            // Navigate to profile activity
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // Sign out user
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


