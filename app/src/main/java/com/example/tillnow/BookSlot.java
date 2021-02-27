package com.example.tillnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.tillnow.main.Booked;
import com.example.tillnow.main.ParkingPlace;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookSlot extends AppCompatActivity {

    //local
    private String placeId;
    private String PlaceName;
    private float pricePerSlot;
    private int TotalHours=0;
    private float TotalPrice;
    boolean isValid = false;

    //Context
    private Context context=this;

    //TextView
    private TextView placeName;
    private TextView TimeTo;
    private TextView TimeFrom;
    private TextView bookError;
    private TextView paymentAmount;

    //ImageView
    private ImageView placePic;

    //Button
    private Button paymeny;
    private Button startBook;

    //shared profrences
    private SharedPreferences userData;

    //progress bar
    private ProgressBar bookProgress;

    //classes
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private Uri imageUri;
    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private StorageReference storageReference = firebaseStorage.getReference();
    private Helper helper = new Helper();
    private Calendar ToTimeCal= Calendar.getInstance();;
    private Calendar FromTimeCal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_slot);

        //hooks
        placeName = findViewById(R.id.placeName);
        placePic = findViewById(R.id.place_pic);
        TimeFrom = findViewById(R.id.time_from);
        TimeTo = findViewById(R.id.time_to);
        startBook = findViewById(R.id.startBook);
        bookError = findViewById(R.id.book_error);
        paymentAmount = findViewById(R.id.payment_amount);
        bookProgress = findViewById(R.id.book_progress_bar);

        //setting up the from time
        FromTimeCal= Calendar.getInstance();
        TimeFrom.setText(helper.getOnlyTime(FromTimeCal));

        //checking if the user has any booking going on
        userData = getSharedPreferences("UserData",MODE_PRIVATE);
        String userId = userData.getString("uid",null);
        db.collection("Users").document(userId).collection("SlotsHistory")
                .whereEqualTo("Status","booked")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(task.getResult().isEmpty()){
                                //user dont have any slot booked
                            }else{
                                //user already have a book going, so finishing the activity
                                Toast.makeText(context, "You already have one slot booked", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    }
                });


        //getting data from caller activity
        Bundle extras = getIntent().getExtras();
        placeId = extras.getString("palceId");
        pricePerSlot = extras.getFloat("pricePerSlot");


        //setting the click listner for To time
        TimeTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //creting the Timepicker dialog to pick the time
                TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                                int ToHour = hourOfDay;
                                int ToMinute = minute;

                                //setting the To time to the  time sellected by User
                                ToTimeCal.set(ToTimeCal.get(Calendar.YEAR),ToTimeCal.get(Calendar.MONTH),ToTimeCal.get(Calendar.DAY_OF_MONTH),ToHour,ToMinute);

                                //cheking if To time is valid
                                if(ToTimeCal.after(FromTimeCal)){
                                    bookError.setVisibility(View.GONE);

                                    //getting the diffrent between From and To time
                                    long diffrence = ToTimeCal.getTime().getTime()-FromTimeCal.getTime().getTime();
                                    int unit = helper.millisToUnit(diffrence);

                                    //checking if time is enough to consider as an hour and getting Total price for that time
                                    if(unit>=4){
                                        bookError.setVisibility(View.GONE);
                                        TotalHours = Math.round(unit/6F);
                                        TotalPrice = TotalHours*pricePerSlot;

                                        //cheking if total hours and total price is valid
                                        if(TotalHours>0 && TotalPrice>0){
                                            isValid =true;
                                        }else{
                                            //showing the error that time is not enough
                                            TotalHours = 0;
                                            TotalPrice = (float) 0.0;
                                            paymentAmount.setText("Total Amount : RS. 0 Only");
                                            bookError.setText("can't book slot for this time");
                                            bookError.setVisibility(View.VISIBLE);
                                        }
                                        paymentAmount.setText("Total Amount : RS. "+TotalPrice+" Only");
                                    }else{
                                        //showing the error that time is not enough
                                        TotalHours = 0;
                                        TotalPrice = (float) 0.0;
                                        paymentAmount.setText("Total Amount : RS. 0 Only");
                                        bookError.setText("can't book slot for less then 1 hour");
                                        bookError.setVisibility(View.VISIBLE);
                                    }
                                }else{
                                    //showing the error that time selected is not valid
                                    TotalHours = 0;
                                    TotalPrice = (float) 0.0;
                                    paymentAmount.setText("Total Amount : RS. 0 Only");
                                    bookError.setText("Please choose the valid time");
                                    bookError.setVisibility(View.VISIBLE);
                                }
                                //setting the text of the To time
                                TimeTo.setText(DateFormat.format("hh:mm aa",ToTimeCal));
                            }
                        },12,0,false);
                //showing the time picker dialog to user
                timePickerDialog.show();
            }
        });

        //getting data of parking place from database
        db.collection("ParkingPlaces").document(placeId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            //getting data from database and converting in into object
                            DocumentSnapshot placeDetDB = task.getResult();
                            ParkingPlace placeUpdated = helper.toParkingPlace(placeDetDB);
                            Map<String,Object> placeDetMap = placeDetDB.getData();
                            PlaceName = placeDetDB.getString("name");
                            placeName.setText(placeDetDB.getString("name"));

                            //checking if place's closed ot don't have slots
                            int availableSlots = placeUpdated.getAvailableSlots();
                            Calendar rightNow = Calendar.getInstance();
                            if (rightNow.after(placeUpdated.getOpeningTime()) && rightNow.before(placeUpdated.getClosingtime())) {
                                if(availableSlots>0){
                                    //slots are available
                                }else{
                                    //slots arer full, finishing activity
                                    Toast.makeText(context, "Slots are full", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }else{
                                //place's closed finishing activity
                                Toast.makeText(context, "Place's closed", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            //getting the photo of parking place from database
                            StorageReference riversRef = storageReference.child("parking places/place"+placeId+".jpg");
                            try {
                                final File localFile = File.createTempFile("image","jpg");
                                riversRef.getFile(localFile)
                                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                //setiing the photo from database to Image view
                                                Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                                placePic.setImageBitmap(bitmap);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d("myTag", "onFailure: place picture not found");
                                            }
                                        });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            Log.d("myTag", "onComplete: task unsuccesfull, chek ID");
                        }
                    }
                });

        //setting on click listner for start the booking
        startBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isValid){
                    //starting the progress bar
                    bookProgress.setVisibility(View.VISIBLE);

                    //getting user data from shared prefrence
                    userData = getSharedPreferences("UserData",MODE_PRIVATE);
                    String userName = userData.getString("userName",null);
                    final String userId = userData.getString("uid",null);

                    //converting From time into object
                    Map<String,Object> from= new HashMap();
                    from.put("hours",FromTimeCal.get(Calendar.HOUR));
                    from.put("minutes",FromTimeCal.get(Calendar.MINUTE));

                    //coverting To time into object
                    Map<String,Object> to= new HashMap();
                    to.put("hours",ToTimeCal.get(Calendar.HOUR));
                    to.put("minutes",ToTimeCal.get(Calendar.MINUTE));

                    //creating the Slot data object
                    Map<String,Object> slotData= new HashMap();
                    slotData.put("UserName",userName);
                    slotData.put("from",from);
                    slotData.put("to",to);
                    slotData.put("Status","booked");
                    slotData.put("totalHours",TotalHours);
                    slotData.put("totalPrice",TotalPrice);

                    //adding the slot data object into the database
                    docRef = db.collection("ParkingPlaces").document(placeId).collection("Slots").document(userId);
                    docRef.set(slotData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //getting the parking place's infomationg to edit the slot details
                                    db.collection("ParkingPlaces").document(placeId)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if(task.isSuccessful()){
                                                        DocumentSnapshot placeDetDB = task.getResult();
                                                        ParkingPlace BookPlace = helper.toParkingPlace(placeDetDB);
                                                        int CurrentAvailSlots = BookPlace.getAvailableSlots();
                                                        CurrentAvailSlots--;
                                                        Map<String,Object> placeDetMap = placeDetDB.getData();
                                                        placeDetMap.put("availableSlots",CurrentAvailSlots);

                                                        //changnig the slots infomation into database
                                                        db.collection("ParkingPlaces").document(placeId)
                                                                .set(placeDetMap)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Log.d("myTag", "onSuccess: value updated in DB");
                                                                    }
                                                                });

                                                    }
                                                }
                                            });

                                    //adding the slot history into user's database
                                    db.collection("Users").document(userId).collection("SlotsHistory")
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        //getting the bookId
                                                        final int bookId =task.getResult().size()+1;

                                                        //creating data in map formet to add into database
                                                        Map<String,Object> BookingDet= new HashMap();
                                                        BookingDet.put("PlaceName",PlaceName);
                                                        BookingDet.put("TotalPrice",TotalPrice);
                                                        BookingDet.put("TotalHours",TotalHours);
                                                        BookingDet.put("Status","booked");
                                                        BookingDet.put("BookedId",bookId);
                                                        BookingDet.put("placeId",placeId);

                                                        //adding slot history data into database
                                                        db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(bookId))
                                                                .set(BookingDet)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        //updating data in shred prefrence
                                                                        SharedPreferences.Editor editor = userData.edit();
                                                                        editor.putString("Book status","booked");
                                                                        editor.putInt("bookId",bookId);
                                                                        editor.commit();

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
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    bookError.setVisibility(View.VISIBLE);
                                    bookError.setText("some problem occure");
                                }
                            });
                }else{
                    //showing the error that time is invalid
                    bookError.setText("can't book slot for this time");
                    bookError.setVisibility(View.VISIBLE);
                }
            }
        });

    }
}