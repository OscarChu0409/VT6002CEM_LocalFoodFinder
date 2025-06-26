package com.example.localfoodfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private SimpleDateFormat dateFormat;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.textViewUserName.setText(review.getUserName());
        holder.textViewComment.setText(review.getComment());
        holder.ratingBarReview.setRating(review.getRating());

        // Format and set the date
        String formattedDate = dateFormat.format(new Date(review.getTimestamp()));
        holder.textViewDate.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName;
        TextView textViewComment;
        TextView textViewDate;
        RatingBar ratingBarReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewComment = itemView.findViewById(R.id.textViewComment);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
        }
    }

    public void updateReviews(List<Review> newReviews) {
        // Calculate the difference between old and new list
        int oldSize = this.reviewList.size();
        this.reviewList.clear();
        notifyItemRangeRemoved(0, oldSize);

        this.reviewList.addAll(newReviews);
        notifyItemRangeInserted(0, newReviews.size());
    }
}

