package com.peter.digitalleash.digitalleash;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.fragment_con, new FragmentMain());
        fragmentTransaction.commit();

    }

    public void selectFrag(View view) {

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        // Status button is clicked, it can go from FragmentInZone or FragmentOutZone and return back.
        // It depends on the location of the child. In this case it will allow the fragments to
        // return to FragmentMain. To go to FragmentInZone or FragmentOutZone is coded in FragmentMain's
        // getVolley().
        if (view == findViewById(R.id.status)) {

            fragmentTransaction.addToBackStack("Main");
            fragmentTransaction.commit();

        } else if (view == findViewById(R.id.woo_woo_button)) {
             getFragmentManager().popBackStack();

        } else if (view == findViewById(R.id.uh_oh_button)) {
            getFragmentManager().popBackStack();

        }
    }


    // The method below allows the user to lose focus of the keyboard if they click outside the
    // EditText zone. In XML, android:focusableInTouchMode="true", was added to each EditText so the
    // function can work.
    @Override
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

}

