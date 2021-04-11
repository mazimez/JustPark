package com.example.tillnow.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tillnow.Helper;
import com.example.tillnow.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class Scan extends AppCompatActivity {



    //local
    private String placeIdDB,placeIdScane;
    private String userId;
    private String SlotStatus;
    private int Bookid;
    private Map<String,Object> slotHistoryMap;
    private static final int CAMERA_PERMISSION_CODE = 101;

    //context
    private Context context=this;

    //shared profrences
    private SharedPreferences userData;

    //classes
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private Helper helper = new Helper();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //getting user Data from Shared Prefrences
        userData = getSharedPreferences("UserData",MODE_PRIVATE);
        userId = userData.getString("uid",null);
        Bookid = userData.getInt("bookId",0);
        SlotStatus = userData.getString("Book status","finished");


        //cheking if user has any slots booked or parked
        if(SlotStatus.equals("parked") || SlotStatus.equals("booked")){
            //Checking in database about User's slot's data
            db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(Bookid))
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                //getting data from Database and converting in into object
                                DocumentSnapshot slotHistory = task.getResult();
                                slotHistoryMap = slotHistory.getData();
                                SlotStatus = slotHistory.getString("Status");
                                placeIdDB = slotHistory.getString("placeId");

                                //checking if there is Slot with status of booked or parked
                                if(SlotStatus.equals("booked") || SlotStatus.equals("parked")){
                                    //checking for device's version and permission
                                    if(Build.VERSION.SDK_INT>=23){
                                        if(checkPermission(Manifest.permission.CAMERA)){
                                            //opening the scanner
                                            openScanner();
                                        }else{
                                            //requesting for permission of scaning
                                            requestPermission(Manifest.permission.CAMERA,CAMERA_PERMISSION_CODE);
                                        }
                                    }else{
                                        //no need to ask permission directly opening scanner
                                        openScanner();
                                    }

                                }else{
                                    //showing the toast to book the slot first
                                    Toast.makeText(context, "Please Book the slot first", Toast.LENGTH_SHORT).show();

                                    //taking user to search activity to book the slot
                                    Intent intent = new Intent(context, Search.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }else{
                                //didn't get any data from database, showing the error
                                Toast.makeText(context, "Error Occur", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(context, Search.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        }else{
            //showing the toast to book the slot first
            Toast.makeText(context, "Please Book the slot first", Toast.LENGTH_SHORT).show();

            //taking user to search activity to book the slot
            Intent intent = new Intent(context, Search.class);
            startActivity(intent);
            finish();
        }

        //setting up bottom menu
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListner);
        Menu menu =bottomNav.getMenu();
        MenuItem menuItem = menu.getItem(2);
        menuItem.setChecked(true);
    }

    //methode to check the device's permission
    private boolean checkPermission(String permission){
        int result = ContextCompat.checkSelfPermission(context,permission);
        if(result== PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    //methode to open the Scanner
    private void openScanner() {
        new IntentIntegrator(Scan.this).initiateScan();
    }

    //methode to get the data from Scanner
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //getting the result from scanner
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);

        //checking if result is null or not
        if(result!=null){
            if(result.getContents()!=null){
                //getting the placeId that scanned by user
                placeIdScane = result.getContents();

                //comparing it with the placeId we got from Database
                if(placeIdDB.equals(placeIdScane)){

                    //checking if slot is booked or parked
                    if(SlotStatus.equals("booked")){

                        //checking from database that user has already visited this place or not
                        Log.d("myTag","checking from database that user has already visited this place or not");
                        db.collection("Users").document(userId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()){
                                            //getting the data from database and converting it into object
                                            Log.d("myTag","getting the data from database and converting it into object");
                                            DocumentSnapshot userData = task.getResult();
                                            Map<String,Object> userDataMap = userData.getData();
                                            ArrayList<String> placesVisited = null;
                                            Log.d("myTag","try catch");
                                            try{
                                                Log.d("myTag","in try");
                                                placesVisited = (ArrayList<String>) userData.get("visited");
                                            }catch(Exception e){
                                                Log.d("myTag","in catch");
                                                placesVisited.add(placeIdScane);
                                            }
                                            Log.d("myTag","out of try catch");
                                            if(placesVisited.contains(placeIdScane)){
                                                //user has already visited this place, no need to do anything
                                                Log.d("myTag","user has already visited this place, no need to do anything");

                                            }else{
                                                //adding the new place's id into array
                                                Log.d("myTag","adding the new place's id into array");
                                                placesVisited.add(placeIdScane);
                                            }
                                            //upadting the value into map
                                            Log.d("myTag","upadting the value into map");
                                            userDataMap.put("visited",placesVisited);
                                            //updating the value into databse
                                            db.collection("Users").document(userId)
                                                    .set(userDataMap)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            Log.d("myTag", "onComplete: new place added");
                                                        }
                                                    });

                                        }
                                    }
                                });

                        //slot is book, changing it's status to parked after scane
                        slotHistoryMap.put("Status","parked");

                        //updating the status into user's Database
                        db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(Bookid))
                                .set(slotHistoryMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        //updating data in shred prefrence
                                        SharedPreferences.Editor editor = userData.edit();
                                        editor.putString("Book status","parked");
                                        editor.putInt("bookId",Bookid);
                                        editor.commit();
                                    }
                                });

                        //updating status into parking place's database
                        db.collection("ParkingPlaces").document(placeIdDB).collection("Slots").document(userId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        //getting data from database into object formet
                                        DocumentSnapshot slotDetail = task.getResult();
                                        Map<String,Object> slotDetailMap = slotDetail.getData();
                                        slotDetailMap.put("Status","parked");

                                        Log.d("myTag","data update slot removed");
                                        //updating data into database
                                        db.collection("ParkingPlaces").document(placeIdDB).collection("Slots").document(userId)
                                                .set(slotDetailMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        //going to slots history
                                                        Log.d("myTag","opening intant");
                                                        Intent intent = new Intent(context, Booked.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                    }
                                });

                    }else if(SlotStatus.equals("parked")){
                        //slot if parked, so changing it's status after scane

                        //getting the alert dialog ready
                        AlertDialog.Builder buidler;
                        final AlertDialog dialog;
                        LayoutInflater inflater;

                        //building the alert dialog for conformation of exit
                        buidler = new AlertDialog.Builder(context);
                        inflater = LayoutInflater.from(context);
                        View view = inflater.inflate(R.layout.confirmation_pop, null);

                        //hooks for yes and no button
                        Button noButton = view.findViewById(R.id.conf_no_button);
                        Button yesButton = view.findViewById(R.id.conf_yes_button);
                        TextView message = view.findViewById(R.id.text_alert);
                        message.setText("Are you sure you want to Exit the place and finish your Slot's Booking");

                        //showing the slert dialoge
                        buidler.setView(view);
                        dialog = buidler.create();
                        dialog.show();

                        //setting click listner for yes button
                        yesButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //changing the status to finsihed
                                slotHistoryMap.put("Status","finished");

                                //updating the status in Database
                                db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(Bookid))
                                        .set(slotHistoryMap)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                //updating data in shred prefrence
                                                SharedPreferences.Editor editor = userData.edit();
                                                editor.putString("Book status","finished");
                                                editor.putInt("bookId",Bookid);
                                                editor.commit();

                                            }
                                        });

                                //removing the Booked slot from Parking place's database
                                db.collection("ParkingPlaces").document(placeIdDB).collection("Slots").document(userId)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //updating the available slots after removing the slot's data
                                                db.collection("ParkingPlaces").document(placeIdDB)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if(task.isSuccessful()){
                                                                    DocumentSnapshot placeDetDB = task.getResult();
                                                                    ParkingPlace BookPlace = helper.toParkingPlace(placeDetDB);
                                                                    int CurrentAvailSlots = BookPlace.getAvailableSlots();
                                                                    CurrentAvailSlots++;
                                                                    Map<String,Object> placeDetMap = placeDetDB.getData();
                                                                    placeDetMap.put("availableSlots",CurrentAvailSlots);

                                                                    //adding the updated value to the database
                                                                    db.collection("ParkingPlaces").document(placeIdDB)
                                                                            .set(placeDetMap)
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {
                                                                                    //going to slots history
                                                                                    Intent intent = new Intent(context, Booked.class);
                                                                                    startActivity(intent);
                                                                                    finish();
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });

                        //setting click listner for no button
                        noButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                //going to slots history
                                Intent intent = new Intent(context, Booked.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }

                }else{
                    //placeId didn't match, showing the Toast of Wrong QR code
                    Toast.makeText(context, "Wrong QR code, please Check and try again", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, Booked.class);
                    startActivity(intent);
                    finish();
                }
            }else{
                //QR scanner didn't return any data, showing the Toast of Error
                Toast.makeText(context, "Error Occured, try again", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, Search.class);
                startActivity(intent);
                finish();
            }
        }else{
            //QR scanner didn't scane the code, showing the error
            Toast.makeText(context, "Didn't find any QR code, please try again", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, Search.class);
            startActivity(intent);
            finish();
        }

    }

    //methode to ask permission from user
    private void requestPermission(String permission, int code){
        if(ActivityCompat.shouldShowRequestPermissionRationale(Scan.this,permission)){

        }else{
            ActivityCompat.requestPermissions(Scan.this,new String[]{permission},code);
        }
    }

    //methode to handle the result of the permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    openScanner();
                }
                break;
            case 0:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(context, "Please Allow Permission to Scane QR code", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, Search.class);
                    startActivity(intent);
                    finish();
                }
                break;
        }
    }

    //setting up the bottom nav
    private BottomNavigationView.OnNavigationItemSelectedListener navListner =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    Intent intent;
                    switch (item.getItemId()){
                        case R.id.nav_home:
                            intent = new Intent(context,Home.class);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();
                            break;
                        case R.id.nav_search:
                            intent = new Intent(context,Search.class);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();
                            break;
                        case R.id.nav_scane:
                            intent = new Intent(context,Scan.class);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();
                            break;
                        case R.id.nav_booked:
                            intent = new Intent(context,Booked.class);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();
                            break;
                        case R.id.nav_profile:
                            intent = new Intent(context,UserProfile.class);
                            startActivity(intent);
                            overridePendingTransition(0,0);
                            finish();
                            break;
                    }
                    return true;
                }
            };
}
