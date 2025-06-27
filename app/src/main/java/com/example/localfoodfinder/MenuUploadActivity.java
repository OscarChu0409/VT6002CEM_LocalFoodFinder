package com.example.localfoodfinder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.google.firebase.storage.OnProgressListener;

public class MenuUploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;

    private Button buttonSelectPhoto, buttonUpload, buttonCancel;
    private ImageView imageViewMenu;
    private TextView textViewNoImage;
    private ProgressBar progressBarUpload;

    private Uri imageUri;
    private String existingMenuUrl;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference storageRef;

    private static final String TAG = "MenuUploadActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_upload);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Upload Menu");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize UI elements
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);
        buttonUpload = findViewById(R.id.buttonUpload);
        buttonCancel = findViewById(R.id.buttonCancel);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        textViewNoImage = findViewById(R.id.textViewNoImage);
        progressBarUpload = findViewById(R.id.progressBarUpload);

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to upload a menu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load existing menu photo if any
        loadExistingMenuPhoto();

        // Set up button click listeners
        buttonSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermission();
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadMenuPhoto();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadExistingMenuPhoto() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference vendorRef = mDatabase.child("vendors").child(userId);

        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    existingMenuUrl = dataSnapshot.child("menuPhotoUrl").getValue(String.class);

                    if (existingMenuUrl != null && !existingMenuUrl.isEmpty()) {
                        // Display existing menu photo
                        imageViewMenu.setVisibility(View.VISIBLE);
                        textViewNoImage.setVisibility(View.GONE);

                        Glide.with(MenuUploadActivity.this)
                                .load(existingMenuUrl)
                                .into(imageViewMenu);

                        Toast.makeText(MenuUploadActivity.this,
                                "You already have a menu photo. Uploading a new one will replace it.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MenuUploadActivity.this,
                        "Error loading existing menu: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 and above, we don't need READ_EXTERNAL_STORAGE permission
            // when using the Storage Access Framework
            Log.d(TAG, "Android 11+ detected, using Storage Access Framework");
            openImageChooser();
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // For Android 6-10, we need to request the permission
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission");
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST);
                return;
            }
            // Permission already granted, proceed with image selection
            Log.d(TAG, "Storage permission already granted");
            openImageChooser();
        } else {
            // For Android 5 and below, permissions are granted at install time
            Log.d(TAG, "Android 5 or below, permissions granted at install time");
            openImageChooser();
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Menu Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.d(TAG, "Image selected: " + imageUri.toString());

            try {
                // Display the selected image
                imageViewMenu.setVisibility(View.VISIBLE);
                textViewNoImage.setVisibility(View.GONE);

                // Use Glide to load the image (more reliable than direct URI)
                Glide.with(this)
                        .load(imageUri)
                        .into(imageViewMenu);

                // Enable upload button
                buttonUpload.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying selected image: " + e.getMessage(), e);
                Toast.makeText(this, "Error displaying selected image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void uploadMenuPhoto() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings and try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Show progress
        progressBarUpload.setVisibility(View.VISIBLE);
        buttonUpload.setEnabled(false);
        buttonSelectPhoto.setEnabled(false);

        // Compress and upload the image
        try {
            // Get bitmap from URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Compress bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();

            // Log compressed size
            Log.d(TAG, "Original image URI: " + imageUri.toString());
            Log.d(TAG, "Compressed image size: " + data.length + " bytes");

            // Get user ID
            String userId = mAuth.getCurrentUser().getUid();

            // Create a storage reference
            final StorageReference menuRef = storageRef.child("menu_photos").child(userId + "_menu.jpg");

            // Upload the compressed image
            UploadTask uploadTask = menuRef.putBytes(data);
            uploadTask
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG, "Upload successful, getting download URL");

                            // Get the download URL
                            menuRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Log.d(TAG, "Got download URL: " + uri.toString());

                                    // Save the URL to the vendor's database record
                                    mDatabase.child("vendors").child(userId).child("menuPhotoUrl").setValue(uri.toString())
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "Menu URL saved to database");

                                                    Toast.makeText(MenuUploadActivity.this,
                                                            "Menu photo uploaded successfully",
                                                            Toast.LENGTH_SHORT).show();

                                                    // Return to previous activity with success result
                                                    Intent resultIntent = new Intent();
                                                    resultIntent.putExtra("menuPhotoUrl", uri.toString());
                                                    setResult(RESULT_OK, resultIntent);
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "Failed to save menu URL: " + e.getMessage(), e);

                                                    progressBarUpload.setVisibility(View.GONE);
                                                    buttonUpload.setEnabled(true);
                                                    buttonSelectPhoto.setEnabled(true);

                                                    Toast.makeText(MenuUploadActivity.this,
                                                            "Failed to save menu URL: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Failed to get download URL: " + e.getMessage(), e);

                                    progressBarUpload.setVisibility(View.GONE);
                                    buttonUpload.setEnabled(true);
                                    buttonSelectPhoto.setEnabled(true);

                                    Toast.makeText(MenuUploadActivity.this,
                                            "Failed to get download URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Upload failed: " + e.getMessage(), e);

                            progressBarUpload.setVisibility(View.GONE);
                            buttonUpload.setEnabled(true);
                            buttonSelectPhoto.setEnabled(true);

                            Toast.makeText(MenuUploadActivity.this,
                                    "Upload failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            // Log progress
                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            Log.d(TAG, "Upload progress: " + progress + "%");
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error compressing image: " + e.getMessage(), e);
            progressBarUpload.setVisibility(View.GONE);
            buttonUpload.setEnabled(true);
            buttonSelectPhoto.setEnabled(true);
            Toast.makeText(this, "Error preparing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean shouldShowRequestPermissionRationale() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with image selection
                Log.d(TAG, "Storage permission granted");
                openImageChooser();
            } else {
                // Permission denied
                Log.d(TAG, "Storage permission denied");

                if (!shouldShowRequestPermissionRationale()) {
                    // User selected "Don't ask again", show dialog to go to settings
                    showSettingsDialog();
                } else {
                    // User denied permission but didn't select "Don't ask again"
                    Toast.makeText(this, "Storage permission is required to select images",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
                .setMessage("This app needs storage permission to access your photos. " +
                        "Please grant this permission in the app settings.")
                .setPositiveButton("Go to Settings", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        // Open app settings
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}


