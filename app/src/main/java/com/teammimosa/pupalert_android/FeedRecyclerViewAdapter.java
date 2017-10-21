package com.teammimosa.pupalert_android;

import android.content.Context;
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

import java.util.ArrayList;

public class FeedRecyclerViewAdapter extends RecyclerView.Adapter<FeedRecyclerViewAdapter.DataObjectHolder>
{
    private ArrayList<FeedPost> mDataset;
    private static MyClickListener myClickListener;
    private Context appContext;

    public static class DataObjectHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView postedBy;
        ImageView image;

        public DataObjectHolder(View itemView)
        {
            super(itemView);
            postedBy = (TextView) itemView.findViewById(R.id.card_author);
            image = (ImageView) itemView.findViewById(R.id.card_image);
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

        //load the image with glide
        StorageReference storRef = FirebaseStorage.getInstance().getReference().child("posts/" + mDataset.get(position).getImageKey());
        Glide.with(appContext).using(new FirebaseImageLoader()).load(storRef).into(holder.image);
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