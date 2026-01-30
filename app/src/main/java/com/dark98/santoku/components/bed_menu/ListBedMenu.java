package com.dark98.santoku.components.bed_menu;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.dark98.santoku.Santoku;
import com.dark98.santoku.fragment.BedFragment;
import com.dark98.santoku.recycler.SimpleRecyclerAdapter;
import com.dark98.santoku.recycler.SimpleRecyclerItem;
import com.dark98.santoku.utils.ViewUtils;

public abstract class ListBedMenu extends BedMenu {
    protected BedFragment fragment;
    protected RecyclerView recyclerView;
    protected SimpleRecyclerAdapter adapter;

    @Override
    public void onSetBed(BedFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public View onCreateView(Context ctx, boolean portrait) {
        recyclerView = new RecyclerView(ctx);
        recyclerView.setLayoutManager(new LinearLayoutManager(ctx, portrait ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(null);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);
        adapter = new SimpleRecyclerAdapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RecyclerView.ViewHolder vh = super.onCreateViewHolder(parent, viewType);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) vh.itemView.getLayoutParams();
                if (!portrait && params != null) {
                    params.rightMargin = ViewUtils.dp(6);
                    params.bottomMargin = 0;
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                return vh;
            }
        };
        adapter.setItems(onCreateItems(portrait));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                if (parent.getChildViewHolder(view).getAdapterPosition() == adapter.getItemCount() - 1) {
                    if (portrait) {
                        outRect.right = ViewUtils.dp(6);
                    } else {
                        outRect.bottom = ViewUtils.dp(6);
                    }
                }
            }
        });
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    @Override
    public void onViewCreated(View v) {
        super.onViewCreated(v);
        Santoku.EVENT_BUS.registerListener(ListBedMenu.this);
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        Santoku.EVENT_BUS.unregisterListener(ListBedMenu.this);
    }

    protected abstract List<SimpleRecyclerItem> onCreateItems(boolean portrait);
}
