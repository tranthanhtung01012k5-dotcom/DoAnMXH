package com.example.doanmxh.Message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SearchMessageAdapter extends RecyclerView.Adapter<SearchMessageAdapter.Holder> {

    public interface OnClick {
        void onClick(SearchItem item);
    }

    private List<SearchItem> list;
    private OnClick onClick;

    public SearchMessageAdapter(List<SearchItem> list, OnClick onClick) {
        this.list = list;
        this.onClick = onClick;
    }

    class Holder extends RecyclerView.ViewHolder {

        TextView txtContent, txtTime;

        public Holder(View v) {
            super(v);
            txtContent = v.findViewById(R.id.txtContent);
            txtTime = v.findViewById(R.id.txtTime);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_message, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {

        SearchItem item = list.get(i);

        h.txtContent.setText(item.content);
        h.txtTime.setText(
                new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                        .format(item.getTimestamp())
        );

        h.itemView.setOnClickListener(v -> onClick.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
