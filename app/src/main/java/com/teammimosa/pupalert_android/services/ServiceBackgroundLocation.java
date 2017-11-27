package com.teammimosa.pupalert_android.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.View;

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
import com.teammimosa.pupalert_android.activity.ActivityMain;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.fragment.FeedPost;
import com.teammimosa.pupalert_android.fragment.FeedRecyclerViewAdapter;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * This service runs in the background when the app is closed to updated the cur loc every 30 mins or so. Used for checking notifications
 */
public class ServiceBackgroundLocation extends Service
{
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = (1000) * 60 * 30;
    private static final float LOCATION_DISTANCE = 1000;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            mLastLocation = new Location(provider);
            Utils.cachedLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            //filteredCreateNotification();
        }

        @Override
        public void onLocationChanged(Location location)
        {
            mLastLocation.set(location);
            Utils.cachedLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            filteredCreateNotification();
        }

        @Override
        public void onProviderDisabled(String provider)
        {

        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }

        private void filteredCreateNotification()
        {
            //createNotification("test body");
            if(mLastLocation.getLatitude() != 0.0)
            {
                FirebaseApp.initializeApp(getApplicationContext());
                DatabaseReference geoRef = FirebaseDatabase.getInstance().getReference("geofire");
                GeoFire geoFire = new GeoFire(geoRef);

                //GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(curLoc.latitude, curLoc.longitude), 10);
                GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), Utils.FEED_LOCATION_RADIUS);
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

    }

    LocationListener[] mLocationListeners;

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        mLocationListeners = new LocationListener[]{
                new LocationListener(LocationManager.GPS_PROVIDER),
                new LocationListener(LocationManager.NETWORK_PROVIDER)
        };

        initializeLocationManager();
        try
        {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex)
        {
            ex.printStackTrace();
        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
        try
        {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        }
        catch (java.lang.SecurityException ex)
        {
            ex.printStackTrace();
        } catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mLocationManager != null)
        {
            for (int i = 0; i < mLocationListeners.length; i++)
            {
                try
                {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initializeLocationManager()
    {
        if (mLocationManager == null)
        {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
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

    /*
    private int getNotificationIcon(int entry)
    {
        if (entry.targetSdk >= Build.VERSION_CODES.LOLLIPOP)
        {
            entry.icon.setColorFilter(mContext.getResources().getColor(android.R.color.white));
        }
        else
        {
            entry.icon.setColorFilter(null);
        }
    }
    */
}