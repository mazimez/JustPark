package com.example.tillnow.main;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

public class ParkingPlace {
    //local
    private String uid;
    private String placeName;
    private LatLng location;
    private int totalSlots,availableSlots;
    private float pricePerSlot;

    //classes
    private Calendar openingTime,closingtime;

    public ParkingPlace(String uid, String placeName, LatLng location, int totalSlots, int availableSlots, float pricePerSlot, Calendar openingTime, Calendar closingtime) {
        this.uid = uid;
        this.placeName = placeName;
        this.location = location;
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
        this.pricePerSlot = pricePerSlot;
        this.openingTime = openingTime;
        this.closingtime = closingtime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public ParkingPlace() {
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public float getPricePerSlot() {
        return pricePerSlot;
    }

    public void setPricePerSlot(float pricePerSlot) {
        this.pricePerSlot = pricePerSlot;
    }

    public Calendar getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(Calendar openingTime) {
        this.openingTime = openingTime;
    }

    public Calendar getClosingtime() {
        return closingtime;
    }

    public void setClosingtime(Calendar closingtime) {
        this.closingtime = closingtime;
    }
}
