package com.tripbd.driver.view;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tripbd.driver.R;
import com.tripbd.driver.model.Driver;
import com.tripbd.driver.model.Trip;
import com.tripbd.driver.model.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BiddingDialog extends Dialog {
    private static final String TAG = "BiddingDialog";
    TextInputLayout text_input_layout_bid_price, text_input_layout_advance, text_input_layout_description;
    RadioGroup radio_groupPayment;
    Spinner spinnerSelectVehicle;
    Vehicle vehicle;
    int bidPrice=0, advance=0;
    String description, paymentMethod;
    MaterialButton btnSubmitBid;
    String vehicleType, vehicleVariety;
    String documentId;
    Activity activity;
    TextView txtvOwnersAmount,txtvServiceCharge;

    public BiddingDialog(@NonNull Activity context, String vehicleType, String vehicleVariety, String documentId) {
        super(context);
        this.activity = context;
        this.vehicleType = vehicleType;
        this.vehicleVariety = vehicleVariety;
        this.documentId = documentId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_place_bid);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        text_input_layout_bid_price = findViewById(R.id.text_input_layout_bid_price);
        text_input_layout_advance = findViewById(R.id.text_input_layout_advance);
        text_input_layout_description = findViewById(R.id.text_input_layout_description);

        spinnerSelectVehicle = findViewById(R.id.spinnerSelectVehicle);
        radio_groupPayment = findViewById(R.id.radio_groupPayment);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);
        txtvOwnersAmount = findViewById(R.id.txtvOwnersAmount);
        txtvServiceCharge = findViewById(R.id.txtvServiceCharge);
        text_input_layout_bid_price.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (text_input_layout_bid_price.getEditText().getText()==null ||
                        text_input_layout_bid_price.getEditText().getText().length()<1) return;

                double bidPrice= Integer.parseInt(text_input_layout_bid_price.getEditText().getText().toString());
                int ownersAmount = (int) ((bidPrice / 100)*97);
                int serviceCharge = (int) ((bidPrice/100)*3);
                //Toast.makeText(activity, "price: "+serviceCharge, Toast.LENGTH_SHORT).show();

                txtvOwnersAmount.setText(getContext().getString(R.string.owner_owners_amount)+ownersAmount+" tk (97%)");
                txtvServiceCharge.setText(getContext().getString(R.string.service_charge)+serviceCharge+" tk (3%)");

            }
        });

        getDriver();
    }

    private void submitBid(Driver driver, Vehicle selectedVehicle) {
        bidPrice = Integer.parseInt(text_input_layout_bid_price.getEditText().getText().toString());
        advance = Integer.parseInt(text_input_layout_advance.getEditText().getText().toString());
        description = text_input_layout_description.getEditText().getText().toString();
        paymentMethod = radio_groupPayment.getCheckedRadioButtonId()==R.id.hand_cash? "HandCash" : "bKash";
        if (advance> (bidPrice/2)){
            text_input_layout_advance.getEditText().setError("Amount should not be more than 50% of Bid Price");
            text_input_layout_advance.getEditText().requestFocus();
            return;
        }

        Trip.Bid bid = new Trip.Bid();
        bid.setBidPrice(bidPrice);
        bid.setAdvance(advance);
        bid.setDescription(description);
        bid.setReqPayMethod(paymentMethod);
        bid.setDriver(driver);
        bid.setVehicle(selectedVehicle);
        Toast.makeText(activity, bid.getVehicle().getType(), Toast.LENGTH_SHORT).show();

        //Toast.makeText(getContext(), "Everything is okay!", Toast.LENGTH_SHORT).show();
        // Todo...

        //Update Bidding
        //FirebaseFirestore db = FirebaseFirestore.getInstance();
        //progressDialog.setMessage("Uploaded " + 90 + "%");
     /*   db.collection("driver").document(driver.getId()).set(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressbar_add_vehicle.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Vehicle Added!", Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(getContext(), MainActivity.class));
                cancel();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                progressbar_add_vehicle.setVisibility(View.GONE);

            }
        });*/

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference tripRef = db.collection("trip").document(documentId);
        tripRef.update("bids", FieldValue.arrayUnion(bid)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //Toast.makeText(getContext(), "Bid successfully placed!", Toast.LENGTH_SHORT).show();
                    String welcomeText = getContext().getString(R.string.thank_you_for_participating_in_the_bid_If_you_win_the_bid_it_would_be_added_to_your_running_trip_);
                    DialogWelcome dialogWelcome = new DialogWelcome(activity, welcomeText,new Intent(getContext(), MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    dialogWelcome.setCancelable(false);
                    dialogWelcome.show();
                }else{
                    Toast.makeText(getContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }
    String getVehicleDetails(Vehicle vehicle){
        if (vehicle==null) return "...";
        else if (
                vehicle.getType().equalsIgnoreCase("Truck") ||
                vehicle.getType().equalsIgnoreCase("Pickup") ||
                vehicle.getType().equalsIgnoreCase("Trailer") ){

            return vehicle.getType()+", "
                    +vehicle.getSize()+" ("
                    +vehicle.getVariety()+")";
        }else {
            return vehicle.getType()+", "
                    +vehicle.getSeat()+" Seats "+" ("
                    +vehicle.getVariety()+")";
        }
    }

    public void getDriver(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sp = getContext().getSharedPreferences("AUTHENTICATION", Context.MODE_PRIVATE);
        String id = sp.getString("DRIVER_ID",null);
        String notification = sp.getString("NOTIFICATION","None");

        if (id==null) {
            Toast.makeText(getContext(), "You are not sign in.", Toast.LENGTH_SHORT).show();
            // getContext().startActivity(new Intent(getContext(), SignInActivity.class));
            // getContext().getActivity().finish();

        }else{
            db.collection("driver").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Driver driver = task.getResult().toObject(Driver.class);
                    driver.setId(task.getResult().getId());
                    if (driver==null){
                        Toast.makeText(getContext(), "Driver Not Found! "+id, Toast.LENGTH_SHORT).show();
                    }else{
                        //todo...
                        if (driver.getVehicles()==null||driver.getVehicles().size()<1){
                            Toast.makeText(getContext(), "Add at least 1 Vehicle to apply for bidding!", Toast.LENGTH_LONG).show();
                            cancel();
                            return;
                        }
                        int avaiableVehicleCount=0;
                        for (int i=0; i<driver.getVehicles().size(); i++){
                            Log.d(TAG, "onComplete: vehicleType: "+driver.getVehicles().get(i).getType()+" "+vehicleType);
                            Log.d(TAG, "onComplete: vehicleVariety: "+driver.getVehicles().get(i).getVariety()+" "+vehicleVariety);
                            if(driver.getVehicles().get(i).getType().equals(vehicleType) &&
                                    (driver.getVehicles().get(i).getVariety().equals(vehicleVariety) ||
                                            driver.getVehicles().get(i).getVariety().equals("")))
                                avaiableVehicleCount++;
                        }
                        if (avaiableVehicleCount<1){
                            //Toast.makeText(getContext(), vehicleType+","+vehicleVariety, Toast.LENGTH_LONG).show();
                            Toast.makeText(getContext(), "You need "+vehicleType+","+vehicleVariety+" vehicle to bid.", Toast.LENGTH_LONG).show();
                            cancel();
                            return;
                        }
                        String[] vehiclesDetails = new String[avaiableVehicleCount];
                        List<Vehicle> filteredVehicles = new ArrayList<>();
                        int availableVehicle = 0;
                        for (int i=0; i<driver.getVehicles().size(); i++){
                            if(driver.getVehicles().get(i).getType().equals(vehicleType) &&
                                    (driver.getVehicles().get(i).getVariety().equals(vehicleVariety) ||
                                            driver.getVehicles().get(i).getVariety().equals(""))) {
                                filteredVehicles.add(driver.getVehicles().get(i));
                                vehiclesDetails[availableVehicle] = getVehicleDetails(driver.getVehicles().get(i));
                                availableVehicle++;
                            }
                        }
                        ArrayAdapter aa = new ArrayAdapter(getContext(),R.layout.simple_list_item, vehiclesDetails);
                        //Setting the ArrayAdapter data on the Spinner
                        spinnerSelectVehicle.setAdapter(aa);

                        btnSubmitBid.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                 submitBid(driver, filteredVehicles.get(spinnerSelectVehicle.getSelectedItemPosition()));
                            }
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }
    }
}
