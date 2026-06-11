package com.example.doanmxh.Search;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RelatedProfileAdapter
        extends RecyclerView.Adapter<RelatedProfileAdapter.ViewHolder> {

    private final ArrayList<User> users;

    public RelatedProfileAdapter(ArrayList<User> users) {
        this.users = users;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ShapeableImageView ivAvatar;
        TextView tvName;
        TextView tvUsername;
        MaterialButton btnFollow;

        public ViewHolder(View itemView) {
            super(itemView);

            ivAvatar =
                    itemView.findViewById(
                            R.id.ivAvatar);

            tvName =
                    itemView.findViewById(
                            R.id.tvName);

            tvUsername =
                    itemView.findViewById(
                            R.id.tvUsername);

            btnFollow =
                    itemView.findViewById(
                            R.id.btnFollow);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(
                                parent.getContext())
                        .inflate(
                                R.layout.item_related_profile,
                                parent,
                                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            ViewHolder holder,
            int position) {
        String myUid = FirebaseAuth.getInstance().getUid();
        User user = users.get(position);

        holder.tvName.setText(
                user.getFullname()
        );

        holder.tvUsername.setText(
                "@" + user.getUsername()
        );
        if (user.isFollowed() || user.getUid().equals(myUid)) {
            holder.btnFollow.setVisibility(View.VISIBLE);
            holder.btnFollow.setText("Đang theo dõi");
            holder.btnFollow.setTextColor(
                    holder.btnFollow.getResources()
                            .getColor(R.color.button_primary_text)
            );
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            holder.btnFollow.setText("Theo dõi");
            holder.btnFollow.setTextColor(
                    holder.btnFollow.getResources()
                            .getColor(R.color.button_primary_text)
            );
            holder.btnFollow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    holder.itemView.getContext(),
                                    R.color.button_primary_bg
                            )
                    )
            );        }
        Glide.with(holder.itemView.getContext()).load(user.getAvatar())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.ivAvatar);

        holder.btnFollow.setOnClickListener(v -> {

            Map<String,Object> u1 = new HashMap<>();
//            u1.put("anh_dai_dien",user.getAvatar());
//            u1.put("ho_va_ten",user.getFullname());
//            u1.put("ten_dang_nhap",user.getUsername());
            u1.put("nguoi_dung_id",user.getUid());
            u1.put("ngay_theo_doi", Timestamp.now());
            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi")
                    .document(user.getUid())
                    .set(u1)
                    .addOnSuccessListener( abc-> {
                        user.setFollowed(true);
                        notifyItemChanged(holder.getAdapterPosition());

                        Toast.makeText(holder.itemView.getContext(), "Follow thành công",Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}