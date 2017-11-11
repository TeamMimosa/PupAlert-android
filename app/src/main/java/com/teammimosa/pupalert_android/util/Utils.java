package com.teammimosa.pupalert_android.util;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.teammimosa.pupalert_android.AccountFragment;
import com.teammimosa.pupalert_android.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Just some utils for PupAlert
 * @author Domenic Portuesi
 */
public class Utils
{
    public static LatLng cachedLoc = new LatLng(0,0);

    /**
     * Gets a formatted timestamp for pushing to database.
     * @return
     */
    public static String getTimeStampForDatabase()
    {
        Date date = new Date();
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
        //SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");

        //Format is: 2017-11-26 15:00
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(date);
        return dateStr;
    }

    /**
     * Gets a formatted timestamp for displaying on card view.
     * @return
     */
    public static String getTimeStampForCardView(Date date)
    {
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
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

    public static void switchToFragment(FragmentActivity activity, Fragment frag)
    {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, frag, frag.getTag());
        ft.commit();
    }

}
