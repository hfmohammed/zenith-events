package com.example.zenithevents.Events;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.zenithevents.Admin.QRViewAdmin;
import com.example.zenithevents.EntrantsList.CancelledEntrants;
import com.example.zenithevents.EntrantsList.EnrolledEntrants;
import com.example.zenithevents.EntrantsList.MapActivity;
import com.example.zenithevents.EntrantsList.SampledEntrants;
import com.example.zenithevents.EntrantsList.WaitlistedEntrants;
import com.example.zenithevents.Events.CreateEventPage;
import com.example.zenithevents.Events.QRView;
import com.example.zenithevents.HelperClasses.BitmapUtils;
import com.example.zenithevents.HelperClasses.DeviceUtils;
import com.example.zenithevents.HelperClasses.EventUtils;
import com.example.zenithevents.HelperClasses.FacilityUtils;
import com.example.zenithevents.HelperClasses.UserUtils;
import com.example.zenithevents.Objects.Event;
import com.example.zenithevents.Objects.User;
import com.example.zenithevents.R;
import com.google.firebase.firestore.DocumentReference;
import com.example.zenithevents.User.OrganizerPage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.example.zenithevents.User.ProfileDetailActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class represents the EventView activity in the application, which displays
 * the details of an event, allowing users to join/leave the waiting list, view the
 * event's description and image, and perform various event-related actions.
 *
 * <p>Note: The Javadocs for this class were generated with the assistance of an AI language model.
 */
public class EventView extends AppCompatActivity {

    private static final String TAG = "EventView";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    ImageView eventPosterImageView;
    private Button btnJoinLeaveWaitingList, qrCodeButton, btnEditEvent, btnSampleUsers, deleteEventButton, deleteImageButton, mapButton;
    private TextView eventDescription, eventName, facilityName, eventAddress;
    private ProgressBar progressBar;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration eventListener;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final String[] entrantOptions = {"Waitlisted Entrants", "Selected Entrants", "Registered Entrants", "Cancelled Entrants"};
    private int currentOptionIndex = 0;
    LinearLayout entrantsSlider;
    FacilityUtils facilityUtils = new FacilityUtils();
    EventUtils eventUtils = new EventUtils();
    private String eventId, type;

    /**
     * Initializes the activity and its UI components.
     *
     * @param savedInstanceState Contains data supplied in onSaveInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventId = getIntent().getStringExtra("event_id");
        type = getIntent().getStringExtra("type");
        initializeViews();

        setupRealTimeEventListener(eventId);
    }

    /**
     * Initializes views used in the activity.
     */
    private void initializeViews() {
        eventPosterImageView = findViewById(R.id.eventImage);
        btnJoinLeaveWaitingList = findViewById(R.id.btnJoinWaitingList);
        facilityName = findViewById(R.id.facilityName);
        eventAddress = findViewById(R.id.eventAddress);
        progressBar = findViewById(R.id.progressBar);
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        qrCodeButton = findViewById(R.id.qrCodeButton);
        entrantsSlider = findViewById(R.id.entrantsSlider);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        deleteEventButton = findViewById(R.id.deleteEvent);
        btnSampleUsers = findViewById(R.id.btnSampleUsers);
        deleteImageButton = findViewById(R.id.deleteImage);
        mapButton = findViewById(R.id.mapButton);
    }

    /**
     * Sets up a real-time listener for the event document in Firestore.
     *
     * @param eventId The unique identifier of the event.
     */
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
                        this.eventId = event != null ? event.getEventId() : null;

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

    /**
     * Displays details of the event on the UI.
     *
     * @param event The event object containing event details.
     */
    private void displayEventDetails(Event event) {
        Log.d("FunctionCall", "displayingDetails");

        // Set event details
        eventName.setText(event.getEventTitle());
        facilityUtils.fetchFacilityName(event.getOwnerFacility(), v -> {
            if (v != null) {
                facilityName.setText(v);
            } else {
                facilityName.setText("");
            }
        });

        eventAddress.setText(event.getEventAddress());
        eventDescription.setText(event.getEventDescription());
        UserUtils userUtils = new UserUtils();
        String deviceID = DeviceUtils.getDeviceID(this);

        Log.d("FunctionCall", "deviceID: " + deviceID);

        if (event.getWaitingList().contains(deviceID) ||
                event.getSelected().contains(deviceID) ||
                event.getCancelledList().contains(deviceID) ||
                event.getRegistrants().contains(deviceID)
        ) {
            btnJoinLeaveWaitingList.setBackgroundColor(Color.RED);
            btnJoinLeaveWaitingList.setText("Leave Event");

            btnJoinLeaveWaitingList.setOnClickListener(v -> {
                Context context = EventView.this;

                userUtils.applyLeaveEvent(context, deviceID, event.getEventId(), (isSuccess, event_) -> {
                    if (isSuccess == 1) {
                        Toast.makeText(context, "Successfully joined the event!", Toast.LENGTH_SHORT).show();
                    } else if (isSuccess == 0) {
                        Toast.makeText(context, "Successfully left the event!", Toast.LENGTH_SHORT).show();
                    } else if (isSuccess == -1) {
                        Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            btnJoinLeaveWaitingList.setBackgroundColor(Color.BLUE);
            btnJoinLeaveWaitingList.setText("Join Waiting List");

            btnJoinLeaveWaitingList.setOnClickListener(v -> {
                Context context = EventView.this;
                userUtils.applyLeaveEvent(context, deviceID, event.getEventId(), (isSuccess, event_) -> {
                    Log.d("FunctionCall", "Applying.." + isSuccess);
                    if (isSuccess == 1) {
                        if (event_.getHasGeolocation()) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(EventView.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }
                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
                            fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener(location -> {
                                        if (location != null) {
                                            double latitude = location.getLatitude();
                                            Log.d("FunctionCall", "EVENTID" + event_.getEventId());

                                            Log.d("FunctionCall", "Latitude" + latitude);
                                            double longitude = location.getLongitude();
                                            Log.d("FunctionCall", "Longitude:" + longitude);
                                            event_.updateUserLocation(deviceID, latitude, longitude);

                                            Log.d("FunctionCall", "updatingEvent...");
                                            eventUtils.createUpdateEvent(event_, callback -> {
                                                if (callback != null) {
                                                    Log.d("FunctionCall", "Location added successfully.");
                                                } else {
                                                    Log.d("FunctionCall", "Failed to update event.");
                                                }
                                            });
                                        } else {
                                            Log.d("Location", "Location is null");
                                        }
                                    });
                        }
                        Toast.makeText(context, "Successfully joined the event!", Toast.LENGTH_SHORT).show();
                    } else if (isSuccess == 0) {
                        Toast.makeText(context, "Successfully left the event!", Toast.LENGTH_SHORT).show();
                    } else if (isSuccess == -1) {
                        Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            if (event.getNumParticipants() != 0 &&
                    event.getCancelledList().size() + event.getSelected().size() + event.getRegistrants().size() + event.getWaitingList().size() >= event.getNumParticipants()) {
                btnJoinLeaveWaitingList.setEnabled(false);
                btnJoinLeaveWaitingList.setText("Event is full");
                btnJoinLeaveWaitingList.setBackgroundColor(Color.GRAY);
            }
        }

        // Load event image using Glide
        loadImage(event.getImageUrl(), eventPosterImageView);
        Log.d("FunctionCAll", "type:: " + type);
        if (Objects.equals(type, "organizer") || Objects.equals(type, "admin")) {
            setupEntrantNavigation(event.getEventId());
            if (Objects.equals(type, "admin")) {
                deleteImageButton.setOnClickListener(v ->
                        new AlertDialog.Builder(EventView.this)
                                .setTitle("Remove Event Picture")
                                .setMessage("Are you sure you want to remove the event's poster picture?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    removePicture();
                                })
                                .setNegativeButton("No", null)
                                .show()
                );
            }

            mapButton.setVisibility(View.VISIBLE);
            mapButton.setOnClickListener(v -> {
                Intent intent = new Intent(EventView.this, MapActivity.class);
                intent.putExtra("EventID", event.getEventId());
                startActivity(intent);
            });

            btnEditEvent.setVisibility(View.VISIBLE);
            btnEditEvent.setOnClickListener(v -> {
                Intent intent = new Intent(EventView.this, CreateEventPage.class);
                intent.putExtra("page_title", "Edit Event");
                intent.putExtra("Event", (Serializable) event);
                startActivity(intent);
            });

            btnSampleUsers.setVisibility(View.VISIBLE);
            btnSampleUsers.setOnClickListener(v -> {
                event.drawLottery();
                Intent intent = new Intent(this, SampledEntrants.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            });

            deleteEventButton.setVisibility(View.VISIBLE);
            deleteEventButton.setOnClickListener(v-> {
                progressBar.setVisibility(View.VISIBLE);
                eventUtils.removeEvent(eventId, success-> {
                    progressBar.setVisibility(View.GONE);
                    if (success) {
                        Toast.makeText(EventView.this, "Event deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EventView.this, "Event did not delete", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            btnEditEvent.setVisibility(View.GONE);
            entrantsSlider.setVisibility(View.GONE);
            deleteEventButton.setVisibility(View.GONE);
            deleteImageButton.setVisibility(View.GONE);
        }

        qrCodeButton.setOnClickListener(v -> {
            DocumentReference userRef = db.collection("users").document(deviceID);
            userRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                            if (Boolean.TRUE.equals(isAdmin) && Objects.equals(type, "admin")) {
                                Intent intent = new Intent(EventView.this, QRViewAdmin.class);
                                intent.putExtra("Event", (Serializable) event);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(EventView.this, QRView.class);
                                intent.putExtra("Event", (Serializable) event);
                                startActivity(intent);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Can't find document", e));
        });
    }

    /**
     * Loads an image from a URL into an ImageView.
     *
     * @param imageUrl   The URL of the image.
     * @param placeholder The ImageView to display the image in.
     */
    private void loadImage(String imageUrl, ImageView placeholder) {
        BitmapUtils bitmapUtils = new BitmapUtils();
        if (imageUrl != null) {
            Bitmap imgBitMap = bitmapUtils.decodeBase64ToBitmap(imageUrl);
            Glide.with(this).load(imgBitMap).into(placeholder);
            deleteImageButton.setVisibility(View.VISIBLE);
        } else {
            placeholder.setImageResource(R.drawable.event_place_holder);
            deleteImageButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up navigation between different entrant lists.
     *
     * @param eventId The unique identifier of the event.
     */
    private void setupEntrantNavigation(String eventId) {
        TextView currentOptionText = findViewById(R.id.currentOptionText);
        TextView leftArrowButton = findViewById(R.id.leftArrowButton);
        TextView rightArrowButton = findViewById(R.id.rightArrowButton);

        updateDisplay(currentOptionText, eventId);

        leftArrowButton.setOnClickListener(v -> {
            currentOptionIndex = (currentOptionIndex - 1 + entrantOptions.length) % entrantOptions.length;
            updateDisplay(currentOptionText, eventId);
        });


        rightArrowButton.setOnClickListener(v -> {
            currentOptionIndex = (currentOptionIndex + 1) % entrantOptions.length;
            updateDisplay(currentOptionText, eventId);
        });
    }

    /**
     * Updates the displayed entrant list based on the current option index.
     *
     * @param currentOptionText The TextView showing the current option.
     * @param eventId           The unique identifier of the event.
     */
    private void updateDisplay(TextView currentOptionText, String eventId) {
        currentOptionText.setText(entrantOptions[currentOptionIndex]);

        currentOptionText.setOnClickListener(v -> {
            Intent intent;

            switch (entrantOptions[currentOptionIndex]) {
                case "Waitlisted Entrants":
                    intent = new Intent(this, WaitlistedEntrants.class);
                    break;
                case "Selected Entrants":
                    intent = new Intent(this, SampledEntrants.class);
                    break;
                case "Registered Entrants":
                    intent = new Intent(this, EnrolledEntrants.class);
                    break;
                case "Cancelled Entrants":
                    intent = new Intent(this, CancelledEntrants.class);
                    break;
                default:
                    return;
            }

            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }

    /**
     * Cleans up resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) {
            eventListener.remove(); // Remove the listener to avoid memory leaks
        }
    }

    private void removePicture() {
        eventPosterImageView.setImageResource(R.drawable.event_place_holder);
        deleteImageButton.setVisibility(View.GONE);
        db.collection("events").document(eventId)
                .update("imageUrl", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EventView.this, "Event picture removed successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventView.this, "Failed to remove event picture.", Toast.LENGTH_SHORT).show();

                });
    }
}