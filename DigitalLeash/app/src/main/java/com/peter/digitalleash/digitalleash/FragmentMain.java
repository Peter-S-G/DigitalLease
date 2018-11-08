package com.peter.digitalleash.digitalleash;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import static android.content.pm.PackageManager.*;

public class FragmentMain extends Fragment {

    EditText userName;
    EditText latitude;
    EditText longitude;
    EditText radius;

    Button create;
    Button update;
    Button status;
    Button woowoo;
    Button uh_oh;

    JSONObject jsonObject;

    BroadcastReceiver broadcastReceiver;

    int radius1;

    float distance;



    private static final String parentLatitude = "latitude";
    private static final String parentLongitude = "longitude";

    private static final String childLatitude = "child_latitude";
    private static final String childLongitude = "child_longitude";

    private static final String radiusJSON = "radius";




    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(
                R.layout.fragment_main, container, false);



        create = view.findViewById(R.id.create);
        update = view.findViewById(R.id.update);
        status = view.findViewById(R.id.status);
        woowoo = view.findViewById(R.id.woo_woo_button);
        uh_oh = view.findViewById(R.id.uh_oh_button);

        userName = view.findViewById(R.id.user_name);
        latitude = view.findViewById(R.id.latitude);
        longitude = view.findViewById(R.id.longitude);
        radius = view.findViewById(R.id.radius);



        setupCreateOnClick();

        setupUpdateOnCLick();

        setupStatusOnClick();


        userName.clearFocus();
        radius.clearFocus();
        latitude.clearFocus();
        longitude.clearFocus();



        /*
        If we have location permission (mentioned below in a different method) be approved, then
        this class (FragmentMain) will be allowed to turn on the receiver to get the broadcast
        from the GPS_Service class. At this point, permission was approved once before. This if
        statement allows the receiver to be continuously be turned on without needing to ask
        permission again. Without it the receiver will be turned off once the app is opened again.
        */
        if (doWeHavePermission()==true) {
            show_gps();
        }

        return view;
    }



    // If there is no text/number in userName or radius EditText then a toast message will appear
    private boolean validateInputs() {

        if (userName.getText().toString().trim().length() == 0 ||
                radius.getText().toString().trim().length() == 0 ){

            Toast.makeText(FragmentMain.this.getActivity(), "Missing information",
                    Toast.LENGTH_LONG).show();
            return false;

        } else {
            return true;

        }
    }



    // This method sets up/creates a JSON from the EditText to be later sent to the firebase
    private void createJsonFromEditText(){

        jsonObject = new JSONObject();
        try {
            Double lat = Double.valueOf(latitude.getText().toString());
            Double lon = Double.valueOf(longitude.getText().toString());
            Integer rad = Integer.valueOf(radius.getText().toString());

            jsonObject.put("username",userName.getText().toString());
            jsonObject.put("latitude", lat);
            jsonObject.put("longitude", lon);
            jsonObject.put("radius",rad);
            Log.i("JSON From EditText: ", jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
     }



    // This method sends the JSON to the firebase. The Request.Method is a PATCH that syncs the "Child
    // App" together. This is important as Patch doesn't erase the other app's data in the firebase
    // if the Request.Method is a PUT
    private void sendVolleyRequest(){

        String content = userName.getText().toString();

        String url = "https://turntotech.firebaseio.com/digitalleash/"+content+".json";

         JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PATCH, url, jsonObject,
                 new Response.Listener<JSONObject>() {
                     @Override
                     public void onResponse(JSONObject response) {
                         //Log.d("Response", response.toString());
                         Toast.makeText(FragmentMain.this.getActivity(), "Data Saved",
                                 Toast.LENGTH_SHORT).show();

                     }
                 },
                 new Response.ErrorListener() {
                     @Override
                     public void onErrorResponse(VolleyError error) {
                         Toast.makeText(FragmentMain.this.getActivity(), "Error",
                                 Toast.LENGTH_SHORT).show();
                         //Log.d("Error Response", error.toString());
                     }
                 });

         RequestQueue requestQueue = Volley.newRequestQueue(this.getActivity());
         requestQueue.add(putRequest);
    }



    // This is the method that retrieves the JSON file from the firebase. It retrieves the parent's and
    // child's location (sent from a different app) and calculates the distance.
    private void getVolley() {


        String content = userName.getText().toString();

        String url = "https://turntotech.firebaseio.com/digitalleash/"+content+".json";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            Double parentLat = response.getDouble(parentLatitude);
                            Double parentLong = response.getDouble(parentLongitude);

                            Double childLat = response.getDouble(childLatitude);
                            Double childLong = response.getDouble(childLongitude);

                            radius1 = response.getInt(radiusJSON);

                            // Here we calculate the distance between the parent and child based of
                            // their location.
                            Location parent = new Location("parent");

                            parent.setLatitude(parentLat);
                            parent.setLongitude(parentLong);

                            Location child = new Location("child");

                            child.setLatitude(childLat);
                            child.setLongitude(childLong);

                            distance = parent.distanceTo(child);


                            // As we are using Fragments, the distance between the parent and child
                            // will decide whether the next fragment will go to FragmentInZone or
                            // FragmentOutZone
                            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                            if (distance<=(radius1*1000)){
                                fragmentTransaction.add(R.id.fragment_con, new FragmentInZone());
                                fragmentTransaction.addToBackStack("Main");
                                fragmentTransaction.commit();

                            } else if (distance>(radius1*1000)){
                                fragmentTransaction.add(R.id.fragment_con, new FragmentOutZone());
                                fragmentTransaction.addToBackStack("Main");
                                fragmentTransaction.commit();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), "User not found", Toast.LENGTH_LONG).show();
                    }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this.getActivity());
        requestQueue.add(jsonObjectRequest);
    }



    // This method turns on the receiver to get the broadcast from GPS_Service class
    private void show_gps(){

        Intent intent = new Intent(FragmentMain.this.getActivity(), GPS_Service.class);
        FragmentMain.this.getActivity().startService(intent);

    }


    /*
    This method creates an alert message asking for permission from the user to allow the GPS
    location of the phone to be turned on. If the user denies the permission then the GPS location
    is not active. Once the user opens the app again, the permission alert will appear again.
    */
    private boolean doWeHavePermission() {

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
                FragmentMain.this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PERMISSION_GRANTED  && ContextCompat.checkSelfPermission(
                        FragmentMain.this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return false; // we need permission so returning false

        }
        return true; // we have permission
    }


    /*
     This method takes the results of the permission from the method above, if allowed or not.
     The if statement (if approved) will allowed this class (FragmentMain) to turn on the receiver
     to get the broadcast from the GPS_Service class. This method is called once permission is
     approved and will not be called afterwords. Once it is granted, the need for the if statement
     on line 127 is needed to have the receiver turn on.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == 0 && grantResults[1] == 0){

            show_gps();
        }
    }




    // defines what needs to be done when you click create button
    private void setupCreateOnClick(){

        // create button onclick detect
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Create button is enabled before it is clicked. Once clicked it becomes disabled
                // In this part Create button getVolley(); from firebase

                // validate inputs
                boolean isValid = validateInputs();
                // if valid, create json from edittext and send to firebase
                if(isValid){
                    // create json from edittext
                    createJsonFromEditText();
                    // send json to firebase using volley
                    sendVolleyRequest();
                }
            }
        });
    }


    // defines what needs to be done when you click update button
    private void setupUpdateOnCLick(){

        // update button onclick detect
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Update button calls these methods to create json from EditText and send json to
                // firebase using volley
                createJsonFromEditText();
                sendVolleyRequest();

            }
        });
    }



    // defines what needs to be done when you click status button
    private void setupStatusOnClick(){

        // update button onclick detect
        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Once the status button is clicked it will call the getVolley() method which will
                // receive the JSON file from the firebase. The method will calculate the distance
                // and determine which fragment to go to next.
                getVolley();

            }
        });
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
                    latitude.setText("" + intent.getExtras().get("latitude"));
                    longitude.setText("" + intent.getExtras().get("longitude"));

                    create.setEnabled(true);
                    status.setEnabled(true);
                    update.setEnabled(true);

                }
            };

        }
        FragmentMain.this.getActivity().registerReceiver(broadcastReceiver, new
                IntentFilter("location_update"));
    }


    // When app is closed
    @Override
    public void onDestroy() {
        super.onDestroy();
        // End broadcast from GPS_Service class.
        if (broadcastReceiver !=null){
            FragmentMain.this.getActivity().unregisterReceiver(broadcastReceiver);
        }
    }
}



