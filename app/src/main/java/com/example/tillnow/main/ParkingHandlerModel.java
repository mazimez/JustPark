package com.example.tillnow.main;

class ParkingHandlerModel {
    private String UserName;
    private String Status;
    private int totalHours;
    private int totalPrice;


    public ParkingHandlerModel(){

    }

    public ParkingHandlerModel(String userName, String status, int totalHours, int totalPrice) {
        this.UserName = userName;
        this.Status = status;
        this.totalHours = totalHours;
        this.totalPrice = totalPrice;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }
}
