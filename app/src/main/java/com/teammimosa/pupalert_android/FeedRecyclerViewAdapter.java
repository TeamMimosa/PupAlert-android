package com.teammimosa.pupalert_android;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.teammimosa.pupalert_android.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedRecyclerViewAdapter extends RecyclerView.Adapter<FeedRecyclerViewAdapter.DataObjectHolder>
{
    private ArrayList<FeedPost> mDataset;
    private static MyClickListener myClickListener;
    private Context appContext;

    public static class DataObjectHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView postedBy;
        ImageView image;
        TextView location;
        TextView minutesAgo;

        public DataObjectHolder(View itemView)
        {
            super(itemView);
            postedBy = (TextView) itemView.findViewById(R.id.card_author);
            image = (ImageView) itemView.findViewById(R.id.card_image);
            location = (TextView)itemView.findViewById(R.id.card_location);
            minutesAgo = (TextView) itemView.findViewById(R.id.card_minutes_ago);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            if(myClickListener != null)
                myClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    /**
     * Add a listener for when the card is clicked.
     */
    public void setOnItemClickListener(MyClickListener myClickListener)
    {
        this.myClickListener = myClickListener;
    }

    public FeedRecyclerViewAdapter(ArrayList<FeedPost> myDataset, Context context)
    {
        mDataset = myDataset;
        appContext = context;
    }


    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_feed_card, parent, false);
        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);

        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(DataObjectHolder holder, int position)
    {
        holder.postedBy.setText(mDataset.get(position).getPostedBy());

        //get minutes ago posted
        Date curDate = new Date();
        Calendar curCal = Calendar.getInstance();
        curCal.setTime(curDate);

        Calendar postCal = Calendar.getInstance();
        try
        {
            postCal.setTime(Utils.dateFormat.parse(mDataset.get(position).getTimestamp()));
        }
        catch (ParseException e)
        {
            postCal.setTime(curDate);
            System.err.println("Could not parse out the timestamp!");
            e.printStackTrace();
        }

        long difference = curCal.getTimeInMillis() - postCal.getTimeInMillis();
        int minutes = (int) (difference/ (1000*60));

        if(minutes > 0)
            holder.minutesAgo.setText(minutes + " minutes ago");
        else
            holder.minutesAgo.setText("very recently");

        //load the image with glide
        StorageReference storRef = FirebaseStorage.getInstance().getReference().child("posts/" + mDataset.get(position).getImageKey());
        Glide.with(appContext).using(new FirebaseImageLoader()).load(storRef).into(holder.image);

        //set the cards location
        try
        {
            Geocoder gcd = new Geocoder(appContext, Locale.getDefault());
            double lat = mDataset.get(position).getPostLoc().latitude;
            double longi = mDataset.get(position).getPostLoc().longitude;
            List<Address> addresses = gcd.getFromLocation(lat, longi, 1);
            if (addresses.size() > 0)
            {
                holder.location.setText(addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea());
            }
            else
            {
                holder.location.setText("Location not found");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void addItem(FeedPost dataObj, int index)
    {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void addItem(FeedPost data)
    {
        mDataset.add(data);
        notifyItemInserted(mDataset.indexOf(data));
    }

    public void deleteItem(int index)
    {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount()
    {
        return mDataset.size();
    }

    public interface MyClickListener
    {
        public void onItemClick(int position, View v);
    }
}