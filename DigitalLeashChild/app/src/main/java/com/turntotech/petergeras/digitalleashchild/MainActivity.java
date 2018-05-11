package com.turntotech.petergeras.digitalleashchild;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    Button reportLocation;

    EditText parentName;

    Double childLat;
    Double childLong;

    BroadcastReceiver broadcastReceiver;

    JSONObject jsonObject;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        reportLocation = findViewById(R.id.button);
        parentName = findViewById(R.id.editText);



        setupReportOnClick();

        doWeHavePermission();

        serviceIntent();


    }



    /*
       This method creates an alert message asking for permission from the user to allow the GPS
       location of the phone to be turned on. If the user denies the permission then the GPS location
       is not active. Once the user opens the app again, the permission alert will appear again.
    */
    private void doWeHavePermission() {

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PERMISSION_GRANTED  && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},100);

        }
    }



    /*
         This method takes the results of the permission from the method above, if allowed or not.
         The if statement (if approved) will allowed this class (FragmentMain) to turn on the receiver
         to get the broadcast from the GPS_Service class. This method is called once permission is
         approved and will not be called afterwords.
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == 0 && grantResults[1] == 0){
            serviceIntent();
        } else {
            reportLocation.setEnabled(false);
            reportLocation.setText("Enable GPS");
            Toast.makeText(this, "Enable GPS",
                    Toast.LENGTH_LONG).show();
        }
    }


    // If no information is entered into the EditText then a toast message will appear
    private boolean validateInputs() {

        if (parentName.getText().toString().trim().length() == 0 ){
            Toast.makeText(this, "Missing information",
                    Toast.LENGTH_LONG).show();
            return false;

        } else {
            return true;

        }
    }


    // This method sets up/creates a JSON from the EditText to be later sent to the firebase
    private void createJsonFromService(){

        jsonObject = new JSONObject();
        try {

            jsonObject.put("child_latitude", childLat);
            jsonObject.put("child_longitude", childLong);

            Log.i("JSON From EditText: ", jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    // This method sends the JSON to the firebase. The Request.Method is a PATCH that syncs the "Child
    // App" together. This is important as Patch doesn't erase the other app's data in the firebase
    // if the Request.Method is a PUT
    private void sendVolleyRequest(){

        String content = parentName.getText().toString();

        String url = "https://turntotech.firebaseio.com/digitalleash/"+content+".json";

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PATCH, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d("Response", response.toString());
                        Toast.makeText(MainActivity.this, "Sending...",
                                Toast.LENGTH_LONG).show();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        //Log.d("Error Response", error.toString());
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(putRequest);
    }




    // defines what needs to be done when you click create button
    private void setupReportOnClick(){

        // report location button onclick detect
        reportLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Create button is enabled before it is clicked. Once clicked it becomes disabled
                // In this part Create button getVolley(); from firebase

                // validate inputs
                boolean isValid = validateInputs();
                // if valid, create json from edittext and send to firebase
                if (isValid){
                    // create json from edittext
                    createJsonFromService();
                    // send json to firebase using volley
                    sendVolleyRequest();
                }
            }
        });
    }

    // This method turns on the receiver to get the broadcast from GPS_Service class
    private void serviceIntent (){
        Intent intent = new Intent(this, GPS_Service.class);
        this.startService(intent);
    }




    // The method below allows the user to lose focus of the keyboard if they click outside the
    // EditText zone. In XML, android:focusableInTouchMode="true", was added to each EditText so the
    // function can work.
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }




    // When the app is active/on
    @Override
    public void onResume() {
        super.onResume();

        // The broadcastReceiver will continue to receive signal from the GPS_Service class.
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    // Here will get the latitude and longitude from the GPS_Service class and set
                    // the EditText of both to display the latitude and longitude to the user their
                    // position.
                    childLat = intent.getDoubleExtra("child_latitude", 0);
                    childLong = intent.getDoubleExtra("child_longitude", 0);

                    Log.i("TTT", intent.getDoubleExtra("child_latitude", 0)+", "+childLong);


                }
            };

        }
        this.registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }


    // When app is closed
    @Override
    public void onDestroy() {
        super.onDestroy();
        // End broadcast from GPS_Service class.
        if (broadcastReceiver !=null){
            this.unregisterReceiver(broadcastReceiver);
        }
    }



}







