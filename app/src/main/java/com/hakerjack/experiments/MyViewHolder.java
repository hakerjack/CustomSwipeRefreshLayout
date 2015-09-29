package com.hakerjack.experiments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by kj on 9/23/15.
 */
public class MyViewHolder extends RecyclerView.ViewHolder {
    private TextView textView;


    public MyViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.text);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void setHeaderHeight() {
        textView.setHeight(textView.getContext().getResources().getDimensionPixelSize(R.dimen.first_row_height));
    }

    public void setNormalHeight() {
        ViewGroup.LayoutParams params = textView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        textView.setLayoutParams(params);
    }
}
