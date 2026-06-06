package com.example.doanmxh.Message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PinnedMessageAdapter
        extends RecyclerView.Adapter<PinnedMessageAdapter.Holder> {

    private final List<Map<String, Object>> data;
    private final OnPinClick listener;

    public interface OnPinClick {
        void onClick(Map<String, Object> pin);
    }

    public PinnedMessageAdapter(
            List<Map<String, Object>> data,
            OnPinClick listener) {

        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_pinned_message,
                        parent,
                        false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {

        Map<String, Object> pin = data.get(position);

        String content =
                (String) pin.get("noi_dung");

        holder.txtPinnedContent.setText(
                content != null
                        ? content
                        : "[Hình ảnh]"
        );

        Timestamp timestamp =
                (Timestamp) pin.get("thoi_gian_ghim");

        if (timestamp != null) {

            String time =
                    new SimpleDateFormat(
                            "HH:mm dd/MM",
                            Locale.getDefault())
                            .format(timestamp.toDate());

            holder.txtPinnedTime.setText(time);
        }

        holder.itemView.setOnClickListener(
                v -> listener.onClick(pin)
        );

        holder.btnRemovePin.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView txtPinnedContent;
        TextView txtPinnedTime;
        ImageView btnRemovePin;

        Holder(View itemView) {
            super(itemView);

            txtPinnedContent =
                    itemView.findViewById(
                            R.id.txtPinnedContent);

            txtPinnedTime =
                    itemView.findViewById(
                            R.id.txtPinnedTime);

            btnRemovePin =
                    itemView.findViewById(
                            R.id.btnRemovePin);
        }
    }
}