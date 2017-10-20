package com.teammimosa.pupalert_android.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Just some utils for PupAlert
 * @author Domenic Portuesi
 */
public class Utils
{
    /**
     * Gets a formatted timestamp for pushing to database.
     * @return
     */
    public static String getTimeStampForDatabase()
    {
        Date date = new Date();
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
        SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");
        String dateStr = sdf.format(date);
        return dateStr;
    }

    /**
     * Gets a formatted timestamp for displaying on card view.
     * @return
     */
    public static String getTimeStampForCardView()
    {
        Date date = new Date();
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss)");
        String dateStr = sdf.format(date);
        return dateStr;
    }


}
