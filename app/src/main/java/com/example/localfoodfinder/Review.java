package com.example.localfoodfinder;

public class Review {
    private String id;
    private String vendorId;
    private String userId;
    private String userName;
    private String comment;
    private float rating;
    private long timestamp;

    public Review() {
        // Required empty constructor for Firebase
    }

    public Review(String vendorId, String userId, String userName, String comment, float rating, long timestamp) {
        this.vendorId = vendorId;
        this.userId = userId;
        this.userName = userName;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}