package com.example.doanmxh.Message;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;

import java.util.List;

public class SugestionAdapter extends RecyclerView.Adapter<SugestionAdapter.ViewHolder> {

    private Context context;
    private List<SugestionModel> list;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(SugestionModel user);
    }

    public SugestionAdapter(Context context,
                            List<SugestionModel> list,
                            OnSuggestionClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SugestionModel user = list.get(position);

        holder.txtHoVaTen.setText(user.getHoVaTen());
        holder.txtTenDangNhap.setText( user.getTenDangNhap());

        Glide.with(context)
                .load(user.getAnhDaiDien())
                .placeholder(R.drawable.ic_person_outline_24)
                .into(holder.imgAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView txtHoVaTen;
        TextView txtTenDangNhap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgAvatar = itemView.findViewById(R.id.ivAvatar);
            txtHoVaTen = itemView.findViewById(R.id.tvFullName);
            txtTenDangNhap = itemView.findViewById(R.id.tvUsername);
        }
    }
}