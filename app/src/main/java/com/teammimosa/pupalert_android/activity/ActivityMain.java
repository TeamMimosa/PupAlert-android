package com.teammimosa.pupalert_android.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.fragment.FragmentAccount;
import com.teammimosa.pupalert_android.fragment.FragmentFeed;
import com.teammimosa.pupalert_android.fragment.FragmentMap;
import com.teammimosa.pupalert_android.fragment.FragmentNewPost;
import com.teammimosa.pupalert_android.services.ServiceBackgroundLocation;
import com.teammimosa.pupalert_android.util.PermissionUtils;
import com.teammimosa.pupalert_android.util.Utils;

import java.lang.reflect.Field;

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
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        //start the background location gathering.
        startService(new Intent(this, ServiceBackgroundLocation.class));
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
}