package com.example.localfoodfinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editTextDisplayName;
    private EditText editTextPhone;
    private ImageView imageViewProfile;
    private Button buttonSave;
    private Button buttonChangeImage;
    private Button buttonManageMenu;
    private String userRole;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private StorageReference mStorageRef;
    private Uri imageUri;

    private TextView textViewVendorOptions;
    private View dividerVendor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to login
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        mUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid());
        mStorageRef = FirebaseStorage.getInstance().getReference().child("profile_images").child(currentUser.getUid());

        // Initialize UI elements
        editTextDisplayName = findViewById(R.id.editTextDisplayName);
        editTextPhone = findViewById(R.id.editTextPhone);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        buttonSave = findViewById(R.id.buttonSave);
        buttonChangeImage = findViewById(R.id.buttonChangeImage);
        buttonManageMenu = findViewById(R.id.buttonManageMenu);

        // Initialize vendor options section
        textViewVendorOptions = findViewById(R.id.textViewVendorOptions);
        dividerVendor = findViewById(R.id.dividerVendor);

        // Initially hide vendor options
        textViewVendorOptions.setVisibility(View.GONE);
        dividerVendor.setVisibility(View.GONE);

        // Initially hide the button
        buttonManageMenu.setVisibility(View.GONE);

        // Check user role and show vendor-specific options if applicable
        checkUserRole();

        // Set up button click listeners
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });

        buttonChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        buttonManageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, MenuUploadActivity.class));
            }
        });
    }

    private void loadUserData() {
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    // Set the values to UI elements
                    if (name != null) {
                        editTextDisplayName.setText(name);
                    }

                    if (phone != null) {
                        editTextPhone.setText(phone);
                    }

                    // Load profile image if available
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        // Load image with Glide
                        Glide.with(ProfileActivity.this)
                                .load(profileImageUrl)
                                .circleCrop() // Optional: makes the image circular
                                .placeholder(R.drawable.default_profile) // You'll need to create this drawable
                                .error(R.drawable.default_profile)
                                .into(imageViewProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Error loading profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            // Display the selected image
            imageViewProfile.setImageURI(imageUri);
        }
    }

    private void saveUserProfile() {
        final String displayName = editTextDisplayName.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();

        if (displayName.isEmpty()) {
            editTextDisplayName.setError("Display name is required");
            return;
        }

        // Show progress
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show();

        // Update user data in Firebase Database
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Update display name in Auth
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();

            currentUser.updateProfile(profileUpdates);

            // Update user data in Database
            mUserRef.child("name").setValue(displayName);
            mUserRef.child("phone").setValue(phone);

            // Upload image if selected
            if (imageUri != null) {
                uploadImage();
            } else {
                // No image to upload, finish
                Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void uploadImage() {
        if (imageUri != null) {
            // Upload to Firebase Storage
            mStorageRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get the download URL
                            mStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Save URL to user profile
                                    mUserRef.child("profileImageUrl").setValue(uri.toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(ProfileActivity.this,
                                                                "Profile updated successfully",
                                                                Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                }
                                            });
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfileActivity.this,
                                    "Failed to upload image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        userRole = dataSnapshot.child("role").getValue(String.class);

                        // Show vendor-specific options if the user is a vendor
                        if (userRole != null && userRole.equals("vendor")) {
                            buttonManageMenu.setVisibility(View.VISIBLE);
                            textViewVendorOptions.setVisibility(View.VISIBLE);
                            dividerVendor.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ProfileActivity.this,
                            "Error checking user role: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


