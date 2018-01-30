package com.example.kavitapc.testproject;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/**
 * An activity that displays a map showing the place at the device's current location.
 * Also, add markers to the currently visible area at random positions
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private Set<Marker> markers = new HashSet<>();


    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_LOCATION = "location";
    private static final String MARKERS = "markers";
    ArrayList<LatLng> pointList = new ArrayList<LatLng>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve location and camera position from saved instance state.
        //Retrieve the markers from saved instance
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            if(savedInstanceState.containsKey(MARKERS)){
                pointList = savedInstanceState.getParcelableArrayList(MARKERS);
                }
        }
        setContentView(R.layout.activity_maps);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Get the location permissions
        getLocationPermission();

        //Show the markers added if saved through onSaveInstanceState
        if(pointList!=null) {
            for (int i = 0; i < pointList.size(); i++) {
                Marker marker = mMap.addMarker(new MarkerOptions().position(pointList.get(i)).title("New Location"));
                markers.add(marker);
                startFadeInOutMarkerAnimation(marker, false);
            }
        }
        //Show the device location if saved through onSaveInstanceState
      if(mLastKnownLocation != null) {
          mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                  new LatLng(mLastKnownLocation.getLatitude(),
                          mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
          mMap.addCircle(new CircleOptions().center(new LatLng(mLastKnownLocation.getLatitude(),
                  mLastKnownLocation.getLongitude()))
                  .radius(20)
                  .strokeColor(Color.RED)
                  .fillColor(Color.BLUE));
      }

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            outState.putParcelableArrayList(MARKERS, pointList);

            super.onSaveInstanceState(outState);
        }
    }

/* Find the user's current device location and moves the camera to current location**/
    public void findDeviceLocation(View view) {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            mMap.addCircle(new CircleOptions()
                                    .center(new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()))
                                    .radius(20)
                                    .strokeColor(Color.RED)
                                    .fillColor(Color.BLUE));
                        } else {

                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            //Log.e("Exception: %s", e.getMessage());
        }
    }


    /**Generate markers to the currently visible map area with fade in animation
     *Also, remove the previously added markers with Fade out animation.
     **/

    public void generateMarkers(View view) {

        if (!markers.isEmpty()|| !pointList.isEmpty()) {
            for (Marker marker : markers) {
                startFadeInOutMarkerAnimation(marker, true);
            }
            markers.clear();
            pointList.clear();
        }

        try {
            if (mLocationPermissionGranted) {
                //get the visible area bounds of map
                LatLngBounds b = mMap.getProjection().getVisibleRegion().latLngBounds;

                //this will add 10 markers on currently visible area of map
                for (int i = 0; i < 10; i++) {
                    LatLng randLocation = new LatLng(generateRandom(b.northeast.latitude, b.southwest.latitude), generateRandom(b.northeast.longitude, b.southwest.longitude));
                    Marker marker = mMap.addMarker(new MarkerOptions().position(randLocation).title("New Location"));
                    markers.add(marker);
                    pointList.add(randLocation);

                    //Animation
                    startFadeInOutMarkerAnimation(marker, false);
                }
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**To generate random value between given range**/
    private double generateRandom(double max, double min) {
        return min + (max - min) * Math.random();
    }

    /**Add fade in and Fade out animation to the markers**/
    private void startFadeInOutMarkerAnimation(final Marker marker, final boolean flagAddRemove) {
        Animator animator;
        if (flagAddRemove) {
            animator = ObjectAnimator.ofFloat(marker, "alpha", 1f, 0.75f, 0.5f, 0.25f, 0f);
        } else {
            animator = ObjectAnimator.ofFloat(marker, "alpha", 0f, 0.25f, 0.5f, 0.75f, 1f);
        }

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (flagAddRemove) {marker.remove();}
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.setDuration(800).start();

    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }


}
