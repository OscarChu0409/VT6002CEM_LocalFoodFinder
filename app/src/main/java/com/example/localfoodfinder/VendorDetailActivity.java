package com.example.localfoodfinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bumptech.glide.Glide;

public class VendorDetailActivity extends AppCompatActivity {

    private TextView textViewBusinessName, textViewDescription, textViewPhone,
            textViewBusinessHours, textViewRating, textViewDietaryOptions;
    private Button buttonDirections, buttonSubmitReview;
    private RatingBar ratingBarVendor, ratingBarUserReview;
    private EditText editTextReview;
    private RecyclerView recyclerViewReviews;

    private String vendorId;
    private Vendor currentVendor;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private List<Review> reviewList;
    private ReviewAdapter reviewAdapter;

    private ImageView imageViewMenuPhoto;
    private TextView textViewNoMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_detail);

        // Set up action bar with back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vendor Details");
        }

        // Register back callback (modern approach)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get vendor ID from intent
        vendorId = getIntent().getStringExtra("vendor_id");
        if (vendorId == null) {
            Toast.makeText(this, "Error: Vendor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        textViewBusinessName = findViewById(R.id.textViewBusinessName);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewBusinessHours = findViewById(R.id.textViewBusinessHours);
        textViewRating = findViewById(R.id.textViewRating);
        textViewDietaryOptions = findViewById(R.id.textViewDietaryOptions);
        ratingBarVendor = findViewById(R.id.ratingBarVendor);
        buttonDirections = findViewById(R.id.buttonDirections);

        // Review section
        ratingBarUserReview = findViewById(R.id.ratingBarUserReview);
        editTextReview = findViewById(R.id.editTextReview);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);

        // Set up RecyclerView for reviews
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(reviewAdapter);

        // Initialize menu photo UI elements
        imageViewMenuPhoto = findViewById(R.id.imageViewMenuPhoto);
        textViewNoMenu = findViewById(R.id.textViewNoMenu);

        // Load vendor details
        loadVendorDetails();

        // Load reviews
        loadReviews();

        // Set up submit review button
        buttonSubmitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });

        // Add back button
        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadVendorDetails() {
        DatabaseReference vendorRef = mDatabase.child("vendors").child(vendorId);
        vendorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentVendor = new Vendor();
                    currentVendor.setId(vendorId);
                    currentVendor.setBusinessName(dataSnapshot.child("businessName").getValue(String.class));
                    currentVendor.setDescription(dataSnapshot.child("description").getValue(String.class));
                    currentVendor.setPhone(dataSnapshot.child("phone").getValue(String.class));

                    String openTime = dataSnapshot.child("openTime").getValue(String.class);
                    String closeTime = dataSnapshot.child("closeTime").getValue(String.class);
                    currentVendor.setOpenTime(openTime);
                    currentVendor.setCloseTime(closeTime);

                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                    if (latitude != null && longitude != null) {
                        currentVendor.setLatitude(latitude);
                        currentVendor.setLongitude(longitude);
                    }

                    Double ratingAvg = dataSnapshot.child("ratingAvg").getValue(Double.class);
                    Integer ratingCount = dataSnapshot.child("ratingCount").getValue(Integer.class);
                    if (ratingAvg != null) currentVendor.setRatingAvg(ratingAvg);
                    if (ratingCount != null) currentVendor.setRatingCount(ratingCount);

                    // Load menu photo URL
                    String menuPhotoUrl = dataSnapshot.child("menuPhotoUrl").getValue(String.class);

                    if (menuPhotoUrl != null && !menuPhotoUrl.isEmpty()) {
                        // Display the menu photo
                        imageViewMenuPhoto.setVisibility(View.VISIBLE);
                        textViewNoMenu.setVisibility(View.GONE);

                        Glide.with(VendorDetailActivity.this)
                                .load(menuPhotoUrl)
                                .into(imageViewMenuPhoto);
                    } else {
                        // No menu photo available
                        imageViewMenuPhoto.setVisibility(View.GONE);
                        textViewNoMenu.setVisibility(View.VISIBLE);
                    }

                    // Update UI with vendor details
                    updateVendorUI();
                } else {
                    Toast.makeText(VendorDetailActivity.this,
                            "Vendor not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VendorDetailActivity.this,
                        "Error loading vendor: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVendorUI() {
        if (currentVendor != null) {
            textViewBusinessName.setText(currentVendor.getBusinessName());
            textViewDescription.setText(currentVendor.getDescription());

            if (currentVendor.getPhone() != null && !currentVendor.getPhone().isEmpty()) {
                textViewPhone.setText("Phone: " + currentVendor.getPhone());
                textViewPhone.setVisibility(View.VISIBLE);
            } else {
                textViewPhone.setVisibility(View.GONE);
            }

            textViewBusinessHours.setText("Hours: " + currentVendor.getBusinessHours());

            // Set rating
            if (currentVendor.getRatingCount() > 0) {
                textViewRating.setText(currentVendor.getFormattedRating());
                ratingBarVendor.setRating((float) currentVendor.getRatingAvg());
            } else {
                textViewRating.setText("No ratings yet");
                ratingBarVendor.setRating(0);
            }

            // Set up directions button
            buttonDirections.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                            currentVendor.getLatitude() + "," + currentVendor.getLongitude());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                }
            });

            // Update dietary options display
            StringBuilder dietaryOptions = new StringBuilder();
            if (currentVendor.isVegan()) {
                dietaryOptions.append("• Vegan Options\n");
            }
            if (currentVendor.isHalal()) {
                dietaryOptions.append("• Halal Options\n");
            }
            if (currentVendor.isGlutenFree()) {
                dietaryOptions.append("• Gluten-Free Options\n");
            }

            if (dietaryOptions.length() > 0) {
                textViewDietaryOptions.setText(dietaryOptions.toString());
                textViewDietaryOptions.setVisibility(View.VISIBLE);
            } else {
                textViewDietaryOptions.setVisibility(View.GONE);
            }
        }
    }

    private void loadReviews() {
        DatabaseReference reviewsRef = mDatabase.child("reviews").orderByChild("vendorId").equalTo(vendorId).getRef();
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Review> newReviewList = new ArrayList<>();

                for (DataSnapshot reviewSnapshot : dataSnapshot.getChildren()) {
                    Review review = reviewSnapshot.getValue(Review.class);
                    if (review != null) {
                        review.setId(reviewSnapshot.getKey());
                        newReviewList.add(review);
                    }
                }

                // Use the new update method instead of notifyDataSetChanged
                reviewAdapter.updateReviews(newReviewList);

                if (newReviewList.isEmpty()) {
                    // Show a message that there are no reviews yet
                    Toast.makeText(VendorDetailActivity.this,
                            "No reviews yet. Be the first to review!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VendorDetailActivity.this,
                        "Error loading reviews: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to leave a review",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBarUserReview.getRating();
        String comment = editTextReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a review", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the user's name from the database
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String userName = "Anonymous";
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                userName = name;
                            }
                        }

                        // Create the review
                        saveReview(currentUser.getUid(), userName, comment, rating);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(VendorDetailActivity.this,
                                "Error getting user data: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReview(String userId, String userName, String comment, float rating) {
        // Create a new review object
        Review review = new Review(vendorId, userId, userName, comment, rating, System.currentTimeMillis());

        // Save to Firebase
        String reviewId = mDatabase.child("reviews").push().getKey();
        if (reviewId != null) {
            mDatabase.child("reviews").child(reviewId).setValue(review)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(VendorDetailActivity.this,
                                        "Review submitted successfully",
                                        Toast.LENGTH_SHORT).show();

                                // Clear the review form
                                ratingBarUserReview.setRating(0);
                                editTextReview.setText("");

                                // Update the vendor's rating
                                updateVendorRating();
                            } else {
                                Toast.makeText(VendorDetailActivity.this,
                                        "Error submitting review: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void updateVendorRating() {
        DatabaseReference reviewsRef = mDatabase.child("reviews").orderByChild("vendorId").equalTo(vendorId).getRef();
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double totalRating = 0;
                int count = 0;

                for (DataSnapshot reviewSnapshot : dataSnapshot.getChildren()) {
                    Float rating = reviewSnapshot.child("rating").getValue(Float.class);
                    if (rating != null) {
                        totalRating += rating;
                        count++;
                    }
                }

                double averageRating = (count > 0) ? (totalRating / count) : 0;

                // Update the vendor's rating in the database
                HashMap<String, Object> ratingUpdate = new HashMap<>();
                ratingUpdate.put("ratingAvg", averageRating);
                ratingUpdate.put("ratingCount", count);

                mDatabase.child("vendors").child(vendorId).updateChildren(ratingUpdate);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(VendorDetailActivity.this,
                        "Error updating vendor rating: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}



