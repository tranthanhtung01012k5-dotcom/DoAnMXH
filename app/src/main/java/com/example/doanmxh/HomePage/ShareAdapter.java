package com.example.doanmxh.HomePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.doanmxh.R;

import java.util.List;

public class ShareAdapter extends BaseAdapter {

    private Context context;
    private List<ShareItem> list;

    public ShareAdapter(Context context, List<ShareItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_share, parent, false);
        }

        ImageView img = convertView.findViewById(R.id.imgIcon);
        TextView txt = convertView.findViewById(R.id.txtName);

        ShareItem item = list.get(i);

        img.setImageResource(item.icon);
        txt.setText(item.title);

        return convertView;
    }
}