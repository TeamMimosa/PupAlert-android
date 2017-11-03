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

    public FeedPost(String author, String imageKey, LatLng postLoc)
    {
        this.postedBy = author;
        this.imageKey = imageKey;
        this.postLoc = postLoc;
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

    public void setPostedBy(String author)
    {
        this.postedBy = author;
    }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public void setPostLoc(LatLng postLoc) { this.postLoc = postLoc; }
}