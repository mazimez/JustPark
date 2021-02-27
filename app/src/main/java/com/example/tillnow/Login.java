package com.example.tillnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.tillnow.main.Home;
import com.example.tillnow.main.ParkingHandler;
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

public class Login extends AppCompatActivity {
    //local
    private String UserName;
    private String Password;

    //context
    private Context context=this;

    //EditText
    private TextInputEditText userName;
    private TextInputEditText password;

    //Buttons
    private Button forgotPass;
    private Button login;

    //Progressbar
    private ProgressBar progressBar;

    //Shared prefrences
    private SharedPreferences sharedPreferences;

    //classes
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //hooks
        userName = findViewById(R.id.lusername);
        password = findViewById(R.id.lpassword);
        forgotPass = findViewById(R.id.forgot_pass);
        login = findViewById(R.id.login);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        forgotPass.setVisibility(View.GONE);

    }

    //methode to login the user
    public void loginUser(View view){
        //cheacking if the values entered by user is correct
        if(checkLoginForm()){

            //start the progress bar and take the data
            progressBar.setVisibility(View.VISIBLE);
            UserName = userName.getText().toString();
            Password = password.getText().toString();

            //checking the database for the userName entered by user
            db.collection("Users")
                    .whereEqualTo("User Name",UserName)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                //checking if there is userName entered by user
                                if(task.getResult().isEmpty()){
                                    //stoping the progress bar and showing error for User not exist
                                    progressBar.setVisibility(View.GONE);
                                    userName.setError("User Not Exist,please check the username and try again");
                                }else{
                                    //getting all the data of user from database
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String uidFromDB = document.getId();
                                        String fullnameFromDB = document.getString("Full Name");
                                        String usernameFromDB = document.getString("User Name");
                                        String emailFromDB = document.getString("Email");
                                        String phoneFromDB = document.getString("Phone Number");
                                        String passwordFromDB = document.getString("Password");
                                        Boolean isCustomer = document.getBoolean("isCustomer");

                                        //checking if password entered by user is correct
                                        if(passwordFromDB.equals(Password)){
                                            //creating shared prefrence with user data
                                            sharedPreferences = getSharedPreferences("UserData",MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean("isLogedin",true);
                                            editor.putString("uid",uidFromDB);
                                            editor.putString("fullName",fullnameFromDB);
                                            editor.putString("userName",usernameFromDB);
                                            editor.putString("email",emailFromDB);
                                            editor.putString("phoneNum",phoneFromDB);
                                            editor.putBoolean("isCustomer",isCustomer);

                                            if(isCustomer){
                                                editor.commit();
                                                //taking user to Home Screen
                                                Intent intent = new Intent(context, Home.class);
                                                startActivity(intent);
                                            }else{
                                                editor.putString("workId",document.getString("workId"));
                                                editor.commit();
                                                //taking user to parking handler
                                                Intent intent = new Intent(context, ParkingHandler.class);
                                                startActivity(intent);
                                            }
                                            finish();
                                        }else{
                                            //stoping progress bar and showing error of wrong password
                                            progressBar.setVisibility(View.GONE);
                                            password.setError("Wrong Password, please try again");
                                        }

                                    }
                                }
                            } else {
                                Log.w("myTag", "Error getting documents.", task.getException());
                            }
                        }
                    });
        }
    }

    //methode to terminate login activity
    public void backLogin(View view){
        finish();
    }

    //methode to check the data entered by user
    public boolean checkLoginForm(){
        boolean isOk=true;
        UserName = userName.getText().toString();
        Password = password.getText().toString();

        //checking username
        if(UserName.length()>=4 && UserName.length()<=15){
            //add more checks if you want
        }else{
            userName.setError("Username to be between 4-15 characters.");
            isOk=false;
        }

        //checking password
        String passwordPattern = "^"+"(?=.*[a-zA-z])"+"(?=.*[@#$%^&+=])"+"(?=\\s+$)"+".(4,)"+"$";
        if(Password.length()>=5){
            //add more checks if you want
        }else{
            password.setError("must have atleast 5 characters");
            isOk=false;
        }

        return isOk;
    }
}
