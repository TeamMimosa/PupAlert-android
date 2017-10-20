package com.teammimosa.pupalert_android;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

/**
 * The activity for creating a new post
 *
 * @author Sydney Micklas, Domenic Portuesi
 */
public class NewPostFragment extends Fragment implements View.OnClickListener
{
    private FusedLocationProviderClient mFusedLocationClient;
    private double userLat = 0;
    private double userLong = 0;

    private Uri file;
    private ImageView cameraButton; //Made ImageView instead of button to make displaying photo easier
    private TextView currentLoc;
    private Button postButton;
    PupAlertFirebase database;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraButton = (ImageView) rootView.findViewById(R.id.new_post_pic);
        postButton = (Button) rootView.findViewById(R.id.post_button);
        currentLoc = (TextView) rootView.findViewById(R.id.loc_display);

        //Disable camera button if permission to use camera was denied
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            cameraButton.setEnabled(false);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        //Check location for when we get the users loc for database
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            currentLoc.setText("Could not get location: permission denied");
        }

        //Button for taking picture
        cameraButton.setOnClickListener(new View.OnClickListener()
        {
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

        //Button for posting data
        postButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.post_button:
                        database.storePost("user-test", Utils.getTimeStampForDatabase(), userLat, userLong, file);
                        break;
                }
            }
        });


        //add location listener to get curr lat and long.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        mFusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>()
        {
            @Override
            public void onSuccess(Location location)
            {
                if (location != null)
                {
                    userLat =  location.getLatitude();
                    userLong = location.getLongitude();
                }
            }
        });

        database = new PupAlertFirebase(getActivity());
        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == 0)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                cameraButton.setEnabled(true);
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
                cameraButton.setImageURI(file);
                cameraButton.setBackgroundResource(R.drawable.rounded);
                //button.setScaleType(ImageView.ScaleType.CENTER_CROP);

                //Set text to cur loc
                try
                {
                    Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
                    List<Address> addresses = gcd.getFromLocation(userLat, userLong, 1);
                    if (addresses.size() > 0)
                    {
                        currentLoc.setText(addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea());
                    } else
                    {
                        currentLoc.setText("Location not found");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


}
