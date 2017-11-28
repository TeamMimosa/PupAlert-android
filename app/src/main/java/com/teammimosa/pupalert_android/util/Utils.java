package com.teammimosa.pupalert_android.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.services.ServiceBackgroundLocation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Just some utils for PupAlert
 * @author Domenic Portuesi
 */
public class Utils
{
    public static int FEED_LOCATION_RADIUS = 10; //in km
    public static LatLng cachedLoc = new LatLng(0,0);

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Gets a formatted timestamp for pushing to database.
     * @return
     */
    public static String getTimeStampForDatabase()
    {
        Date date = new Date();
        //the old format
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
        //SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");

        //Format is: 2017-11-26 15:00:01
        String dateStr = dateFormat.format(date);
        return dateStr;
    }

    /**
     * Gets a formatted timestamp for displaying on card view.
     * @return
     */
    public static String getTimeStampForCardView(Date date)
    {
        //old Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)

        //Format is: 1:24:77
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss)");
        String dateStr = sdf.format(date);
        return dateStr;
    }

    public static LatLngBounds toBounds(LatLng center, double radiusInMeters)
    {
        double distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }

    /**
     * Swaps the the fragment of activity to Frag.
     * @param activity
     * @param frag
     */
    public static void switchToFragment(FragmentActivity activity, Fragment frag)
    {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, frag, frag.getTag());
        ft.commit();
    }

    /**
     * Gets the current fragment in use (if there is only one), otherwise returns null.
     * @param activity
     * @return
     */
    public static Fragment getCurrentFragment(FragmentActivity activity)
    {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        int stackCount = fragmentManager.getBackStackEntryCount();
        if( fragmentManager.getFragments() != null ) return fragmentManager.getFragments().get( stackCount > 0 ? stackCount-1 : stackCount );
        else return null;
    }

    /**
     * Checks if the service class is currenlty running.
     * https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
     * @param serviceClass
     * @return
     */
    public static boolean isServiceRunning(Activity mainActivity, Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean listContains(ArrayList<String> list, String value)
    {
        for(String str: list)
        {
            if(str.trim().contains(value))
                return true;
        }
        return false;
    }

    public static boolean isAppRunning(final Context context, final String packageName)
    {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null)
        {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
