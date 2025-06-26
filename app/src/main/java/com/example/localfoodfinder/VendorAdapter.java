package com.example.localfoodfinder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VendorAdapter extends RecyclerView.Adapter<VendorAdapter.VendorViewHolder> {

    private Context context;
    private List<Vendor> vendorList;
    private List<Vendor> originalList;

    public VendorAdapter(Context context, List<Vendor> vendorList) {
        this.context = context;
        this.vendorList = vendorList;
        this.originalList = vendorList;
    }

    @NonNull
    @Override
    public VendorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vendor, parent, false);
        return new VendorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VendorViewHolder holder, int position) {
        Vendor vendor = vendorList.get(position);

        holder.textViewBusinessName.setText(vendor.getBusinessName());
        holder.textViewDescription.setText(vendor.getDescription());

        if (vendor.getPhone() != null && !vendor.getPhone().isEmpty()) {
            holder.textViewPhone.setText("Phone: " + vendor.getPhone());
            holder.textViewPhone.setVisibility(View.VISIBLE);
        } else {
            holder.textViewPhone.setVisibility(View.GONE);
        }

        // Display business hours
        if (vendor.getOpenTime() != null && !vendor.getOpenTime().isEmpty() &&
                vendor.getCloseTime() != null && !vendor.getCloseTime().isEmpty()) {
            holder.textViewBusinessHours.setText("Hours: " + vendor.getBusinessHours());
            holder.textViewBusinessHours.setVisibility(View.VISIBLE);
        } else {
            holder.textViewBusinessHours.setVisibility(View.GONE);
        }

        // Set up directions button
        holder.buttonDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                        vendor.getLatitude() + "," + vendor.getLongitude());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapIntent);
                }
            }
        });

        // Set up item click to open vendor details
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, VendorDetailActivity.class);
                intent.putExtra("vendor_id", vendor.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return vendorList.size();
    }

    public void updateList(List<Vendor> filteredList) {
        this.vendorList = filteredList;
        notifyDataSetChanged();
    }

    public void resetList() {
        this.vendorList = originalList;
        notifyDataSetChanged();
    }

    /**
     * Returns the current list of vendors being displayed
     * @return The current list of vendors
     */
    public List<Vendor> getCurrentList() {
        return vendorList;
    }

    public static class VendorViewHolder extends RecyclerView.ViewHolder {
        TextView textViewBusinessName;
        TextView textViewDescription;
        TextView textViewPhone;
        TextView textViewBusinessHours;
        Button buttonDirections;

        public VendorViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewBusinessName = itemView.findViewById(R.id.textViewBusinessName);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewPhone = itemView.findViewById(R.id.textViewPhone);
            textViewBusinessHours = itemView.findViewById(R.id.textViewBusinessHours);
            buttonDirections = itemView.findViewById(R.id.buttonDirections);
        }
    }
}


