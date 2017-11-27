package com.teammimosa.pupalert_android.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Fragment that demonstrates how to use CardView.
 */
public class FragmentFeed extends Fragment implements SwipeRefreshLayout.OnRefreshListener
{
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private TextView mEmptyText;

    public SwipeRefreshLayout swipeLayout;

    private ArrayList<FeedPost> posts;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NotificationFragment.
     */
    public static FragmentFeed newInstance()
    {
        FragmentFeed fragment = new FragmentFeed();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public FragmentFeed() {}

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

        mEmptyText = rootView.findViewById(R.id.feed_no_data);

        // Code to Add an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).addItem(obj, index);

        // Code to remove an item with default animation
        //((MyRecyclerViewAdapter) mAdapter).deleteItem(index);

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_feed);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(android.R.color.holo_orange_light);
        swipeLayout.setRefreshing(true);

        loadCards(rootView);

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
    public void onRefresh()
    {
        //reload fragment
        Utils.switchToFragment(getActivity(), FragmentFeed.newInstance());
    }

    public void loadCards(View rootView)
    {
        DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
        GeoFire geoFire = new GeoFire(geoRef);
        if(Utils.cachedLoc.longitude != 0) //check if the activity retrieved the location
        {
            //GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(curLoc.latitude, curLoc.longitude), 10);
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(Utils.cachedLoc.latitude, Utils.cachedLoc.longitude), Utils.FEED_LOCATION_RADIUS);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener()
            {
                @Override
                public void onKeyEntered(final String key, final GeoLocation location)
                {
                    //get date ranges to be in

                    Date date = new Date();
                    Calendar queryRangeLow = Calendar.getInstance();
                    queryRangeLow.setTime(date);
                    queryRangeLow.add(Calendar.MINUTE, -30);

                    Calendar queryRangeHi = Calendar.getInstance();
                    queryRangeHi.setTime(date);

                    DateFormat dateFormat = Utils.dateFormat;
                    String lo = dateFormat.format(queryRangeLow.getTime());
                    String hi = dateFormat.format(queryRangeHi.getTime());

                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("posts").child(key);
                    dbRef.addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            if(dataSnapshot.exists())
                            {
                                final PupAlertFirebase.Post post = dataSnapshot.getValue(PupAlertFirebase.Post.class);

                                //query uid for real name
                                DatabaseReference mRef = FirebaseDatabase.getInstance().getReference().child("users").child(post.userID);
                                mRef.addListenerForSingleValueEvent(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        if(dataSnapshot.exists())
                                        {
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
                                                //add post to the adapater.
                                                PupAlertFirebase.User user = dataSnapshot.getValue(PupAlertFirebase.User.class);
                                                FeedPost feedPost = new FeedPost(user.getname(), key, new LatLng(location.latitude, location.longitude), post.getttimestamp());

                                                posts.add(feedPost);
                                                //re-create the adapter.
                                                mAdapter = new FeedRecyclerViewAdapter(posts, getActivity());
                                                mRecyclerView.setAdapter(mAdapter);
                                                mRecyclerView.setNestedScrollingEnabled(false);

                                                mEmptyText.setVisibility(View.INVISIBLE);
                                            }
                                            else
                                            {
                                                mEmptyText.setVisibility(View.VISIBLE);
                                            }
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

            swipeLayout.setRefreshing(false);
            if(!posts.isEmpty())
            {
                mEmptyText.setVisibility(View.INVISIBLE);
            }
        }
        else
        {
            swipeLayout.setRefreshing(true);
            mEmptyText.setVisibility(View.INVISIBLE);
        }
    }
}