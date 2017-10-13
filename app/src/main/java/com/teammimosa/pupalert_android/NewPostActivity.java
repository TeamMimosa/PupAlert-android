package com.teammimosa.pupalert_android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/**
 * The activity for creating a new post
 * @author Sydney Micklas, Domenic Portuesi
 */
public class NewPostActivity extends Fragment
{
    private Uri file;
    private ImageView button;
    private TextView currentLoc;

    protected void onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_camera);
        button = (ImageView) findViewById(R.id.imageView1);
        currentLoc = (TextView) findViewById(R.id.loc_display);
        if (ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            button.setEnabled(false);
        }
        if(ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            currentLoc.setText("Could not get location: permission denied");
        }
    }

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

    public void takePicture(View view)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, 100);
    }

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


    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 100)
        {
            if (resultCode == RESULT_OK)
            {
                button.setImageURI(file);
                button.setBackgroundResource(R.drawable.rounded);
                //button.setScaleType(ImageView.ScaleType.CENTER_CROP);


            }
        }
    }
}
