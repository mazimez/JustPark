package com.example.tillnow;

import android.content.SharedPreferences;
import android.util.Log;

import com.example.tillnow.main.ParkingPlace;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;

import static android.content.Context.MODE_PRIVATE;

public class Helper {

    //methode to convert the calender into only time string
    public String getOnlyTime(Calendar cal){
        if(cal.get(Calendar.AM_PM)==1){
            return cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE)+" PM";
        }else{
            return cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE)+" AM";
        }
    }

    //methode to convert QueryDocumentSnapshot into parking place object
    public ParkingPlace toParkingPlace(QueryDocumentSnapshot placeDB){
        Map<String,Object> placemap = placeDB.getData();
        Map<String,Object> closeTimemap = (Map<String,Object>) placemap.get("closingTime");
        Map<String,Object> openTimemap = (Map<String,Object>) placemap.get("openingTime");
        Map<String,Object> locationmap = (Map<String,Object>) placemap.get("location");
        LatLng location = new LatLng(Double.parseDouble(locationmap.get("latitude").toString()),Double.parseDouble(locationmap.get("longitude").toString()));
        Calendar openTime =Calendar.getInstance();
        openTime.setTime(new Date(openTime.get(Calendar.YEAR)-1900,openTime.get(Calendar.MONTH),openTime.get(Calendar.DAY_OF_MONTH),Integer.parseInt(openTimemap.get("hours").toString()),Integer.parseInt(openTimemap.get("minutes").toString()),0));
        Calendar closeTime =Calendar.getInstance();
        closeTime.setTime(new Date(closeTime.get(Calendar.YEAR)-1900,closeTime.get(Calendar.MONTH),closeTime.get(Calendar.DAY_OF_MONTH), Integer.parseInt(closeTimemap.get("hours").toString()),Integer.parseInt(closeTimemap.get("minutes").toString()),0));
        ParkingPlace place = new ParkingPlace(placeDB.getId(),placeDB.getString("name"),location, Integer.parseInt(placeDB.get("totalSlots").toString()),Integer.parseInt(placeDB.get("availableSlots").toString()),Integer.parseInt(placeDB.get("pricePerSlot").toString()),openTime,closeTime);
        return place;
    }

    //methode to convert DocumentSnapshot into parking place object
     public  ParkingPlace toParkingPlace(DocumentSnapshot palceDetDB){
         Map<String,Object> placemap = palceDetDB.getData();
         Map<String,Object> closeTimemap = (Map<String,Object>) placemap.get("closingTime");
         Map<String,Object> openTimemap = (Map<String,Object>) placemap.get("openingTime");
         Map<String,Object> locationmap = (Map<String,Object>) placemap.get("location");
         LatLng location = new LatLng(Double.parseDouble(locationmap.get("latitude").toString()),Double.parseDouble(locationmap.get("longitude").toString()));
         Calendar openTime =Calendar.getInstance();
         openTime.setTime(new Date(openTime.get(Calendar.YEAR)-1900,openTime.get(Calendar.MONTH),openTime.get(Calendar.DAY_OF_MONTH),Integer.parseInt(openTimemap.get("hours").toString()),Integer.parseInt(openTimemap.get("minutes").toString()),0));
         Calendar closeTime =Calendar.getInstance();
         closeTime.setTime(new Date(closeTime.get(Calendar.YEAR)-1900,closeTime.get(Calendar.MONTH),closeTime.get(Calendar.DAY_OF_MONTH), Integer.parseInt(closeTimemap.get("hours").toString()),Integer.parseInt(closeTimemap.get("minutes").toString()),0));
         ParkingPlace place = new ParkingPlace(palceDetDB.getId(),palceDetDB.getString("name"),location, Integer.parseInt(palceDetDB.get("totalSlots").toString()),Integer.parseInt(palceDetDB.get("availableSlots").toString()),Integer.parseInt(palceDetDB.get("pricePerSlot").toString()),openTime,closeTime);
         return place;
     }

     //methode to convert milli seconds into UNIT(10 minutes)
     public int millisToUnit(Long millis){
         int unit = (int) (millis / 600000L);
         return unit;
     }

}
