package com.example.localfoodfinder;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class VendorQRCodeActivity extends AppCompatActivity {

    private ImageView imageViewQRCode;
    private TextView textViewBusinessName;
    private Button buttonShare, buttonSave;
    private Bitmap qrBitmap;
    private String vendorId;
    private String businessName;
    private Double vendorLatitude;
    private Double vendorLongitude;
    private static final String TAG = "VendorQRCodeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_qr_code);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your QR Code");
        }

        // Initialize UI elements
        imageViewQRCode = findViewById(R.id.imageViewQRCode);
        textViewBusinessName = findViewById(R.id.textViewBusinessName);
        buttonShare = findViewById(R.id.buttonShare);
        buttonSave = findViewById(R.id.buttonSave);
        Button buttonBack = findViewById(R.id.buttonBack);

        // Get current vendor ID
        vendorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load vendor details and generate QR code
        loadVendorDetails();

        // Set up button click listeners
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareQRCode();
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQRCode();
            }
        });

        // Add back button click listener
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and return to the previous one
            }
        });
    }

    private void loadVendorDetails() {
        DatabaseReference vendorRef = FirebaseDatabase.getInstance().getReference()
                .child("vendors").child(vendorId);

        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    businessName = dataSnapshot.child("businessName").getValue(String.class);
                    if (businessName != null) {
                        textViewBusinessName.setText(businessName);
                    }

                    // Get vendor location
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);

                    // Store location for QR code generation
                    if (latitude != null && longitude != null) {
                        vendorLatitude = latitude;
                        vendorLongitude = longitude;
                    }

                    // Generate QR code
                    generateQRCode();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VendorQRCodeActivity.this,
                        "Error loading vendor details: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQRCode() {
        // Create QR code content with Google Maps link
        String qrContent;

        if (vendorLatitude != null && vendorLongitude != null) {
            // Format: "https://maps.google.com/maps?q=LATITUDE,LONGITUDE"
            qrContent = "https://maps.google.com/maps?q=" + vendorLatitude + "," + vendorLongitude;

            // Add business name as label if available
            if (businessName != null && !businessName.isEmpty()) {
                qrContent += "(" + businessName.replace(" ", "+") + ")";
            }
        } else {
            // Fallback to app-specific URI if location is not available
            qrContent = "localfoodfinder://vendor?id=" + vendorId;
        }

        // Log the QR content for debugging
        Log.d("VendorQRCodeActivity", "QR content: " + qrContent);

        // Generate QR code
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.createBitmap(bitMatrix);
            imageViewQRCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR code not generated yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save bitmap to cache directory
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "vendor_qr_code.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get content URI via FileProvider
            Uri contentUri = FileProvider.getUriForFile(this,
                    "com.example.localfoodfinder.fileprovider", imageFile);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

            // Create share message with location info if available
            String shareMessage = "Scan this QR code to find " + businessName + " on Local Food Finder!";
            if (vendorLatitude != null && vendorLongitude != null) {
                shareMessage += " Located at: https://maps.google.com/maps?q=" +
                        vendorLatitude + "," + vendorLongitude;
            }

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR code not generated yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // For Android 10 (API 29) and above, use MediaStore API
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            String fileName = "LocalFoodFinder_" + businessName.replaceAll("\\s+", "_") + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try {
                OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                Toast.makeText(this, "QR code saved to Pictures folder", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving QR code", Toast.LENGTH_SHORT).show();
            }
        } else {
            // For older Android versions, use the traditional approach
            String fileName = "LocalFoodFinder_" + businessName.replaceAll("\\s+", "_") + ".png";
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File imageFile = new File(picturesDir, fileName);

            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                // Notify gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "QR code saved to Pictures folder", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving QR code", Toast.LENGTH_SHORT).show();
            }
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
}







