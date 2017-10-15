package com.teammimosa.pupalert_android;

import android.*;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
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
    public void storePost(Double lat, Double longi, String id, Uri phtl)
    {
        //TODO UID Stuff
        //https://firebase.google.com/docs/auth/web/manage-users

        //Check if we have the current location
        if(ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            String key = databaseRef.child("posts").push().getKey(); //generate random value
            Post post = new Post(lat, longi, id, phtl);
            Map<String, Object> postValues = post.toMap();
            Map<String, Object> childUpdates = new HashMap<>();

            childUpdates.put("/posts/" + id + key, postValues);
            databaseRef.updateChildren(childUpdates);
            //store image
            storeImage(phtl, id + key);
        }
        else
        {
            Toast.makeText(activity.getApplicationContext(), "Location permissions not allowed, not posting!", Toast.LENGTH_LONG).show();
        }
    }

    private void storeImage(Uri uri, String id)
    {
        //TODO UID Stuff
        //https://firebase.google.com/docs/auth/web/manage-users
        //https://stackoverflow.com/questions/43470758/how-to-save-image-to-firebase

        //storageRef.child(id).child(id).putFile(uri); ### this one creates a sub folder for the post. Only necessary if we have more than one photo.
        storageRef.child(id).putFile(uri);
    }

    public static class Post
    {
        public Double userLat;
        public Double userLong;
        public String userID;

        public Post(Double lat, Double longi, String id, Uri pht)
        {
            userLat = lat;
            userLong = longi;
            userID = id;
        }

        @Exclude
        public Map<String, Object> toMap()
        {
            HashMap<String, Object> result = new HashMap<>();
            result.put("uid", userID);
            result.put("lat", userLat);
            result.put("long", userLong);
            return result;
        }
    }
}
