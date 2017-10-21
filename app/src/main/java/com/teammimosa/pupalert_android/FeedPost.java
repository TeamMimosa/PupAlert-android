package com.teammimosa.pupalert_android;

import android.net.Uri;

/**
 * A "card" object for the feed.
 *
 * @author Domenic Portuesi
 */
public class FeedPost
{
    private String postedBy;
    private String image;

    public FeedPost(String author, String imageKey)
    {
        this.postedBy = author;
        this.image = imageKey;
    }

    public String getPostedBy()
    {
        return postedBy;
    }

    public String getImageKey()
    {
        return image;
    }

    public void setPostedBy(String author)
    {
        this.postedBy = author;
    }
    public void setImage(String image) { this.image = image; }
}