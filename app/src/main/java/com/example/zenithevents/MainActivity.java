package com.example.zenithevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zenithevents.EntrantDashboard.EntrantViewActivity;
import com.example.zenithevents.User.OrganizerPage;
import com.example.zenithevents.Admin.AdminViewActivity;
import com.example.zenithevents.HelperClasses.DeviceUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * MainActivity serves as the entry point for the application, providing buttons for users to
 * navigate to different sections of the app based on their roles, such as an entrant, organizer,
 * or admin. The activity handles Firebase authentication to manage user sessions.
 *
 * <p>Note: The Javadocs for this class were generated with the assistance of an AI language model.</p>
 *
 * @see AppCompatActivity
 * @see FirebaseAuth
 */
public class MainActivity extends AppCompatActivity {
    Button buttonEntrant;
    Button organizerButton;
    Button buttonAdmin;
    LinearLayout adminLayout;

    /**
     * Initializes the activity, sets up button click listeners for navigation
     * and initializes Firebase authentication.
     *
     * @param savedInstanceState the saved state of the application.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        buttonEntrant = findViewById(R.id.entrantButton);
        organizerButton = findViewById(R.id.organizerButton);
        buttonAdmin = findViewById(R.id.adminButton);
        adminLayout = findViewById(R.id.admin_layout);

        FirebaseMessaging.getInstance().subscribeToTopic("news")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "done";
                        if (!task.isSuccessful()) {
                            msg = "failed";
                        }
                    }
                });


        organizerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizerPage.class);
            startActivity(intent);
        });

        buttonEntrant.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EntrantViewActivity.class);
            startActivity(intent);
        });

        String deviceID = DeviceUtils.getDeviceID(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(deviceID)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        // Handle errors
                        adminLayout.setVisibility(View.GONE);
                        Log.e("Firebase", "Error retrieving user document", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        if (Boolean.TRUE.equals(isAdmin)) {
                            // Show admin layout and set click listener
                            adminLayout.setVisibility(View.VISIBLE);
                            buttonAdmin.setOnClickListener(v -> {
                                Intent intent = new Intent(MainActivity.this, AdminViewActivity.class);
                                startActivity(intent);
                            });
                        } else {
                            // Hide admin layout if user is not admin
                            adminLayout.setVisibility(View.GONE);
                        }
                    } else {
                        // Document doesn't exist, hide admin layout
                        adminLayout.setVisibility(View.GONE);
                        Log.d("UserClass", "No isAdmin field");
                    }
                });

    }
}
