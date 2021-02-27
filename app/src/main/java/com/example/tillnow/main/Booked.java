package com.example.tillnow.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tillnow.Helper;
import com.example.tillnow.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class Booked extends AppCompatActivity {

    //local
    private String userId;

    //context
    private Context context=this;

    //TextView
    TextView defaultMessage;

    //recyler view
    private RecyclerView BookedHistory;

    //shared profrences
    SharedPreferences userData;

    //classes
    private Helper helper = new Helper();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;
    private FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booked);

        //hooks
        BookedHistory = findViewById(R.id.booked_history);
        defaultMessage = findViewById(R.id.default_message);

        //getting user data from shared prefrence
        userData = getSharedPreferences("UserData",MODE_PRIVATE);
        userId = userData.getString("uid",null);

        //checking from database if user has any booked slot history or not
        db.collection("Users").document(userId).collection("SlotsHistory")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            QuerySnapshot slotHistory = task.getResult();
                            //cheaking if user has any booked slot
                            if(slotHistory.isEmpty()){
                                //user dont have any booked slots, so default message remains
                            }else{
                                //user has booked slots. so, showing the booked history
                                defaultMessage.setVisibility(View.GONE);
                                BookedHistory.setVisibility(View.VISIBLE);
                            }
                        }else{
                            //problem
                        }
                    }
                });


        //creating query to get user's history
        Query query = db.collection("Users").document(userId).collection("SlotsHistory").orderBy("BookedId", Query.Direction.DESCENDING);

        //creating to Recycler option to get data in objects formet
        FirestoreRecyclerOptions<BookedHistoryModel> options = new FirestoreRecyclerOptions.Builder<BookedHistoryModel>()
                .setQuery(query,BookedHistoryModel.class)
                .build();

        //creating adapter for recycler view to show data in user readable formet
        adapter = new FirestoreRecyclerAdapter<BookedHistoryModel, HistoryHolder>(options) {
            @NonNull
            @Override
            public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.booked_item,parent,false);
                return new HistoryHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull HistoryHolder holder, int position, @NonNull BookedHistoryModel model) {
                //getting the status
                String status = model.getStatus();
                //checking if the slot is booked or not
                if(status.equals("booked")){
                    holder.status.setTextColor(Color.rgb(0,255,0));
                    holder.delete.setVisibility(View.VISIBLE);
                }else{
                    holder.status.setTextColor(Color.rgb(255,255,255));
                    holder.delete.setVisibility(View.GONE);
                }
                //setting all the holder's data
                holder.bookId= model.getBookedId();
                holder.placeId =model.getPlaceId();
                holder.placeName.setText(model.getPlaceName());
                holder.totalPrice.setText("Total price: "+model.getTotalPrice());
                holder.totalHours.setText("Total Hours: "+model.getTotalHours());
                holder.status.setText("Status: "+model.getStatus());
            }
        };

        //setting the adapter on recycler view with credential
        BookedHistory.setHasFixedSize(true);
        BookedHistory.setLayoutManager(new LinearLayoutManager(context));
        BookedHistory.setAdapter(adapter);

        //setting up bottom menu
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListner);
        Menu menu =bottomNav.getMenu();
        MenuItem menuItem = menu.getItem(3);
        menuItem.setChecked(true);
    }

    //inner class for view holder
    private class HistoryHolder extends RecyclerView.ViewHolder{

        private TextView placeName,totalPrice,totalHours,status;
        private CardView container;
        private Button delete;
        private int bookId;
        private String placeId;
        public HistoryHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.item_container);
            placeName = itemView.findViewById(R.id.place_name);
            totalPrice = itemView.findViewById(R.id.total_price);
            totalHours = itemView.findViewById(R.id.total_hours);
            status = itemView.findViewById(R.id.status);
            delete = itemView.findViewById(R.id.deleteButton);

            //setting click lisner for cancle the booked slot
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //getting the alert dialog ready
                    AlertDialog.Builder buidler;
                    final AlertDialog dialog;
                    LayoutInflater inflater;

                    //building the alert dialog for conformation of cancling the booking
                    buidler = new AlertDialog.Builder(context);
                    inflater = LayoutInflater.from(context);
                    View view = inflater.inflate(R.layout.confirmation_pop, null);

                    //hooks for yes and no button
                    Button noButton = view.findViewById(R.id.conf_no_button);
                    Button yesButton = view.findViewById(R.id.conf_yes_button);
                    TextView message = view.findViewById(R.id.text_alert);
                    message.setText("Are you sure you want to Cancle this booked slot (you wont get any refund)");

                    //showing the alert dialoge
                    buidler.setView(view);
                    dialog = buidler.create();
                    dialog.show();

                    //setting click listner for yes button
                    yesButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            //changing the status of slot from user's history
                            db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(bookId))
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                Map<String,Object> BookHistory= task.getResult().getData();
                                                BookHistory.put("Status","cancled");

                                                //adding the new status of history into user's database
                                                db.collection("Users").document(userId).collection("SlotsHistory").document(String.valueOf(bookId))
                                                        .set(BookHistory)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                //user's history updated, updating shared prefrence now
                                                                SharedPreferences.Editor editor = userData.edit();
                                                                editor.putString("Book status","cancled");
                                                                editor.putInt("bookId",bookId);
                                                                editor.commit();
                                                            }
                                                        });
                                            }else{
                                                Log.d("myTag", "onComplete: task failed");
                                            }
                                        }
                                    });

                            //removing the booked slot from parking place's database
                            db.collection("ParkingPlaces").document(placeId).collection("Slots").document(userId)
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //updating the available slots after removing the slot's data
                                            db.collection("ParkingPlaces").document(placeId)
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
                                                                db.collection("ParkingPlaces").document(placeId)
                                                                        .set(placeDetMap)
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {
                                                                                Toast.makeText(context, "Slot Cancled", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });

                                                            }
                                                        }
                                                    });
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
            });

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
