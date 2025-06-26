package com.example.localfoodfinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRScannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start QR code scanner immediately
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a vendor QR code");
        integrator.setCameraId(0);  // Use default camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // Call super first

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                // Scan cancelled
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Scan successful, process the result
                processQRCode(result.getContents());
            }
        }
    }

    private void processQRCode(String qrContent) {
        Log.d("QRScannerActivity", "Processing QR code: " + qrContent);

        // Check if the QR code is a Google Maps URL
        if (qrContent.startsWith("https://maps.google.com/maps?q=")) {
            // Open Google Maps directly
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(qrContent));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // If Google Maps app is not installed, open in browser
                intent.setPackage(null);
                startActivity(intent);
            }
        }
        // Check if the QR code is in the app-specific format
        else if (qrContent.startsWith("localfoodfinder://vendor?id=")) {
            // Extract vendor ID
            String vendorId = qrContent.substring("localfoodfinder://vendor?id=".length());

            // Open VendorDetailActivity with the scanned vendor ID
            Intent intent = new Intent(this, VendorDetailActivity.class);
            intent.putExtra("vendor_id", vendorId);
            startActivity(intent);
        } else {
            // Invalid QR code format
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
        }

        // Close scanner activity
        finish();
    }
}






