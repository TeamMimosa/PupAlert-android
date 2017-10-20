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
     * Gets a formatted timestamp.
     * @return
     */
    public static String getTimeStamp()
    {
        Date date = new Date();
        //Format is: Wed Oct 18 2017 15:32:10 GMT-0400 (EDT)
        SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");
        String dateStr = sdf.format(date);
        return dateStr;
    }
}
