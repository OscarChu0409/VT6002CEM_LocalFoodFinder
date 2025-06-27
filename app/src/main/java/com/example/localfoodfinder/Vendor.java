package com.example.localfoodfinder;

public class Vendor {
    private String id;
    private String businessName;
    private String description;
    private String phone;
    private double latitude;
    private double longitude;
    private String openTime;
    private String closeTime;
    private double ratingAvg;
    private int ratingCount;
    private boolean isVegan;
    private boolean isHalal;
    private boolean isGlutenFree;
    private String menuPhotoUrl;

    public Vendor() {
        // Required empty constructor for Firebase
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public String getBusinessHours() {
        if (openTime != null && !openTime.isEmpty() && closeTime != null && !closeTime.isEmpty()) {
            return openTime + " - " + closeTime;
        } else {
            return "Hours not specified";
        }
    }

    public double getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(double ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getFormattedRating() {
        if (ratingCount > 0) {
            return String.format("%.1f (%d)", ratingAvg, ratingCount);
        } else {
            return "No ratings yet";
        }
    }

    public boolean isVegan() {
        return isVegan;
    }

    public void setVegan(boolean vegan) {
        isVegan = vegan;
    }

    public boolean isHalal() {
        return isHalal;
    }

    public void setHalal(boolean halal) {
        isHalal = halal;
    }

    public boolean isGlutenFree() {
        return isGlutenFree;
    }

    public void setGlutenFree(boolean glutenFree) {
        isGlutenFree = glutenFree;
    }

    public String getMenuPhotoUrl() {
        return menuPhotoUrl;
    }

    public void setMenuPhotoUrl(String menuPhotoUrl) {
        this.menuPhotoUrl = menuPhotoUrl;
    }
}



