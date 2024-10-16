package com.tripbd.driver.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.tripbd.driver.R;
import com.tripbd.driver.model.Driver;
import com.tripbd.driver.model.Vehicle;
import com.tripbd.driver.utils.SystemUI;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProfileSetUpActivity extends AppCompatActivity {
    private static final String TAG = "ProfileSetUpActivity";
    private static final int PICK_IMAGE = 1;

    TextInputLayout text_input_layout_name, text_input_layout_email, text_input_layout_phone_number, text_input_layout_refer_code,
            text_input_layout_password, text_input_layout_confirm_pass;
    String name, email, phone_number, password, refer_code;
    Button btn_submit_profile;
    ImageView imgv_profile_image;
    ProgressBar progressDialog;
    MaterialToolbar profile_toolbar;
    StorageReference storageReference;
    String profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_set_up);

        profile_toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(profile_toolbar);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        imgv_profile_image = findViewById(R.id.imgv_pharmacy_image);
        text_input_layout_name = findViewById(R.id.text_input_layout_name);
        text_input_layout_email = findViewById(R.id.text_input_layout_email);
        text_input_layout_phone_number = findViewById(R.id.text_input_layout_phone_number);
        text_input_layout_password = findViewById(R.id.text_input_layout_password);
        text_input_layout_confirm_pass = findViewById(R.id.text_input_layout_confirm_pass);
        text_input_layout_refer_code = findViewById(R.id.text_input_layout_refer_code);

        btn_submit_profile = findViewById(R.id.btn_submit_profile);
        progressDialog = findViewById(R.id.progressbar_add_deliveryman);

        progressDialog.setVisibility(View.GONE);

        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        String email = getIntent().getStringExtra("email");
        String name = getIntent().getStringExtra("name");
        profileImage = getIntent().getStringExtra("imageUrl");

        if (phoneNumber != null) {
            text_input_layout_phone_number.getEditText().setText(phoneNumber);
            text_input_layout_phone_number.setFocusable(false);
            text_input_layout_phone_number.getEditText().setFocusable(false);
        }
        if (email != null) {
            text_input_layout_email.getEditText().setText(email);
            text_input_layout_email.setFocusable(false);
            text_input_layout_email.getEditText().setFocusable(false);
        }
        if (name != null) {
            text_input_layout_name.getEditText().setText(name);
        }
        if (profileImage != null) {
            text_input_layout_name.getEditText().setText(name);
            Picasso.get().load(profileImage).into(imgv_profile_image);
        }

        btn_submit_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitProfile(profileImage);
            }
        });


        imgv_profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.selectpicture)), PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        receiveImageFromDeviceRequest(requestCode, resultCode, data);
    }

    private void receiveImageFromDeviceRequest(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            //imgv_pharmacy_image.setImageURI(imageUri);

            try {
                // Defining the child of storageReference
                String imageRef = "images/" + UUID.randomUUID().toString();
                StorageReference ref = storageReference.child(imageRef);


                Bitmap bitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), imageUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


                int height = bitmap.getHeight();
                int width = bitmap.getWidth();


                //Compressed file size if greater than 1 MB;
                if (height >= 3000) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, width / 3, height / 3, true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

                } else if (height >= 2500) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int) (width / 2.5), (int) (height / 2.5), true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
                } else if (height >= 2000) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, (width / 2), (height / 2), true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                } else if (height >= 1500) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int) (width / 1.5), (int) (height / 1.5), true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                }

                byte[] bitmapdata = byteArrayOutputStream.toByteArray();
                imgv_profile_image.setImageBitmap(bitmap);
                ref.putBytes(bitmapdata)
                        .addOnSuccessListener(
                                new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Image uploaded successfully
                                        // Dismiss dialog
                                        ref.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Uri> task) {
                                                Log.d(TAG, "onComplete: Image Uploaded");
                                                Toast.makeText(ProfileSetUpActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();

                                                btn_submit_profile.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        submitProfile(task.getResult().toString());
                                                    }
                                                });

                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Error, Image not uploaded
                                progressDialog.setVisibility(View.GONE);
                                Toast.makeText(ProfileSetUpActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            // Progress Listener for loading
                            // percentage on the dialog box
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()
                                        / taskSnapshot.getTotalByteCount());
                                //progress = (startIndex*(100.0/(uris.size()+1))) + (progress/(uris.size()+1));
                                // progressDialog.setMessage("Uploaded " + (int)progress + "%");
                            }
                        });


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void submitProfile(String profileImage) {
        Log.d(TAG, "submitProfile: called");

        progressDialog.setVisibility(View.VISIBLE);
        name = text_input_layout_name.getEditText().getText().toString();
        email = text_input_layout_email.getEditText().getText().toString();
        phone_number = text_input_layout_phone_number.getEditText().getText().toString();
        password = text_input_layout_password.getEditText().getText().toString();
        String confirm_pass = text_input_layout_confirm_pass.getEditText().getText().toString();
        refer_code = text_input_layout_refer_code.getEditText().getText().toString();

        if (name.isEmpty()) {
            text_input_layout_name.setError("Name required!");
            text_input_layout_name.requestFocus();
        } else if (password.isEmpty()) {
            text_input_layout_password.setError("Password required!");
            text_input_layout_password.requestFocus();
        } else if (confirm_pass.isEmpty() || !confirm_pass.equals(password)) {
            text_input_layout_confirm_pass.setError("Password can't match!");
            text_input_layout_confirm_pass.requestFocus();
        } else if (profileImage.isEmpty()) {
            Toast.makeText(this, "Select a Profile image!", Toast.LENGTH_SHORT).show();
        } else {
            if (email.isEmpty()) {
                email = " ";
            }
            if(!phone_number.contains("88")) phone_number = "88"+phone_number;

            //Check Phone Number Is Used or not start........
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("driver").addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    assert value != null;
                    int user = 0;
                    for (QueryDocumentSnapshot doc : Objects.<QuerySnapshot>requireNonNull(value)) {
                        if (doc.get("name") != null) {
                            Driver driver = doc.toObject(Driver.class);
                            if (driver.getPhoneNumber().equalsIgnoreCase(phone_number)) {
                                user = 1;
                            }
                        }
                    }
                    if (user == 1) {
                        progressDialog.setVisibility(View.GONE);
                        Toast.makeText(ProfileSetUpActivity.this, "This number is already used in other account, Try another...", Toast.LENGTH_SHORT).show();
                    } else {
                        List<Vehicle> vehicles = new ArrayList<>();
                        Driver driver = new Driver(null, name, phone_number, password, email, profileImage,
                                "", 0, 0, 0, vehicles, refer_code, "", "",  "");

                         addDriver(driver);
                    }
                }
            });
            //Check Phone Number Is Used or not end........

        }
    }



        private void addDriver (Driver driver){
            progressDialog.setVisibility(View.VISIBLE);


            //todo...submit budget data to server...
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            //progressDialog.setMessage("Uploaded " + 90 + "%");
            db.collection("driver").add(driver).addOnSuccessListener(documentReference -> {
                Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                SharedPreferences sharedPreferences = getSharedPreferences("AUTHENTICATION", Context.MODE_PRIVATE);
                sharedPreferences.edit().putString("DRIVER_PHONE_NUMBER", phone_number).apply();
                sharedPreferences.edit().putString("DRIVER_PHONE_EMAIL", email).apply();
                sharedPreferences.edit().putString("DRIVER_NAME", name).apply();
                sharedPreferences.edit().putString("DRIVER_ID", documentReference.getId()).apply();
                sharedPreferences.edit().putString("DRIVER_PASSWORD", password).apply();
                DialogWelcome dialogWelcome = new DialogWelcome(ProfileSetUpActivity.this,
                        "Welcome to " + getResources().getString(R.string.app_name) + "\n  Always stay with us!",
                        new Intent(ProfileSetUpActivity.this, AddVehicleActivity.class));
                dialogWelcome.setCancelable(false);
                dialogWelcome.show();

            }).addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));

            //progressDialog.dismiss();
            progressDialog.setVisibility(View.GONE);
        }


        @Override
        public void onWindowFocusChanged ( boolean hasFocus){
            super.onWindowFocusChanged(hasFocus);
            if (hasFocus) {
                new SystemUI(ProfileSetUpActivity.this).hideSystemUI();

            }
        }

}



