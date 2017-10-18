package com.teammimosa.pupalert_android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The fragment for our map viewer.
 *
 * @author Domenic Portuesi
 */
public class MapsFragment extends Fragment implements LocationListener, GeoQueryEventListener
{
    private MapView mMapView;
    public GoogleMap googleMap;
    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final GeoLocation INITIAL_CENTER = new GeoLocation(47.115, -88.541);
    private GeoFire geoFire;
    private GeoQuery geoQuery;
    private Map<String,Marker> markers;

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
            }
        });

        // setup GeoFire
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire");
        this.geoFire = new GeoFire(ref);
        this.geoQuery = this.geoFire.queryAtLocation(INITIAL_CENTER, 100);
        this.markers = new HashMap<String, Marker>();

        this.geoQuery.addGeoQueryEventListener(this);

        return rootView;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        //move camera to user position
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        googleMap.animateCamera(cameraUpdate);
        locationManager.removeUpdates(this);
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
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        }
    }

    //https://github.com/firebase/geofire-java/blob/master/examples/SFVehicles/SF%20Vehicles/src/main/java/com/firebase/sfvehicles/SFVehiclesActivity.java

    @Override
    public void onKeyEntered(String key, GeoLocation location)
    {
        // Add a new marker to the map
        Marker marker = this.googleMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)));
        this.markers.put(key, marker);

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


