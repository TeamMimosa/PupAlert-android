package com.teammimosa.pupalert_android;

import com.google.android.gms.maps.model.LatLng;

/**
 * A "card" object for the feed.
 *
 * @author Domenic Portuesi
 */
public class FeedPost
{
    private String postedBy;
    private String imageKey;
    private LatLng postLoc;
    private String timestamp;

    public FeedPost(String author, String imageKey, LatLng postLoc, String timestamp)
    {
        this.postedBy = author;
        this.imageKey = imageKey;
        this.postLoc = postLoc;
        this.timestamp = timestamp;
    }

    public String getPostedBy()
    {
        return postedBy;
    }
    public String getImageKey()
    {
        return imageKey;
    }
    public LatLng getPostLoc()
    {
        return postLoc;
    }
    public String getTimestamp() { return timestamp; }

    public void setPostedBy(String author)
    {
        this.postedBy = author;
    }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public void setPostLoc(LatLng postLoc) { this.postLoc = postLoc; }
    public void setTimestamp(String t) {this.timestamp = t; }
}