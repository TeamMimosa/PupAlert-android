package com.teammimosa.pupalert_android;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * The activity for creating a new post
 * @author Sydney Micklas, Domenic Portuesi
 */
public class NewPostFragment extends Fragment implements View.OnClickListener
{
    private Uri file;
    private ImageView button; //Made ImageView instead of button to make displaying photo easier
    private TextView currentLoc;
    private double userLat;
    private double userLong;
    final FirebaseDatabase pupDataBase = FirebaseDatabase.getInstance();
    DatabaseReference ref = pupDataBase.getReference("https://pupalert-f3b79.firebaseio.com/");
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        button = (ImageView) rootView.findViewById(R.id.new_post_pic);
        currentLoc = (TextView) rootView.findViewById(R.id.loc_display);

        //Disable camera button if permission to use camera was denied
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            button.setEnabled(false);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        //Check location for when we get the users loc for database
        if(ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            currentLoc.setText("Could not get location: permission denied");
        }

        //ActionListener for "button"
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.new_post_pic:
                        takePicture(v);
                        break;
                }
            }
        });
        retrieveLocation();
        return rootView;

    }

    //Retrieves, displays, stores the users location
    public void retrieveLocation(){
        //TO BE IMPLEMENTED
        currentLoc.setText("1801 Woodland Road");   //PLACEHOLDER DATA
        userLat = 65.222344;    //PLACEHOLDER
        userLong = 94.349554; //PLACEHOLDER
    }

    //Stores data to server
    public void storeData(){
        DatabaseReference usersRef = ref.child("uid");
        User user = new User(45.4444, 24.4334, "Sydney", "photo loc");
        usersRef.setValue(user);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == 0)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                button.setEnabled(true);
            }
        }
    }

    //Takes photo (launches camera) , builds file, stores to phone
    public void takePicture(View view)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, 100);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.new_post_pic:
                takePicture(v);
                break;
        }
    }

    //Stores photo to user local data; this may be removed later
    private static File getOutputMediaFile()
    {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "PupAlert");

        if (!mediaStorageDir.exists())
        {
            if (!mediaStorageDir.mkdirs())
            {
                Log.d("PupAlert", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
    }

    //Displays photo taken in the ImageView "button"
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 100)
        {
            if (resultCode == RESULT_OK)
            {
                button.setImageURI(file);
                button.setBackgroundResource(R.drawable.rounded);
                storeData();
                //button.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
    }

    public static class User{
        private Double userLat;
        private Double userLong;
        private String userID;
        private String photo;

        public User(Double lat, Double longi, String id, String pht){
            userLat = lat;
            userLong = longi;
            userID = id;
            photo = pht;
        }

    }
}
