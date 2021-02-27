package com.example.tillnow.main;

class BookedHistoryModel {
    private String PlaceName;
    private int TotalPrice;
    private int TotalHours;
    private int BookedId;
    private String placeId;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    private String Status;

    public BookedHistoryModel() {
    }

    public BookedHistoryModel(String placeName, int totalPrice, int totalHours, int BookedId,String status) {
        this.PlaceName = placeName;
        this.TotalPrice = totalPrice;
        this.TotalHours = totalHours;
        this.BookedId = BookedId;
        this.Status = status;
    }

    public String getPlaceName() {
        return PlaceName;
    }

    public void setPlaceName(String placeName) {
        PlaceName = placeName;
    }

    public int getTotalPrice() {
        return TotalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        TotalPrice = totalPrice;
    }

    public int getTotalHours() {
        return TotalHours;
    }

    public void setTotalHours(int totalHours) {
        TotalHours = totalHours;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public int getBookedId() {
        return BookedId;
    }

    public void setBookedId(int bookedId) {
        BookedId = bookedId;
    }
}
