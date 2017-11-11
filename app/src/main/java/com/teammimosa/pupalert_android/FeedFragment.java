package com.teammimosa.pupalert_android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.teammimosa.pupalert_android.util.PermissionUtils;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that demonstrates how to use CardView.
 */
public class FeedFragment extends Fragment implements LocationListener
{
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<FeedPost> posts;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long MIN_TIME = 60000;
    private static final float MIN_DISTANCE = 1000;

    private LatLng curLoc = new LatLng(0,0);

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NotificationFragment.
     */
    public static FeedFragment newInstance()
    {
        FeedFragment fragment = new FeedFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public FeedFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_feed, container, false);

        posts = new ArrayList<FeedPost>();

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.feed_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new FeedRecyclerViewAdapter(posts, getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setNestedScrollingEnabled(false);

        // Code to Add an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).addItem(obj, index);

        // Code to remove an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).deleteItem(index);

        enableMyLocation();

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ((FeedRecyclerViewAdapter) mAdapter).setOnItemClickListener(new FeedRecyclerViewAdapter.MyClickListener()
        {
            @Override
            public void onItemClick(int position, View v)
            {
                //TODO implement when card is clicked
            }
        });
    }

    @Override
    public void onLocationChanged(Location location)
    {
        curLoc = new LatLng(location.getLatitude(), location.getLongitude());
        loadCards();
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
        } else
        {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        }
    }

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

    public void loadCards()
    {
        DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
        GeoFire geoFire = new GeoFire(geoRef);
        if(curLoc.longitude != 0)
        {
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(curLoc.latitude, curLoc.longitude), 10);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener()
            {
                @Override
                public void onKeyEntered(final String key, final GeoLocation location)
                {
                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("posts").child(key);
                    dbRef.addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            if(dataSnapshot.exists())
                            {
                                PupAlertFirebase.Post post = dataSnapshot.getValue(PupAlertFirebase.Post.class);

                                //query uid for real name
                                DatabaseReference mRef = FirebaseDatabase.getInstance().getReference().child("users").child(post.userID);
                                mRef.addListenerForSingleValueEvent(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        if(dataSnapshot.exists())
                                        {
                                            //add post to the adapater.
                                            PupAlertFirebase.User user = dataSnapshot.getValue(PupAlertFirebase.User.class);

                                            FeedPost feedPost = new FeedPost(user.getname(), key, new LatLng(location.latitude, location.longitude));

                                            posts.add(feedPost);
                                            //re-create the adapter.
                                            mAdapter = new FeedRecyclerViewAdapter(posts, getActivity());
                                            mRecyclerView.setAdapter(mAdapter);
                                            mRecyclerView.setNestedScrollingEnabled(false);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError)
                                    {

                                    }
                                });

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
            });
        }
    }
}