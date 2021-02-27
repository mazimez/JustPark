package com.example.tillnow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.regex.Pattern;

public class Signup extends AppCompatActivity {

    //local
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    //"(?=.*[0-9])" +         //at least 1 digit
                    //"(?=.*[a-z])" +         //at least 1 lower case letter
                    //"(?=.*[A-Z])" +         //at least 1 upper case letter
                    "(?=.*[a-zA-Z])" +      //any letter
                    "(?=.*[@#$%^&+=])" +    //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{4,}" +               //at least 4 characters
                    "$");
    private final int REQUEST_CODE = 1;
    private boolean isAuthenticated;
    private String uid;

    //Context
    private Context context=this;

    //classes
    private User user;
    private Helper helper =new Helper();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;

    //Edit text
    private TextInputEditText fullName;
    private TextInputEditText userName;
    private TextInputEditText email;
    private TextInputEditText phoneNum;
    private TextInputEditText password;
    private TextInputEditText confPassword;

    //Button
    private Button phoneOtp;
    private Button submit;

    //ProgressBars
    private ProgressBar progressBar;
    private ProgressBar phoneCheckBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //hooks
        //Edit text
        fullName = findViewById(R.id.fullname);
        userName = findViewById(R.id.username);
        email = findViewById(R.id.emial);
        phoneNum = findViewById(R.id.phoneNum);
        password = findViewById(R.id.password);
        confPassword = findViewById(R.id.confPassword);

        //Buttons
        phoneOtp =findViewById(R.id.phone_otp);
        submit = findViewById(R.id.submit);
        progressBar = findViewById(R.id.progress_bar);
        phoneCheckBar = findViewById(R.id.number_check_bar);
        phoneCheckBar.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        //setting on click listner for OTP
        phoneOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //starting progress bar
                phoneCheckBar.setVisibility(View.VISIBLE);

                //checking if data entered by user is correct
                if(checkSignupForm()){

                    //checing in dataabase is phone number new or not
                    db.collection("Users")
                            .whereEqualTo("Phone Number",phoneNum.getText().toString())
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        //checking if phone number is new
                                        if(task.getResult().isEmpty()){
                                            //adding country code to phone number
                                            String num = "+91"+phoneNum.getText().toString();

                                            //checking if phone number is of right length
                                            if(num.length()==13 && num.startsWith("+91")){
                                                Log.d("myTag", "sendVerificationCode: " + "code sending TO" + num);

                                                //stoping progress bar and starting the send OTP activity
                                                phoneCheckBar.setVisibility(View.GONE);
                                                Intent intent = new Intent(context, PhoneOtp.class);
                                                intent.putExtra("phoneNumber",num);
                                                startActivityForResult(intent,REQUEST_CODE);
                                            }else{
                                                //stoping progress bar and showing wrong number error
                                                phoneNum.setError("wrong number");
                                                phoneCheckBar.setVisibility(View.GONE);
                                                Log.d("myTag", "onClick: wrong number: "+num);
                                            }
                                        }else{
                                            //stoping progress bar and showing the user exist error
                                            phoneCheckBar.setVisibility(View.GONE);
                                            phoneNum.setError("PhoneNumber already exist");
                                        }
                                    } else {
                                        Log.w("myTag", "Error getting documents.", task.getException());
                                    }
                                }
                            });
                }else{
                    //stoping progress bar because data entered by user is not corect
                    phoneCheckBar.setVisibility(View.GONE);
                }
            }
        });

        //seting on click listner for sign up form
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //starting the progress bar
                progressBar.setVisibility(View.VISIBLE);

                //checking if data entered by user is correct
                if(checkSignupForm()){
                    //checking is user is authenticated i.e. user's phone number is verified
                    if(isAuthenticated){
                        //checking in database if user already exist
                        db.collection("Users")
                                .whereEqualTo("User Name",userName.getText().toString())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            //checking if use already exist
                                            if(task.getResult().isEmpty()){
                                                //creating new user since userName not exist
                                                user = new User(
                                                        fullName.getText().toString(),
                                                        userName.getText().toString(),
                                                        email.getText().toString(),
                                                        phoneNum.getText().toString(),
                                                        password.getText().toString()
                                                );
                                                Map<String,Object> userData= new HashMap();
                                                userData.put("Full Name",user.getName());
                                                userData.put("User Name",user.getUsername());
                                                userData.put("Email",user.getEmail());
                                                userData.put("Phone Number",user.getPhoneNo());
                                                userData.put("Password",user.getPassword());
                                                userData.put("isCustomer",true);

                                                //adding the new user into the database
                                                docRef = db.collection("Users").document(uid);
                                                docRef.set(userData)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                //new user added going to log in activity
                                                                Log.d("myTag", "DocumentSnapshot added with ID: " + docRef.getId());
                                                                Intent intent = new Intent(context,Login.class);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.d("myTag", "Error adding document", e);
                                                            }
                                                        });

                                            }else{
                                                //stoping progres bar and showing user already exist error
                                                progressBar.setVisibility(View.GONE);
                                                phoneNum.setError("UserName already exist");
                                            }
                                        } else {
                                            Log.w("myTag", "Error getting documents.", task.getException());
                                        }
                                    }
                                });
                    }else{
                        //stoping progress bar and showing error for user not verified
                        progressBar.setVisibility(View.GONE);
                        phoneOtp.setText("You Have to Verify first");
                        phoneOtp.setBackgroundColor(Color.rgb(205, 50, 50));
                    }
                }else{
                    //stoping progress bar because data entered by user is not correct
                    progressBar.setVisibility(View.GONE);
                }
            }
        });


    }

    //when user comeback from send OTP activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE){
            if(data!=null){
                //getting data about user's authenticationg
                isAuthenticated = data.getBooleanExtra("isAuthenticate",false);
                uid = data.getStringExtra("uid");

                //checking if user is authenticated
                if(isAuthenticated) {
                    //setting the verified text for user
                    phoneOtp.setText("Phone Number Verified");
                    phoneOtp.setBackgroundColor(Color.rgb(50, 205, 50));
                }
            }
        }
    }

    //methode to terminate the sign in acticity
    public void backSignup(View view){
        finish();
    }

    //checking the data entered by user
    public boolean checkSignupForm(){
        boolean isOk=true;
        String FullName = fullName.getText().toString().trim();
        String UserName = userName.getText().toString().trim();
        String Email = email.getText().toString().trim();
        String PhoneNum = phoneNum.getText().toString().trim();
        String Password = password.getText().toString().trim();
        String ConfPassword = confPassword.getText().toString().trim();

        //checking firstname
        if(FullName.length()>=4 && FullName.length()<=20){
            //add more checks if you want
        }else{
            fullName.setError("FullName has to be between 4-20 characters.");
            isOk=false;
        }

        //checking username
        if(UserName.length()>=4 && UserName.length()<=15){
            //add more checks if you want
        }else{
            userName.setError("UserName has to be between 4-15 characters.");
            isOk=false;
        }

        //checking E-mail
        if(!Email.isEmpty()){
            if(Patterns.EMAIL_ADDRESS.matcher(Email).matches()){
                //add more checks if you want
            }else{
                email.setError("invalid mail");
                isOk=false;
            }
        }else{
            email.setError("Email can't be empty");
            isOk=false;
        }

        //checking phone number
        String phonePattern ="^(*[0-9])$";
        if(PhoneNum.length()==10){
            //add more checks if you want
        }else{
            phoneNum.setError("Phone Number is invalid");
            isOk=false;
        }

        //checking password
        if(!Password.isEmpty()){
            if(PASSWORD_PATTERN.matcher(Password).matches()){
                //add more checks if you want
            }else{
                password.setError("password must have speacial characterno with no wide space");
                isOk=false;
            }
        }else{
            password.setError("password can't be empty");
            isOk=false;
        }

        //checking confirm password
        if(ConfPassword.equals(Password)){
            //add more checks if you want
        }else{
            confPassword.setError("Password must be same");
            isOk=false;
        }

        return isOk;
    }
}
