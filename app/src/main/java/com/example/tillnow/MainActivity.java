package com.example.tillnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tillnow.main.Home;
import com.example.tillnow.main.ParkingHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //local
    private boolean isNew=true;
    private boolean isLogedin=false;
    private static int SPLASH_TIME_OUT = 2000;

    //Context
    private Context context=this;

    //TextView
    private TextView tagline;

    //ImageView
    private ImageView logo,left,right;

    //Animations
    private Animation bottomAnimation, middleAnimation,middleAnimationLeft,middleAnimationRight;

    //SharedPrefrence
    private SharedPreferences firstTime;
    private SharedPreferences userData;

    //classes
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //loading up animations
        bottomAnimation = AnimationUtils.loadAnimation(context,R.anim.bottom_animation);
        middleAnimation = AnimationUtils.loadAnimation(context,R.anim.middle_animation);
        middleAnimationLeft = AnimationUtils.loadAnimation(context,R.anim.middle_animation_left);
        middleAnimationRight = AnimationUtils.loadAnimation(context,R.anim.middle_animation_right);

        //hooks
        logo = findViewById(R.id.sign);
        left = findViewById(R.id.left);
        right = findViewById(R.id.right);
        tagline = findViewById(R.id.tagLine);

        //setting up animations
        left.setAnimation(middleAnimationLeft);
        logo.setAnimation(middleAnimation);
        right.setAnimation(middleAnimationRight);
        tagline.setAnimation(bottomAnimation);

        //setting shared prefrence
        firstTime = getSharedPreferences("onBoardingScreen",MODE_PRIVATE);
        isNew = firstTime.getBoolean("new",true);

        //calling after delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //checking up user open the app first time
                if(isNew){
                    //going to on bearding
                    Intent intent = new Intent(context,OnBoarding.class);
                    startActivity(intent);
                    finish();
                }else{
                    //getting users data from shared prefrence
                    userData = getSharedPreferences("UserData",MODE_PRIVATE);
                    isLogedin = userData.getBoolean("isLogedin",false);

                    //checking if user is logged in
                    if(isLogedin){
                        String userName = userData.getString("userName",null);

                        //checking in database that user exist
                        db.collection("Users")
                                .whereEqualTo("User Name",userName)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            if(task.getResult().isEmpty()){
                                                //getting to welcome screen because user is not on database
                                                Intent intent = new Intent(context,Welcome.class);
                                                startActivity(intent);
                                                finish();
                                            }else{
                                                Log.d("myTag", "updating the shared prefrence from database");
                                                //updating the shared prefrence from database
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    String uidFromDB = document.getId();
                                                    String fullnameFromDB = document.getString("Full Name");
                                                    String usernameFromDB = document.getString("User Name");
                                                    String emailFromDB = document.getString("Email");
                                                    String phoneFromDB = document.getString("Phone Number");
                                                    Boolean isCustomer = document.getBoolean("isCustomer");
                                                    SharedPreferences.Editor editor = userData.edit();
                                                    editor.putString("uid",uidFromDB);
                                                    editor.putString("fullName",fullnameFromDB);
                                                    editor.putString("userName",usernameFromDB);
                                                    editor.putString("email",emailFromDB);
                                                    editor.putString("phoneNum",phoneFromDB);
                                                    editor.putBoolean("isCustomer",isCustomer);
                                                    if (isCustomer){
                                                        editor.commit();
                                                        //taking customer to home screen
                                                        Intent intent = new Intent(context,Home.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }else{
                                                        editor.putString("workId",document.getString("workId"));
                                                        editor.commit();
                                                        //taking parking handler to his activity
                                                        Intent intent = new Intent(context, ParkingHandler.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                }

                                            }
                                        } else {
                                            Log.w("myTag", "Error getting documents.", task.getException());
                                        }
                                    }
                                });
                    }else{
                        //going to welcome screen because user is not looged in
                        Intent intent = new Intent(context,Welcome.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        },SPLASH_TIME_OUT);
    }
}
