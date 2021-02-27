package com.example.tillnow.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;


import com.example.tillnow.BookSlot;
import com.example.tillnow.Helper;
import com.example.tillnow.PhoneOtp;
import com.example.tillnow.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static java.lang.String.valueOf;

public class Search extends AppCompatActivity implements OnMapReadyCallback {

    //local

    //context
    private Context context=this;

    //TextView
    private TextView placeName;
    private TextView TotalSlots;
    private TextView AvailableSlots;
    private TextView PricePerSlot;
    private TextView OpeningTime;
    private TextView ClosingTime;

    //ImageView
    private ImageView placePicture;

    //Button
    private Button BookSlotBtn;
    private Button SlotNotAvail;
    private Button PlaceClosed;
    private Button AlreadyBooked;

    //SearchView
    private SearchView searchBar;

    //ListView
    private ListView placeList;
    private ArrayList<String> PlaceList = new ArrayList<String>();
    private ArrayAdapter<String> searchAdapter;

    //Shared prefrence
    private SharedPreferences userData;

    //classes
    private GoogleMap mMap;
    private ArrayList<ParkingPlace> placesList;
    private FusedLocationProviderClient Client;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private Helper helper=new Helper();
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    private Uri imageUri;
    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private StorageReference storageReference = firebaseStorage.getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //hooks
        searchBar = findViewById(R.id.searchBar);
        placeList = (ListView)findViewById(R.id.placeList);

        //setting the query listner for the search bar
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchBar.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //setting the item on the search result
                placeList.setVisibility(View.VISIBLE);
                searchAdapter.getFilter().filter(s);
                return false;
            }

        });

        //setting the click listner for the item in the search bar
        placeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                placeList.setVisibility(View.GONE);
                searchBar.clearFocus();
                String name = searchAdapter.getItem(i);
                LatLng placeLocation;
                //getting the location of the place which's item is clicked
                for(ParkingPlace place : placesList){
                    if(name.equals(place.getPlaceName())){
                        placeLocation = place.getLocation();
                        //moving the camara to that location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation,14));
                    }
                }

                //creating the pop up for that clicked item
                createPopup(name);
            }
        });

        //creating the fragment for the map to show
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Client = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);

        //setting up bottom menu
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListner);
        Menu menu =bottomNav.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);
    }

    //methode for the custom icon on the map
    private BitmapDescriptor bitmapDescriptorFromVector(Context context,int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context,vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap= Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),vectorDrawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //methode to get current location
    public void getCurrentLocation(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Task<Location> task = Client.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        LatLng currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("You Are Here").icon(bitmapDescriptorFromVector(context,R.drawable.ic_drive_eta_black_24dp)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,14));
                    }
                }
            });
        }
    }


    //methode to call once the map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //getting the current location with permission
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
        }

        //getting the places data and storing in into the array
        placesList = new ArrayList<>();
        db.collection("ParkingPlaces")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot placeDB : task.getResult()) {

                                //get all the places from database and store it as parkingplace object
                                placesList.add(helper.toParkingPlace(placeDB));
                                PlaceList.add(placeDB.getString("name"));
                                Calendar rightNow = Calendar.getInstance();

                                //showing the marker of the places that we got from database
                                for(ParkingPlace p: placesList){
                                    if(p.getAvailableSlots()==0 || rightNow.before(p.getOpeningTime()) || rightNow.after(p.getClosingtime())){
                                        mMap.addMarker(new MarkerOptions()
                                                .title(p.getPlaceName())
                                                .position(p.getLocation())
                                                .icon(bitmapDescriptorFromVector(context,R.drawable.parking_icon_black)));
                                    }else if(p.getAvailableSlots()<=(p.getTotalSlots()/3)){
                                        mMap.addMarker(new MarkerOptions()
                                                .title(p.getPlaceName())
                                                .position(p.getLocation())
                                                .icon(bitmapDescriptorFromVector(context,R.drawable.parking_icon_red)));
                                    }else if(p.getAvailableSlots()<=(p.getTotalSlots()/2)){
                                        mMap.addMarker(new MarkerOptions()
                                                .title(p.getPlaceName())
                                                .position(p.getLocation())
                                                .icon(bitmapDescriptorFromVector(context,R.drawable.parking_icon_orange)));
                                    }else{
                                        mMap.addMarker(new MarkerOptions()
                                                .title(p.getPlaceName())
                                                .position(p.getLocation())
                                                .icon(bitmapDescriptorFromVector(context,R.drawable.parking_icon_green)));
                                    }
                                }
                            }
                            //setting the search adapter for the search bar
                            searchAdapter = new ArrayAdapter<>(context,android.R.layout.simple_list_item_1,PlaceList);
                            placeList.setAdapter(searchAdapter);
                        }else{
                            Log.d("myTag", "Error "+task.getException());
                        }
                    }
                });


        //methode for the click on any marker
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String name = marker.getTitle();
                createPopup(name);
                return false;
            }
        });

    }

    //methode to creating the popup for the  place
    public void createPopup(String name){
        //getting the specific palce from the array
        for(final ParkingPlace place : placesList){
            if(name.equals(place.getPlaceName())){

                //creating the builfer for the pop up and hooks
                builder = new AlertDialog.Builder(context);
                final View view = getLayoutInflater().inflate(R.layout.popup,null);
                placePicture = view.findViewById(R.id.place_photo);

                //getting the photo of place from the database
                StorageReference riversRef = storageReference.child("parking places/place"+place.getUid()+".jpg");
                Log.d("myTag", "onMarkerClick: "+place.getUid());
                try {
                    final File localFile = File.createTempFile("image","jpg");
                    riversRef.getFile(localFile)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    //setting that photo to tha image view of the pop up
                                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                    placePicture.setImageBitmap(bitmap);
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

                //getting the place's information from the database
                db.collection("ParkingPlaces").document(place.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    //getting the place's data in onject and setting the hooks with data
                                    DocumentSnapshot placeDetDB = task.getResult();
                                    ParkingPlace placeUpdated = helper.toParkingPlace(placeDetDB);
                                    placeName = view.findViewById(R.id.place_name);
                                    placeName.setText(placeUpdated.getPlaceName());

                                    AvailableSlots = view.findViewById(R.id.slots_detail);
                                    AvailableSlots.setText("Available Slots: "+placeUpdated.getAvailableSlots()+"/"+place.getTotalSlots());

                                    PricePerSlot = view.findViewById(R.id.price_perslots);
                                    PricePerSlot.setText("Price per Slots: Rs."+(int) placeUpdated.getPricePerSlot()+" Only");

                                    OpeningTime = view.findViewById(R.id.open_time);
                                    OpeningTime.setText("Opening Time: "+helper.getOnlyTime(placeUpdated.getOpeningTime()));

                                    ClosingTime = view.findViewById(R.id.close_time);
                                    ClosingTime.setText("Closing Time: "+helper.getOnlyTime(placeUpdated.getClosingtime()));

                                    BookSlotBtn = view.findViewById(R.id.bookSlot);
                                    SlotNotAvail = view.findViewById(R.id.SlotNotAvail);
                                    PlaceClosed = view.findViewById(R.id.place_closed);
                                    AlreadyBooked = view.findViewById(R.id.already_booked);

                                    //checking the place is open or closed and also that the slot is available or not
                                    int availableSlots = placeUpdated.getAvailableSlots();
                                    Calendar rightNow = Calendar.getInstance();
                                    if (rightNow.after(placeUpdated.getOpeningTime()) && rightNow.before(placeUpdated.getClosingtime())) {
                                        if(availableSlots>0){
                                            BookSlotBtn.setVisibility(View.VISIBLE);
                                        }else{
                                            SlotNotAvail.setVisibility(View.VISIBLE);
                                        }
                                    }else{
                                        AvailableSlots.setText("Available Slots: Place's Closed");
                                        AvailableSlots.setTextColor(Color.rgb(255,0,0));
                                        PlaceClosed.setVisibility(View.VISIBLE);
                                    }

                                    //checking that is user already has any booked slot going on
                                    userData = getSharedPreferences("UserData",MODE_PRIVATE);
                                    String userId = userData.getString("uid",null);
                                    String userBookStatus = userData.getString("Book status","finished");

                                    if(userBookStatus.equals("booked") || userBookStatus.equals("parked")){
                                        //showing that the user can't book another slot because one slot is already booked
                                        BookSlotBtn.setVisibility(View.GONE);
                                        SlotNotAvail.setVisibility(View.GONE);
                                        PlaceClosed.setVisibility(View.GONE);
                                        AlreadyBooked.setVisibility(View.VISIBLE);
                                    }else{
                                        //user dont have any slot booked
                                    }

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
                                                            //showing that the user can't book another slot because one slot is already going
                                                            BookSlotBtn.setVisibility(View.GONE);
                                                            SlotNotAvail.setVisibility(View.GONE);
                                                            PlaceClosed.setVisibility(View.GONE);
                                                            AlreadyBooked.setVisibility(View.VISIBLE);
                                                        }
                                                    }
                                                }
                                            });
                                }else{
                                    Log.d("myTag", "onComplete: task unsuccesfull, chek ID");
                                }
                            }
                        });

                //setting the click listner for the book slot button
                BookSlotBtn = view.findViewById(R.id.bookSlot);
                BookSlotBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //getting the data of place from the database
                        db.collection("ParkingPlaces").document(place.getUid())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful()){

                                            //getting data and converting it into object
                                            DocumentSnapshot placeDetDB = task.getResult();
                                            Map<String,Object> placeDetMap = placeDetDB.getData();

                                            //cheking if the  slot is available in the place
                                            int availableSlots = Integer.parseInt(placeDetMap.get("availableSlots").toString());
                                            if(availableSlots!=0){
                                                //taking user to book slot activity
                                                Intent intent = new Intent(context, BookSlot.class);
                                                intent.putExtra("palceId",place.getUid());
                                                intent.putExtra("pricePerSlot",place.getPricePerSlot());
                                                startActivity(intent);
                                            }else{
                                                Log.d("myTag", "onComplete: slot not available");
                                            }

                                        }else{
                                            Log.d("myTag", "onComplete: task unsuccesfull, chek ID");
                                        }
                                    }
                                });
                    }
                });
                //show the pop up
                builder.setView(view);
                alertDialog = builder.create();
                alertDialog.show();

            }else {
                //
            }
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


    //on permission result for the current positon
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode ==44){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            }
        }
    }
}
