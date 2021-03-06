package com.teammimosa.pupalert_android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.services.ServiceBackgroundLocation;
import com.teammimosa.pupalert_android.util.PermissionUtils;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The fragment for our map viewer.
 *
 * @author Domenic Portuesi
 */
public class FragmentMap extends Fragment implements LocationListener, GeoQueryEventListener
{
    private MapView mMapView;
    public GoogleMap googleMap;

    private static final GeoLocation INITIAL_CENTER = new GeoLocation(51.574446, -17.031768); //Center of maps (kind of)
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private Map<String,Marker> markers;

    private LocationManager locationManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NotificationFragment.
     */
    public static FragmentMap newInstance()
    {
        FragmentMap fragment = new FragmentMap();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public FragmentMap(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_location, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try
        {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback()
        {
            @Override
            public void onMapReady(GoogleMap mMap)
            {
                googleMap = mMap;
                enableMyLocation();

                //zoom to location when fragment is created
                if(Utils.cachedLoc.longitude != 0)
                    animateCameraTo(new LatLng(Utils.cachedLoc.latitude, Utils.cachedLoc.longitude));
            }
        });

        // setup GeoFire
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire");
        this.geoFire = new GeoFire(ref);
        this.geoQuery = this.geoFire.queryAtLocation(INITIAL_CENTER, 9999); //query the whole earth
        this.markers = new HashMap<String, Marker>();

        this.geoQuery.addGeoQueryEventListener(this);

        return rootView;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        animateCameraTo(latLng);
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation()
    {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Permission to access the location is missing.
            //ASSUMING the main activity as a appcompatactivity
            PermissionUtils.requestPermission((AppCompatActivity) getActivity(), LOCATION_PERMISSION_REQUEST_CODE, android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (googleMap != null)
        {
            // Access to the location has been granted to the app.
            googleMap.setMyLocationEnabled(true);

            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Utils.MIN_LOCATION_CHECK_TIME, Utils.MIN_LOCATION_DISTANCE_CHECK, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        }
    }

    //https://github.com/firebase/geofire-java/blob/master/examples/SFVehicles/SF%20Vehicles/src/main/java/com/firebase/sfvehicles/SFVehiclesActivity.java

    @Override
    public void onKeyEntered(final String key, final GeoLocation location)
    {
        //check if marker is within 30 mins
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("posts").child(key);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    final PupAlertFirebase.Post post = dataSnapshot.getValue(PupAlertFirebase.Post.class);
                    DateFormat dateFormat = Utils.dateFormat;

                    //check for if its the last 30 mins
                    String timeStamp = post.getttimestamp();
                    Date timeStampDate = new Date();

                    Date currentDate = new Date();
                    Calendar calTimestampLo = Calendar.getInstance();
                    calTimestampLo.setTime(currentDate);
                    calTimestampLo.add(Calendar.MINUTE, -30);

                    Calendar calTimestampHi = Calendar.getInstance();
                    calTimestampHi.setTime(currentDate);

                    try
                    {
                        timeStampDate = dateFormat.parse(timeStamp);
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                    }

                    Calendar calTimestamp = Calendar.getInstance();
                    calTimestamp.setTime(timeStampDate);

                    //add the post if the time is the last 30 mins
                    if(calTimestamp.after(calTimestampLo) && calTimestamp.before(calTimestampHi))
                    {
                        // Add a new marker to the map
                        Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));
                        markers.put(key, marker);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    @Override
    public void onKeyExited(String key)
    {
        // Remove any old marker
        Marker marker = this.markers.get(key);
        if (marker != null)
        {
            marker.remove();
            this.markers.remove(key);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location)
    {

    }

    @Override
    public void onGeoQueryReady()
    {

    }

    @Override
    public void onGeoQueryError(DatabaseError error)
    {

    }

    private void animateCameraTo(LatLng latLng)
    {
        //move camera to user position
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
    }

    //FOR FUTURE USE:

    /*
    * Returns the radius in visible region.
     */
    private double calculateVisibleRadius(VisibleRegion visibleRegion)
    {
        float[] distanceWidth = new float[1];
        float[] distanceHeight = new float[1];

        LatLng farRight = visibleRegion.farRight;
        LatLng farLeft = visibleRegion.farLeft;
        LatLng nearRight = visibleRegion.nearRight;
        LatLng nearLeft = visibleRegion.nearLeft;

        //calculate the distance width (left <-> right of map on screen)
        Location.distanceBetween(
                (farLeft.latitude + nearLeft.latitude) / 2,
                farLeft.longitude,
                (farRight.latitude + nearRight.latitude) / 2,
                farRight.longitude,
                distanceWidth
        );

        //calculate the distance height (top <-> bottom of map on screen)
        Location.distanceBetween(
                farRight.latitude,
                (farRight.longitude + farLeft.longitude) / 2,
                nearRight.latitude,
                (nearRight.longitude + nearLeft.longitude) / 2,
                distanceHeight
        );

        //visible radius is (smaller distance) / 2:
        return (distanceWidth[0] < distanceHeight[0]) ? distanceWidth[0] / 2 : distanceHeight[0] / 2;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    //Location listener functions we dont use

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }
}


