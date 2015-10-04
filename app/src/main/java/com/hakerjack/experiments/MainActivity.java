package com.hakerjack.experiments;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class MainActivity extends Activity implements CustomSwipeRefreshLayout.OffsetViewListener {
    private CustomSwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private int mRecyclerViewOriginalTop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefreshLayout = (CustomSwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(new CustomSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.setRefreshing(false);
                        Toast.makeText(mRefreshLayout.getContext(), "refresh complete", Toast.LENGTH_SHORT).show();
                    }
                }, 1500);

            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerViewOriginalTop = mRecyclerView.getTop();
        RecyclerVIewAdapter adapter = new RecyclerVIewAdapter();
        mRecyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.clearOnScrollListeners();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager.findFirstCompletelyVisibleItemPosition() != 0) {
                    mRefreshLayout.setEnabled(false);
                } else {
                    mRefreshLayout.setEnabled(true);
                }
            }
        });
        mRefreshLayout.setOffsetViewListener(this);
    }

    @Override
    public void onOffset(int offset) {
        if (mRecyclerView.getTop() + offset >= mRecyclerViewOriginalTop) {
            mRecyclerView.offsetTopAndBottom(offset);
        }
    }
}
