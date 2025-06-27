package com.example.localfoodfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CheckBox;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;

import java.util.HashMap;

public class VendorSetupActivity extends AppCompatActivity {

    private EditText editTextBusinessName, editTextDescription, editTextPhone,
            editTextLatitude, editTextLongitude, editTextOpenTime, editTextCloseTime;
    private Button buttonSave, buttonGetLocation, buttonUploadMenu;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_MENU_IMAGE_REQUEST = 2;
    private static final int MENU_UPLOAD_REQUEST = 3;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference storageRef;

    private CheckBox checkBoxVegan, checkBoxHalal, checkBoxGlutenFree;
    private ImageView imageViewMenu;
    private TextView textViewMenuStatus;
    private Uri menuImageUri;
    private String menuPhotoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_setup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI elements
        editTextBusinessName = findViewById(R.id.editTextBusinessName);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        editTextOpenTime = findViewById(R.id.editTextOpenTime);
        editTextCloseTime = findViewById(R.id.editTextCloseTime);
        buttonSave = findViewById(R.id.buttonSave);
        buttonGetLocation = findViewById(R.id.buttonGetLocation);
        checkBoxVegan = findViewById(R.id.checkBoxVegan);
        checkBoxHalal = findViewById(R.id.checkBoxHalal);
        checkBoxGlutenFree = findViewById(R.id.checkBoxGlutenFree);
        buttonUploadMenu = findViewById(R.id.buttonUploadMenu);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        textViewMenuStatus = findViewById(R.id.textViewMenuStatus);

        // Set up menu photo upload button
        buttonUploadMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the menu upload activity
                Intent intent = new Intent(VendorSetupActivity.this, MenuUploadActivity.class);
                startActivityForResult(intent, MENU_UPLOAD_REQUEST);
            }
        });

        // Check if this is an update (load existing data)
        loadExistingVendorData();

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveVendorInfo();
            }
        });

        buttonGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        // Try to get location when activity starts
        getCurrentLocation();
    }

    private void loadExistingVendorData() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference vendorRef = FirebaseDatabase.getInstance().getReference()
                .child("vendors").child(userId);

        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // This is an update, not a new vendor setup
                    String businessName = dataSnapshot.child("businessName").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                    String openTime = dataSnapshot.child("openTime").getValue(String.class);
                    String closeTime = dataSnapshot.child("closeTime").getValue(String.class);

                    // Fill the form with existing data
                    if (businessName != null) editTextBusinessName.setText(businessName);
                    if (description != null) editTextDescription.setText(description);
                    if (phone != null) editTextPhone.setText(phone);
                    if (latitude != null) editTextLatitude.setText(String.valueOf(latitude));
                    if (longitude != null) editTextLongitude.setText(String.valueOf(longitude));
                    if (openTime != null) editTextOpenTime.setText(openTime);
                    if (closeTime != null) editTextCloseTime.setText(closeTime);

                    // Load dietary restrictions
                    Boolean isVegan = dataSnapshot.child("isVegan").getValue(Boolean.class);
                    Boolean isHalal = dataSnapshot.child("isHalal").getValue(Boolean.class);
                    Boolean isGlutenFree = dataSnapshot.child("isGlutenFree").getValue(Boolean.class);

                    if (isVegan != null) checkBoxVegan.setChecked(isVegan);
                    if (isHalal != null) checkBoxHalal.setChecked(isHalal);
                    if (isGlutenFree != null) checkBoxGlutenFree.setChecked(isGlutenFree);

                    // Load menu photo URL
                    menuPhotoUrl = dataSnapshot.child("menuPhotoUrl").getValue(String.class);

                    if (menuPhotoUrl != null && !menuPhotoUrl.isEmpty()) {
                        // Display the existing menu photo
                        imageViewMenu.setVisibility(View.VISIBLE);
                        Glide.with(VendorSetupActivity.this)
                                .load(menuPhotoUrl)
                                .into(imageViewMenu);
                        textViewMenuStatus.setText("Current menu photo (will be replaced if you upload a new one)");
                    }

                    // Update the title and button text
                    setTitle("Update Vendor Information");
                    buttonSave.setText("Update Information");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VendorSetupActivity.this,
                        "Error loading vendor data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Auto-fill the latitude and longitude fields
                                editTextLatitude.setText(String.valueOf(location.getLatitude()));
                                editTextLongitude.setText(String.valueOf(location.getLongitude()));
                                Toast.makeText(VendorSetupActivity.this,
                                        "Location detected successfully",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(VendorSetupActivity.this,
                                        "Could not detect location. Please enter manually.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this,
                        "Location permission denied. Please enter coordinates manually.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openMenuImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Menu Photo"), PICK_MENU_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_MENU_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            menuImageUri = data.getData();

            // Display the selected image
            imageViewMenu.setVisibility(View.VISIBLE);
            imageViewMenu.setImageURI(menuImageUri);
            textViewMenuStatus.setText("Menu photo selected (not yet uploaded)");
        }

        // Handle menu upload result
        if (requestCode == MENU_UPLOAD_REQUEST && resultCode == RESULT_OK && data != null) {
            menuPhotoUrl = data.getStringExtra("menuPhotoUrl");

            if (menuPhotoUrl != null && !menuPhotoUrl.isEmpty()) {
                // Display the uploaded menu photo
                imageViewMenu.setVisibility(View.VISIBLE);
                Glide.with(VendorSetupActivity.this)
                        .load(menuPhotoUrl)
                        .into(imageViewMenu);
                textViewMenuStatus.setText("Menu photo uploaded successfully");
            }
        }
    }

    private void saveVendorInfo() {
        String businessName = editTextBusinessName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String latitudeStr = editTextLatitude.getText().toString().trim();
        String longitudeStr = editTextLongitude.getText().toString().trim();
        String openTime = editTextOpenTime.getText().toString().trim();
        String closeTime = editTextCloseTime.getText().toString().trim();

        // Get dietary restrictions
        boolean isVegan = checkBoxVegan.isChecked();
        boolean isHalal = checkBoxHalal.isChecked();
        boolean isGlutenFree = checkBoxGlutenFree.isChecked();

        // Validate inputs
        if (TextUtils.isEmpty(businessName)) {
            editTextBusinessName.setError("Business name is required");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            editTextDescription.setError("Description is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required");
            return;
        }

        // Parse latitude and longitude
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            if (!TextUtils.isEmpty(latitudeStr)) latitude = Double.parseDouble(latitudeStr);
            if (!TextUtils.isEmpty(longitudeStr)) longitude = Double.parseDouble(longitudeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid location format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save vendor info to database
        String userId = mAuth.getCurrentUser().getUid();

        HashMap<String, Object> vendorData = new HashMap<>();
        vendorData.put("businessName", businessName);
        vendorData.put("description", description);
        vendorData.put("phone", phone);
        vendorData.put("latitude", latitude);
        vendorData.put("longitude", longitude);
        vendorData.put("openTime", openTime);
        vendorData.put("closeTime", closeTime);
        vendorData.put("approved", true); // Set to true for immediate visibility in searches
        vendorData.put("ratingAvg", 0.0); // Initialize average rating
        vendorData.put("ratingCount", 0); // Initialize rating count
        vendorData.put("isVegan", isVegan);
        vendorData.put("isHalal", isHalal);
        vendorData.put("isGlutenFree", isGlutenFree);

        // Add menu photo URL to vendor data if available
        if (menuPhotoUrl != null && !menuPhotoUrl.isEmpty()) {
            vendorData.put("menuPhotoUrl", menuPhotoUrl);
        }

        // Save vendor data to Firebase
        saveVendorDataToFirebase(userId, vendorData);
    }

    private void uploadMenuPhoto(String userId, final HashMap<String, Object> vendorData) {
        // Show progress
        textViewMenuStatus.setText("Uploading menu photo...");

        // Create a storage reference
        final StorageReference menuRef = storageRef.child("menu_photos").child(userId + "_menu.jpg");

        // Upload the file
        menuRef.putFile(menuImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the download URL
                        menuRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Add the URL to vendor data
                                vendorData.put("menuPhotoUrl", uri.toString());

                                // Save the vendor data
                                saveVendorDataToFirebase(userId, vendorData);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        textViewMenuStatus.setText("Failed to upload menu photo");
                        Toast.makeText(VendorSetupActivity.this,
                                "Error uploading menu photo: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // Save vendor data without the photo
                        saveVendorDataToFirebase(userId, vendorData);
                    }
                });
    }

    private void saveVendorDataToFirebase(String userId, HashMap<String, Object> vendorData) {
        mDatabase.child("vendors").child(userId).setValue(vendorData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(VendorSetupActivity.this,
                                    "Vendor profile saved successfully!",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(VendorSetupActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(VendorSetupActivity.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
