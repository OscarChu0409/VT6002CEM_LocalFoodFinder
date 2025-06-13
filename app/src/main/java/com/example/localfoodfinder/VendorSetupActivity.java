package com.example.localfoodfinder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class VendorSetupActivity extends AppCompatActivity {

    private EditText editTextBusinessName, editTextDescription, editTextPhone;
    private Button buttonSave;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_setup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        editTextBusinessName = findViewById(R.id.editTextBusinessName);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveVendorInfo();
            }
        });
    }

    private void saveVendorInfo() {
        String businessName = editTextBusinessName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

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

        // Save vendor info to database
        String userId = mAuth.getCurrentUser().getUid();

        HashMap<String, Object> vendorData = new HashMap<>();
        vendorData.put("businessName", businessName);
        vendorData.put("description", description);
        vendorData.put("phone", phone);
        vendorData.put("approved", false); // Vendors need approval before appearing in searches

        mDatabase.child("vendors").child(userId).setValue(vendorData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(VendorSetupActivity.this,
                                    "Vendor profile created successfully!",
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