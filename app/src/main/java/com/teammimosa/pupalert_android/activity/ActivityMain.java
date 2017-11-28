package com.teammimosa.pupalert_android.activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.fragment.FragmentAccount;
import com.teammimosa.pupalert_android.fragment.FragmentFeed;
import com.teammimosa.pupalert_android.fragment.FragmentMap;
import com.teammimosa.pupalert_android.fragment.FragmentNewPost;
import com.teammimosa.pupalert_android.services.ServiceBackgroundLocation;
import com.teammimosa.pupalert_android.util.PermissionUtils;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Our main activity that hosts the nav bar and all fragments.
 * @author Domenic Portuesi
 */
public class ActivityMain extends AppCompatActivity implements LocationListener
{
    private static final String SELECTED_ITEM = "arg_selected_item";

    private BottomNavigationView mBottomNav;
    private int mSelectedItem;

    private LocationManager locationManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long MIN_TIME = 60000;
    private static final float MIN_DISTANCE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        mBottomNav = (BottomNavigationView) findViewById(R.id.navigation);
        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                selectFragment(item);
                return true;
            }
        });
        removeShiftMode(mBottomNav);

        MenuItem selectedItem;
        if (savedInstanceState != null)
        {
            mSelectedItem = savedInstanceState.getInt(SELECTED_ITEM, 0);
            selectedItem = mBottomNav.getMenu().findItem(mSelectedItem);
        } else
        {
            selectedItem = mBottomNav.getMenu().getItem(0);
        }
        selectFragment(selectedItem);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Permission to access the location is missing.
            //ASSUMING the main activity as a appcompatactivity
            PermissionUtils.requestPermission((AppCompatActivity) this, LOCATION_PERMISSION_REQUEST_CODE, android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else
        {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        }

        //create notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        //start the background location gathering.
        if (!Utils.isServiceRunning(this, ServiceBackgroundLocation.class))
        {
            startService(new Intent(this, ServiceBackgroundLocation.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case LOCATION_PERMISSION_REQUEST_CODE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

                } else
                {

                    Toast.makeText(this, "Location not enabled, app will not function correctly!", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putInt(SELECTED_ITEM, mSelectedItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed()
    {
        //Sets the back button to map
        MenuItem homeItem = mBottomNav.getMenu().getItem(0);
        if (mSelectedItem != homeItem.getItemId())
        {
            // select home item
            selectFragment(homeItem);
        } else
        {
            super.onBackPressed();
        }
    }

    /**
     * Removes the wierd shifting from nav bars.
     * https://stackoverflow.com/questions/40972293/remove-animation-shifting-mode-from-bottomnavigationview-android
     * @param view
     */
    private void removeShiftMode(BottomNavigationView view)
    {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try
        {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++)
            {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e)
        {
            Log.e("ERROR NO SUCH FIELD", "Unable to get shift mode field");
        } catch (IllegalAccessException e)
        {
            Log.e("ERROR ILLEGAL ALG", "Unable to change value of shift mode");
        }
    }

    private void selectFragment(MenuItem item)
    {
        Fragment frag = null;

        switch (item.getItemId())
        {
            case R.id.menu_map:
                frag = FragmentMap.newInstance();
                break;
            case R.id.menu_new_post:
                frag = new FragmentNewPost();
                break;
            case R.id.menu_feed:
                frag = FragmentFeed.newInstance();
                break;
            case R.id.menu_user:
                frag = FragmentAccount.newInstance();
                break;
        }

        // update selected item
        mSelectedItem = item.getItemId();

        // uncheck the other items.
        for (int i = 0; i < mBottomNav.getMenu().size(); i++)
        {
            MenuItem menuItem = mBottomNav.getMenu().getItem(i);
            menuItem.setChecked(menuItem.getItemId() == item.getItemId());
        }

        updateToolbarText(item.getTitle());

        if (frag != null)
        {
            Utils.switchToFragment(this, frag);
        }
    }

    private void updateToolbarText(CharSequence text)
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setTitle(text);
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Utils.cachedLoc = new LatLng(location.getLatitude(), location.getLongitude());
        locationManager.removeUpdates(this);

        createFirebaseNotificationListeners();

        //This is to stop refreshing the feed once it finds a location!
        Fragment f = Utils.getCurrentFragment(this);

        if (f instanceof FragmentFeed)
        {
            ((FragmentFeed) f).swipeLayout.setRefreshing(false);
            Utils.switchToFragment(this, FragmentFeed.newInstance());
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

    private void createFirebaseNotificationListeners()
    {
        if(Utils.cachedLoc.latitude != 0)
        {
            FirebaseApp.initializeApp(getApplicationContext());
            DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
            GeoFire geoFire = new GeoFire(geoRef);

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
                                    if(!Utils.isAppRunning(getApplicationContext(), "com.teammimosa.pupalert_android"))
                                        createNotification("New pups in your area!");
                                }
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

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody body to put in notification bar.
     */
    private void createNotification(String messageBody)
    {
        Intent intent = new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_app)
                        .setContentTitle("Pup Alert")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}