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
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("posts");

        // Attach a listener to read the data at our posts reference
        dbRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                PupAlertFirebase.Post post = dataSnapshot.getValue(PupAlertFirebase.Post.class);
                FeedPost feedPost = new FeedPost(post.userID, dataSnapshot.getKey());
                posts.add(feedPost);
                mAdapter = new FeedRecyclerViewAdapter(posts, getActivity());
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setNestedScrollingEnabled(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
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