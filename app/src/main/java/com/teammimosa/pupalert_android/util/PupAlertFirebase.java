package com.teammimosa.pupalert_android.util;

import android.*;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions for storing/reading from database.
 * Instantiate in onCreate() to make sure getActivity() is not null.
 * @author Domenic Portuesi
 */

public class PupAlertFirebase
{
    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("posts");

    private Activity activity;

    public PupAlertFirebase(Activity activity)
    {
        this.activity = activity;
    }

    /**
     * Uploads post to database
     */
    public void storePost(String postedBy, String timeStamp, Double lat, Double longi, Uri photo)
    {
        //Check if we have the current location
        if(ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            String key = databaseRef.child("posts").push().getKey(); //generate random value
            Post post = new Post(postedBy, timeStamp);
            Map<String, Object> postValues = post.toMap();
            Map<String, Object> childUpdates = new HashMap<>();

            childUpdates.put("/posts/" + key, postValues);

            databaseRef.updateChildren(childUpdates);
            //store image
            storeImage(photo, key);

            //geofire push loc;
            DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
            GeoFire geoFire = new GeoFire(geoRef);
            geoFire.setLocation(key, new GeoLocation(lat, longi));
        }
        else
        {
            Toast.makeText(activity.getApplicationContext(), "Location permissions not allowed, not posting!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Uploads post to database
     */
    public void storeUser(String uid, String name, int total_posts)
    {
        User user = new User(name, total_posts);
        Map<String, Object> postValues = user.toMap();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/users/" + uid, postValues);

        databaseRef.updateChildren(childUpdates);

    }

    public void incrementPostsByOne(String uid)
    {
        DatabaseReference mRef = databaseRef.child("users").child(uid);
        mRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    PupAlertFirebase.User user = dataSnapshot.getValue(PupAlertFirebase.User.class);
                    int newTot = user.gettotal_posts() + 1;
                    user.settotal_posts(newTot);
                    dataSnapshot.getRef().setValue(user);
                    dataSnapshot.getRef().child("total_posts").setValue(newTot);
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

    private void storeImage(Uri uri, String postedBy)
    {
        //TODO UID Stuff
        //https://firebase.google.com/docs/auth/web/manage-users
        //https://stackoverflow.com/questions/43470758/how-to-save-image-to-firebase

        //storageRef.child(id).child(id).putFile(uri); ### this one creates a sub folder for the post. Only necessary if we have more than one photo.
        storageRef.child(postedBy).putFile(uri);
    }

    public static class Post
    {
        public String userID;
        public String timestamp;

        public Post(String postedBy, String timestamp)
        {
            this.userID = postedBy;
            this.timestamp = timestamp;
        }

        public Post(){}

        public void setpostedBy(String id)
        {
            userID = id;
        }

        public String getpostedBy()
        {
            return userID;
        }

        public void settimestamp(String timestamp)
        {
            this.timestamp = timestamp;
        }

        public String getttimestamp()
        {
            return this.timestamp;
        }

        @Exclude
        public Map<String, Object> toMap()
        {
            HashMap<String, Object> result = new HashMap<>();
            result.put("postedBy", userID);
            result.put("timestamp", timestamp);
            return result;
        }
    }

    public static class User
    {
        public String name;
        public int total_posts = 0;

        public User(String name, int total_posts)
        {
            this.name = name;
            this.total_posts = total_posts;
        }

        public User(){}

        public String getname()
        {
            return name;
        }

        public int gettotal_posts()
        {
            return total_posts;
        }

        public void setname(String name)
        {
            this.name = name;
        }

        public void settotal_posts(int post)
        {
            this.total_posts = post;
        }

        @Exclude
        public Map<String, Object> toMap()
        {
            HashMap<String, Object> result = new HashMap<>();
            result.put("name", name);
            result.put("total_posts", total_posts);
            return result;
        }
    }
}
