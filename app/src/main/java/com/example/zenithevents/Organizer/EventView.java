package com.example.zenithevents.Organizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.zenithevents.HelperClasses.UserUtils;
import com.example.zenithevents.MainActivity;
import com.example.zenithevents.Objects.Event;
import com.example.zenithevents.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class EventView extends AppCompatActivity {

    private static final String TAG = "EventView";


    ImageView eventPosterImageView, eventQRImageView;
    private Button btnJoinWaitingList, btnLeaveWaitingList;
    private TextView QRCodeRequiredText, eventDescription, eventName, facilityName, eventAddress;
    private ProgressBar progressBar;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String eventId = getIntent().getStringExtra("event_id");
        initializeViews();
        setupRealTimeEventListener(eventId);

    }


    private void initializeViews() {
        eventPosterImageView = findViewById(R.id.eventImage);
        btnJoinWaitingList = findViewById(R.id.btnJoinWaitingList);
        facilityName = findViewById(R.id.facilityName);
        eventAddress = findViewById(R.id.eventAddress);
        progressBar = findViewById(R.id.progressBar);
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        eventQRImageView = findViewById(R.id.eventQR);
    }

    private void setupRealTimeEventListener(String eventId) {
        if (eventId == null) {
            Log.e(TAG, "Event ID not provided in the Intent.");
            return;
        }

        // Show progress bar while loading
        progressBar.setVisibility(ProgressBar.VISIBLE);

        // Set up the real-time listener for the event document
        eventListener = db.collection("events").document(eventId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        progressBar.setVisibility(ProgressBar.GONE);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Convert document snapshot to Event object
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            displayEventDetails(event);
                        }
                    } else {
                        Log.e(TAG, "Event document does not exist.");
                    }

                    // Hide progress bar after loading
                    progressBar.setVisibility(ProgressBar.GONE);
                });
    }

    private void displayEventDetails(Event event) {
        // Set event details
        eventName.setText(event.getEventTitle());
        facilityName.setText(event.getOwnerFacility());
        eventAddress.setText(event.getEventAddress());
        eventDescription.setText(event.getEventDescription());

        btnJoinWaitingList.setOnClickListener(v -> {
            Context context = this;

            UserUtils userUtils = new UserUtils();
            userUtils.applyEvent(context, event.getEventId(), isSuccess -> {
                if (isSuccess) {
                    Toast.makeText(context, "Successfully joined the event!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Load event image using Glide
        loadImage(event, event.getImageUrl(), eventPosterImageView);
//        loadImage(event, event.getQRCodeUrl(), eventQRImageView);
    }

    private void loadImage(Event event, String imageUrl, ImageView placeholder) {
        if (imageUrl != null) {
            Bitmap imgBitMap = event.decodeBase64ToBitmap(imageUrl);
            Glide.with(this).load(imgBitMap).into(placeholder);
        } else {
            placeholder.setImageResource(R.drawable.event_place_holder);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) {
            eventListener.remove(); // Remove the listener to avoid memory leaks
        }
    }
}