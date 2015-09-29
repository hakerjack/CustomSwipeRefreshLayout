package com.hakerjack.experiments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by kj on 9/23/15.
 */
public class RecyclerVIewAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private ArrayList<String> mIds;

    public RecyclerVIewAdapter() {
        mIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            mIds.add("hi there " + i);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView = View.inflate(parent.getContext(), R.layout.my_viewholder, null);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder myViewHolder, int i) {
        myViewHolder.setText(mIds.get(i));
        if (i == 0) {
            myViewHolder.setHeaderHeight();
        } else {
            myViewHolder.setNormalHeight();
        }
    }

    @Override
    public int getItemCount() {
        return 50;
    }
}
