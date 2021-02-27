package com.example.tillnow.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tillnow.Helper;
import com.example.tillnow.R;
import com.example.tillnow.Welcome;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Map;

public class ParkingHandler extends AppCompatActivity {

    //local
    private String workId;
    private int localVehicle=0;

    //context
    private Context context=this;

    //TextView
    private TextView parkedVehicle;

    //recyler view
    private RecyclerView parkedData;

    //shared profrences
    private SharedPreferences userData;
    private SharedPreferences placeData;

    //classes
    private Helper helper = new Helper();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private FirestoreRecyclerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_handler);

        //hooks
        parkedData = findViewById(R.id.place_record);
        parkedVehicle = findViewById(R.id.parked_vehicles);

        //getting user data from shared prefrence
        userData = getSharedPreferences("UserData",MODE_PRIVATE);
        workId = userData.getString("workId",null);

        //getting place's data from shared prefrence
        placeData =getSharedPreferences("placeData",MODE_PRIVATE);
        localVehicle = placeData.getInt("localVehicle",0);
        parkedVehicle.setText("Total Vehicles: "+localVehicle);

        //creating query to get parked details
        Query query = db.collection("ParkingPlaces").document(workId).collection("Slots").whereEqualTo("Status","parked");

        //creating to Recycler option to get data in objects formet
        FirestoreRecyclerOptions<ParkingHandlerModel> options = new FirestoreRecyclerOptions.Builder<ParkingHandlerModel>()
                .setQuery(query,ParkingHandlerModel.class)
                .build();

        //creating adapter for recycler view to show data in user readable formet
        adapter = new FirestoreRecyclerAdapter<ParkingHandlerModel,ParkedHolder>(options) {
            @Override
            public int getItemCount() {
                return super.getItemCount();
            }

            @NonNull
            @Override
            public ParkedHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parked_item,parent,false);
                return new ParkedHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ParkedHolder holder, int position, @NonNull ParkingHandlerModel model) {
                //getting the status
                String status = model.getStatus();
                //checking if the slot is booked or not
                if(status.equals("booked")){
                    holder.container.setVisibility(View.GONE);
                }else{
                    if(status.equals("parked")){
                        holder.container.setVisibility(View.VISIBLE);
                    }
                }
                int TotalVehicleInside = adapter.getItemCount()+localVehicle;
                parkedVehicle.setText("Total Vehicles: "+TotalVehicleInside);

                //setting all the holder's data
                holder.userName.setText(model.getUserName());
                holder.totalPrice.setText("Total price: "+model.getTotalPrice());
                holder.totalHours.setText("Total Hours: "+model.getTotalHours());
                holder.status.setText("Status: "+model.getStatus());
            }
        };

        //setting the adapter on recycler view with credential
        parkedData.setHasFixedSize(true);
        parkedData.setLayoutManager(new LinearLayoutManager(context));
        parkedData.setAdapter(adapter);
    }

    //inner class for view holder
    private class ParkedHolder extends RecyclerView.ViewHolder{
        private TextView userName,totalPrice,totalHours,status;
        private CardView container;
        public ParkedHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.item_container);
            userName = itemView.findViewById(R.id.userName);
            totalPrice = itemView.findViewById(R.id.total_price);
            totalHours = itemView.findViewById(R.id.total_hours);
            status = itemView.findViewById(R.id.status);
        }
    }

    //overriding onStart methode to make adapter listen to changes
    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    //overriding onStop methode to make adapter listen to changes
    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    //methode to add the vehicle manualy
    public void addVehicle(View view1){

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
        message.setText("Are you sure you want manually Add a vehicle");

        //showing the alert dialoge
        buidler.setView(view);
        dialog = buidler.create();
        dialog.show();

        //setting click listner for yes button
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("ParkingPlaces").document(workId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    DocumentSnapshot placeDetDB = task.getResult();
                                    ParkingPlace BookPlace = helper.toParkingPlace(placeDetDB);
                                    int CurrentAvailSlots = BookPlace.getAvailableSlots();
                                    CurrentAvailSlots--;
                                    if(CurrentAvailSlots<=0){
                                        Toast.makeText(context, "can't add more vehicle place is full", Toast.LENGTH_SHORT).show();
                                    }else{
                                        Map<String,Object> placeDetMap = placeDetDB.getData();
                                        placeDetMap.put("availableSlots",CurrentAvailSlots);

                                        //changnig the slots infomation into database
                                        db.collection("ParkingPlaces").document(workId)
                                                .set(placeDetMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        localVehicle++;
                                                        SharedPreferences.Editor editor = placeData.edit();
                                                        editor.putInt("localVehicle",localVehicle);
                                                        editor.commit();
                                                        int TotalVehicleInside = adapter.getItemCount()+localVehicle;
                                                        parkedVehicle.setText("Total Vehicles: "+TotalVehicleInside);
                                                        Toast.makeText(context, "Vehicle Added", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            }
                        });
                dialog.dismiss();
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

    //methode to remove the vehicle manualy
    public void removeVehicle(View view1){

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
        message.setText("Are you sure you want manually Remove a vehicle");

        //showing the alert dialoge
        buidler.setView(view);
        dialog = buidler.create();
        dialog.show();

        //setting click listner for yes button
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("ParkingPlaces").document(workId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    DocumentSnapshot placeDetDB = task.getResult();
                                    ParkingPlace BookPlace = helper.toParkingPlace(placeDetDB);
                                    int CurrentAvailSlots = BookPlace.getAvailableSlots();
                                    int TotalSlots = BookPlace.getTotalSlots();
                                    CurrentAvailSlots++;
                                    if(CurrentAvailSlots>TotalSlots){
                                        Toast.makeText(context, "There are no vehicle to remove", Toast.LENGTH_SHORT).show();
                                    }else if(localVehicle==0){
                                        Toast.makeText(context, "You can't remove more vehicle", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        Map<String,Object> placeDetMap = placeDetDB.getData();
                                        placeDetMap.put("availableSlots",CurrentAvailSlots);

                                        //changnig the slots infomation into database
                                        db.collection("ParkingPlaces").document(workId)
                                                .set(placeDetMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        localVehicle--;
                                                        SharedPreferences.Editor editor = placeData.edit();
                                                        editor.putInt("localVehicle",localVehicle);
                                                        editor.commit();
                                                        int TotalVehicleInside = adapter.getItemCount()+localVehicle;
                                                        parkedVehicle.setText("Total Vehicles: "+TotalVehicleInside);
                                                        Toast.makeText(context, "Vehicle Removed", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                    }
                                }
                            }
                        });
                dialog.dismiss();
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


    //methode to logout the user
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