package com.vehicle.driver.view;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vehicle.driver.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.vehicle.driver.model.Driver;
import com.vehicle.driver.utils.SystemUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    TextInputLayout text_input_layout_country_code, textInputLayout_phone_number, textInputLayout_password;
    MaterialButton btn_sign_in, btn_goto_sign_up;
    MaterialToolbar signin_toolbar;
    ProgressBar progressbar;
    MyPushNotificationHandler myPushNotificationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);






        progressbar = findViewById(R.id.progressbar);

        signin_toolbar = findViewById(R.id.signin_toolbar);
        signin_toolbar = findViewById(R.id.signin_toolbar);
        setSupportActionBar(signin_toolbar);
        setTitle("Sign In");

        text_input_layout_country_code = findViewById(R.id.text_input_layout_country_code);
        textInputLayout_phone_number = findViewById(R.id.text_input_layout_phone_number);
        textInputLayout_password = findViewById(R.id.textInputLayout_password);
        btn_sign_in = findViewById(R.id.btn_sign_in);
        btn_goto_sign_up = findViewById(R.id.btn_goto_sign_up);

        String phoneNumber;
        btn_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String countryCode = text_input_layout_country_code.getEditText().getText().toString();
                if (countryCode.charAt(0)=='+') countryCode = countryCode.substring(1);
                String phoneNumber = textInputLayout_phone_number.getEditText().getText().toString();
                String password = textInputLayout_password.getEditText().getText().toString();

                if (countryCode.isEmpty()) {
                    text_input_layout_country_code.setError("Required.");
                    text_input_layout_country_code.requestFocus();
                    return;
                }else if (phoneNumber.isEmpty()) {
                    textInputLayout_phone_number.setError("Required.");
                    textInputLayout_phone_number.requestFocus();
                    return;
                }
                if (password.isEmpty()) {
                    textInputLayout_password.setError("Required.");
                    textInputLayout_password.requestFocus();
                    return;
                }

                //todo...Log in...

                phoneNumber = countryCode + phoneNumber;
                progressbar .setVisibility(View.VISIBLE);
                Log.d(TAG, "onResponse: phone_number: "+phoneNumber);
                Log.d(TAG, "onClick: password: "+password);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("driver").addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen failed: "+e.getMessage());
                            progressbar.setVisibility(View.GONE);
                            return;
                        }

                        String countryCode = text_input_layout_country_code.getEditText().getText().toString();
                        if (countryCode.charAt(0)=='+') countryCode = countryCode.substring(1);
                        String phoneNumber = textInputLayout_phone_number.getEditText().getText().toString();
                        String password = textInputLayout_password.getEditText().getText().toString();
                        phoneNumber = countryCode + phoneNumber;

                        //assert value != null;
                        if (value == null || value.getDocuments().size()<1) {
                            progressbar.setVisibility(View.GONE);
                            Toast.makeText(SignInActivity.this, "Registration First...", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
                        }
                        for (QueryDocumentSnapshot doc : Objects.<QuerySnapshot>requireNonNull(value)) {
                            Driver driver = doc.toObject(Driver.class);
                            if (driver.getName() != null) {
                                progressbar.setVisibility(View.GONE);
                                driver.setId(doc.getId());
                                if (driver.getPhoneNumber().equalsIgnoreCase(phoneNumber) &&
                                        driver.getPassword().equalsIgnoreCase(password)) {
                                    SharedPreferences sharedPreferences = getSharedPreferences("AUTHENTICATION", Context.MODE_PRIVATE);
                                    sharedPreferences.edit().putString("DRIVER_PHONE_NUMBER", phoneNumber).apply();
                                    sharedPreferences.edit().putString("DRIVER_PHONE_EMAIL", driver.getEmail()).apply();
                                    sharedPreferences.edit().putString("DRIVER_NAME", driver.getName()).apply();
                                    sharedPreferences.edit().putString("DRIVER_ID", driver.getId()).apply();
                                    sharedPreferences.edit().putString("DRIVER_PASSWORD", password).apply();
                                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(SignInActivity.this, "Does not match Phone Number or Password", Toast.LENGTH_SHORT).show();
                                    progressbar.setVisibility(View.GONE);
                                }
                            } else {
                                Toast.makeText(SignInActivity.this, "Something went wrong, try again!", Toast.LENGTH_SHORT).show();
                                progressbar.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        });

        btn_goto_sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            }
        });

        myPushNotificationHandler = new MyPushNotificationHandler(SignInActivity.this);
        myPushNotificationHandler.createChannel("ChannelId1","ChannelName");
        myPushNotificationHandler.subscribeTopic();
        askNotificationPermission();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted",Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, "FCM can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });


    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.

            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }





    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = getSharedPreferences("AUTHENTICATION", Context.MODE_PRIVATE);
        if(!sharedPreferences.getString("DRIVER_PHONE_NUMBER", "null").equalsIgnoreCase("null") &&
                sharedPreferences.getString("DRIVER_PASSWORD", "null") != null) {
            String driverID = sharedPreferences.getString("DRIVER_ID", "null");
            // after getting and updating fcm token to the database, it will open main activity itself
                myPushNotificationHandler.getAndSaveToken(driverID);

            //Toast.makeText(this, "User Sign in", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "user not sign in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            new SystemUI(SignInActivity.this).hideSystemUI();
        }
    }
}