package com.example.localfoodfinder;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SearchActivity extends AppCompatActivity {

    private EditText editTextSearch;
    private RecyclerView recyclerViewVendors;
    private ProgressBar progressBar;
    private TextView textViewNoResults;

    private VendorAdapter vendorAdapter;
    private List<Vendor> vendorList;
    private DatabaseReference vendorsRef;

    private CheckBox filterVegan, filterHalal, filterGlutenFree;
    private Button buttonApplyFilters;
    private boolean filteringActive = false;
    private ImageButton buttonVoiceSearch;
    private static final int SPEECH_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Search Vendors");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Firebase
        vendorsRef = FirebaseDatabase.getInstance().getReference().child("vendors");

        // Initialize UI elements
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewVendors = findViewById(R.id.recyclerViewVendors);
        progressBar = findViewById(R.id.progressBar);
        textViewNoResults = findViewById(R.id.textViewNoResults);

        // Initialize filter controls
        filterVegan = findViewById(R.id.filterVegan);
        filterHalal = findViewById(R.id.filterHalal);
        filterGlutenFree = findViewById(R.id.filterGlutenFree);
        buttonApplyFilters = findViewById(R.id.buttonApplyFilters);

        buttonApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filteringActive = filterVegan.isChecked() || filterHalal.isChecked() || filterGlutenFree.isChecked();
                applyFilters();
            }
        });

        // Set up RecyclerView
        vendorList = new ArrayList<>();
        vendorAdapter = new VendorAdapter(this, vendorList);
        recyclerViewVendors.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewVendors.setAdapter(vendorAdapter);

        // Set up search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Search as user types
                searchVendors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Load all vendors initially
        loadAllVendors();

        // Initialize voice search button
        buttonVoiceSearch = findViewById(R.id.buttonVoiceSearch);
        buttonVoiceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognition();
            }
        });
    }

    private void loadAllVendors() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoResults.setVisibility(View.GONE);

        vendorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                vendorList.clear();

                for (DataSnapshot vendorSnapshot : dataSnapshot.getChildren()) {
                    // Only show approved vendors
                    Boolean approved = vendorSnapshot.child("approved").getValue(Boolean.class);
                    if (approved != null && approved) {
                        Vendor vendor = new Vendor();
                        vendor.setId(vendorSnapshot.getKey());
                        vendor.setBusinessName(vendorSnapshot.child("businessName").getValue(String.class));
                        vendor.setDescription(vendorSnapshot.child("description").getValue(String.class));
                        vendor.setPhone(vendorSnapshot.child("phone").getValue(String.class));

                        // Get business hours
                        String openTime = vendorSnapshot.child("openTime").getValue(String.class);
                        String closeTime = vendorSnapshot.child("closeTime").getValue(String.class);
                        vendor.setOpenTime(openTime);
                        vendor.setCloseTime(closeTime);

                        // Get location data
                        Double latitude = vendorSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = vendorSnapshot.child("longitude").getValue(Double.class);
                        if (latitude != null && longitude != null) {
                            vendor.setLatitude(latitude);
                            vendor.setLongitude(longitude);
                        }

                        // Load dietary restrictions
                        Boolean isVegan = vendorSnapshot.child("isVegan").getValue(Boolean.class);
                        Boolean isHalal = vendorSnapshot.child("isHalal").getValue(Boolean.class);
                        Boolean isGlutenFree = vendorSnapshot.child("isGlutenFree").getValue(Boolean.class);

                        if (isVegan != null) vendor.setVegan(isVegan);
                        if (isHalal != null) vendor.setHalal(isHalal);
                        if (isGlutenFree != null) vendor.setGlutenFree(isGlutenFree);

                        vendorList.add(vendor);
                    }
                }

                vendorAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (vendorList.isEmpty()) {
                    textViewNoResults.setVisibility(View.VISIBLE);
                } else {
                    textViewNoResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                textViewNoResults.setText("Error: " + databaseError.getMessage());
                textViewNoResults.setVisibility(View.VISIBLE);
            }
        });
    }

    private void searchVendors(String query) {
        if (query.isEmpty()) {
            // If search is empty, show all vendors
            vendorAdapter.updateList(vendorList);
            if (vendorList.isEmpty()) {
                textViewNoResults.setVisibility(View.VISIBLE);
            } else {
                textViewNoResults.setVisibility(View.GONE);
            }
            return;
        }

        // Filter vendors based on search query
        List<Vendor> filteredList = new ArrayList<>();
        for (Vendor vendor : vendorList) {
            if (vendor.getBusinessName() != null &&
                    vendor.getBusinessName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(vendor);
            } else if (vendor.getDescription() != null &&
                    vendor.getDescription().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(vendor);
            }
        }

        vendorAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            textViewNoResults.setText("No vendors found matching '" + query + "'");
            textViewNoResults.setVisibility(View.VISIBLE);
        } else {
            textViewNoResults.setVisibility(View.GONE);
        }
    }

    private void applyFilters() {
        if (!filteringActive) {
            // If no filters are active, show all vendors (or current search results)
            if (editTextSearch.getText().toString().isEmpty()) {
                vendorAdapter.updateList(vendorList);
            } else {
                searchVendors(editTextSearch.getText().toString());
            }
            return;
        }

        // Apply dietary filters
        List<Vendor> filteredList = new ArrayList<>();
        List<Vendor> currentList = editTextSearch.getText().toString().isEmpty() ?
                vendorList : vendorAdapter.getCurrentList();

        for (Vendor vendor : currentList) {
            boolean matchesFilters = true;

            if (filterVegan.isChecked() && !vendor.isVegan()) {
                matchesFilters = false;
            }

            if (matchesFilters && filterHalal.isChecked() && !vendor.isHalal()) {
                matchesFilters = false;
            }

            if (matchesFilters && filterGlutenFree.isChecked() && !vendor.isGlutenFree()) {
                matchesFilters = false;
            }

            if (matchesFilters) {
                filteredList.add(vendor);
            }
        }

        vendorAdapter.updateList(filteredList);

        // Show "no results" message if needed
        if (filteredList.isEmpty()) {
            textViewNoResults.setVisibility(View.VISIBLE);
        } else {
            textViewNoResults.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what you're looking for...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                editTextSearch.setText(spokenText);
                // The search will automatically trigger due to the TextWatcher
            }
        }
    }
}
