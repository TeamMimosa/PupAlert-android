package com.teammimosa.pupalert_android;

import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that demonstrates how to use CardView.
 */
public class FeedFragment extends Fragment
{
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<FeedPost> posts;

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

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("posts");

        // Attach a listener to read the data at our posts reference
        dbRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s)
            {
                //query location of post key
                //FIXME query within query? No problem. YET.
                //TODO SORTING CARDS
                DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
                GeoFire geoFire = new GeoFire(geoRef);
                geoFire.getLocation(dataSnapshot.getKey(), new LocationCallback()
                {
                    @Override
                    public void onLocationResult(final String key, final GeoLocation location)
                    {
                        if (location != null)
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
                                    else
                                    {
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError)
                                {

                                }
                            });


                        }
                        else
                        {
                            System.out.println(String.format("There is no location for key %s in GeoFire", key));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        System.err.println("There was an error getting the GeoFire location: " + databaseError);
                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
            }

            @Override
            public void onChildRemoved(final DataSnapshot dataSnapshot)
            {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s)
            {
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
            }
        });

        // Code to Add an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).addItem(obj, index);

        // Code to remove an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).deleteItem(index);

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
}