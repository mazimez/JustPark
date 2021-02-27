package com.example.tillnow.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tillnow.Login;
import com.example.tillnow.R;
import com.example.tillnow.Welcome;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class UserProfile extends AppCompatActivity {

    //local
    private  String uid;
    private boolean isLogedin;
    private String fullName;
    private String userName;
    private String email;
    private String phoneNum;
    private int bookedSlots;
    private int visited;

    //TextView
    private TextView TfullName;
    private TextView TuserName;
    private TextView BookedSlots;
    private TextView Visited;

    //EditText
    private EditText FullName;
    private EditText UserName;
    private EditText Email;
    private EditText PhoneNum;

    //ImageView
    private ImageView profileImage;

    //context
    private Context context=this;

    //Shared profrence
    private SharedPreferences userData;

    //classes
    private FirebaseDatabase database;
    private DatabaseReference reference;
    private Uri imageUri;
    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private StorageReference storageReference = firebaseStorage.getReference();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //setting up bottom menu
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListner);
        Menu menu =bottomNav.getMenu();
        MenuItem menuItem = menu.getItem(4);
        menuItem.setChecked(true);

        //hooks
        //TextView
        TfullName = findViewById(R.id.Tfullname);
        TuserName = findViewById(R.id.Tusername);
        BookedSlots= findViewById(R.id.current_parkings);
        Visited = findViewById(R.id.places_visited);


        //Edittext
        FullName = findViewById(R.id.profile_fullname);
        UserName = findViewById(R.id.profile_username);
        Email = findViewById(R.id.profile_email);
        PhoneNum = findViewById(R.id.profile_phonenum);

        //ImageView
        profileImage = findViewById(R.id.profile_image);

        //getting the data from shared profrence
        userData = getSharedPreferences("UserData",MODE_PRIVATE);
        isLogedin = userData.getBoolean("isLogedin",false);
        uid = userData.getString("uid",null);
        fullName = userData.getString("fullName",null);
        userName = userData.getString("userName",null);
        email = userData.getString("email",null);
        phoneNum = userData.getString("phoneNum",null);
        bookedSlots = userData.getInt("BookedSlots",0);
        visited = userData.getInt("PlacesVisited",0);



        //cheking if user is logged in
        if(isLogedin){
            //retrive the photo from the database and set to profile photo
            StorageReference riversRef = storageReference.child("profile images/"+uid);
            try {
                final File localFile = File.createTempFile("image","jpg");
                riversRef.getFile(localFile)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                profileImage.setImageBitmap(bitmap);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("myTag", "onFailure: please upload image first");
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }

            //setting the user's data to textViews
            TfullName.setText(fullName);
            TuserName.setText(userName);
            FullName.setText(fullName);
            UserName.setText(userName);
            Email.setText(email);
            PhoneNum.setText(phoneNum);
            BookedSlots.setText(String.valueOf(bookedSlots));
            Visited.setText(String.valueOf(visited));


            //getting the data of user's booked Slots
            db.collection("Users").document(uid).collection("SlotsHistory")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                int BookedSlots =0;
                                for(QueryDocumentSnapshot history : task.getResult()){
                                    BookedSlots++;
                                }
                                updateBookedSlots(BookedSlots);
                            }else{
                                Log.d("myTag", "onComplete: task failed");
                            }
                        }
                    });

            //getting the data for place's visited from database
            db.collection("Users").document(uid)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                //getting the data from database and converting it into object
                                DocumentSnapshot userData = task.getResult();
                                Map<String,Object> userDataMap = userData.getData();
                                ArrayList<String> placesVisited = (ArrayList<String>) userData.get("visited");
                                int PlacesVisited = placesVisited.size();
                                updatePlacesVisited(PlacesVisited-1);
                            }
                        }
                    });

        }else{
            //taking user to login activity because he is not logged in
            Intent intent = new Intent(context, Login.class);
            startActivity(intent);
            finish();
        }

        //setting the click listner fot profile to select from device
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,1);
            }
        });

    }

    //methode to update the places visited
    private void updatePlacesVisited(int placesVisited) {
        Visited.setText(String.valueOf(placesVisited));

        //updating data in shared prefrences
        SharedPreferences.Editor editor = userData.edit();
        editor.putInt("PlacesVisited",placesVisited);
        editor.commit();
    }

    //methode to update the total number of booked slots
    private void updateBookedSlots(int bookedSlots) {
        BookedSlots.setText(String.valueOf(bookedSlots));

        //updating data in shared prefrences
        SharedPreferences.Editor editor = userData.edit();
        editor.putInt("BookedSlots",bookedSlots);
        editor.commit();
    }

    //methode to handle the photo once selected from device
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK && data!=null){
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadPhoto();
        }
    }

    //methode for uploading the photo into the database
    public void uploadPhoto(){
        StorageReference riversRef = storageReference.child("profile images/"+uid);
        riversRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Log.d("myTag", "onSuccess: done uploading");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Log.d("myTag", "onFailure: "+getApplicationContext());
                    }
                });
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

    public void logOut(View view1){
        //getting the alert dialog ready
        AlertDialog.Builder buidler;
        final AlertDialog dialog;
        LayoutInflater inflater;

        //building the alert dialog for conformation of Logout
        buidler = new AlertDialog.Builder(context);
        inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.confirmation_pop, null);

        //hooks for yes and no button
        Button noButton = view.findViewById(R.id.conf_no_button);
        Button yesButton = view.findViewById(R.id.conf_yes_button);
        TextView message = view.findViewById(R.id.text_alert);
        message.setText("Are you sure you want to Logout");

        //showing the alert dialoge
        buidler.setView(view);
        dialog = buidler.create();
        dialog.show();

        //setting click listner for yes button
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = userData.edit();
                editor.putBoolean("isLogedin",false);
                editor.remove("uid");
                editor.remove("fullName");
                editor.remove("userName");
                editor.remove("email");
                editor.remove("phoneNum");
                editor.commit();
                Intent intent = new Intent(context, Welcome.class);
                startActivity(intent);
                dialog.dismiss();
                finish();
            }
        });

        //setting click listner for no button
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}
