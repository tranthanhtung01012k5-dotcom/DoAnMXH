package com.example.doanmxh.Search;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Notifications.NotificationsFragment;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatedProfileAdapter
        extends RecyclerView.Adapter<RelatedProfileAdapter.ViewHolder> {

    private ArrayList<User> users;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public RelatedProfileAdapter(ArrayList<User> users) {
        this.users = users;
    }

    public void setListWithoutCheck(List<User> newList) {
        this.users = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // Kiểm tra follow cho toàn bộ danh sách
    public void checkFollowStatuses() {
        if (auth.getCurrentUser() == null) return;
        String myUid = auth.getCurrentUser().getUid();

        for (int i = 0; i < users.size(); i++) {
            final int index = i;
            User user = users.get(i);

            if (user.getUid() == null || user.getUid().equals(myUid)) continue;

            db.collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        user.setFollowed(doc.exists());
                        notifyItemChanged(index);
                    });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName;
        TextView tvUsername;
        MaterialButton btnFollow;

        public ViewHolder(View itemView) {
            super(itemView);
            ivAvatar   = itemView.findViewById(R.id.ivAvatar);
            tvName     = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnFollow  = itemView.findViewById(R.id.btnFollow);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_related_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (auth.getCurrentUser() == null) return;
        String myUid = auth.getCurrentUser().getUid();
        User user = users.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(user.getFullname());
        holder.tvUsername.setText("@" + user.getUsername());

        Glide.with(ctx)
                .load(user.getAvatar())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.ivAvatar);

        // Ẩn nút nếu là chính mình
        if (user.getUid().equals(myUid)) {
            holder.btnFollow.setVisibility(View.GONE);
            holder.btnFollow.setOnClickListener(null);
            holder.btnFollow.setOnLongClickListener(null);

        } else if (user.isFollowed()) {
            // Đã follow → hiện "Đang theo dõi", nhấn lâu để hủy
            holder.btnFollow.setVisibility(View.VISIBLE);
            holder.btnFollow.setText("Đang theo dõi");
            holder.btnFollow.setTextColor(
                    ContextCompat.getColor(ctx, R.color.button_primary_text));
            holder.btnFollow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.button_primary_bg)));
            holder.btnFollow.setOnClickListener(null);
            holder.btnFollow.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(ctx)
                        .setTitle("Hủy theo dõi")
                        .setMessage("Bạn có muốn hủy theo dõi " + user.getFullname() + " không?")
                        .setPositiveButton("Hủy theo dõi", (dialog, which) -> {
                            db.collection("nguoi_dung").document(myUid)
                                    .collection("nguoi_dang_theo_doi")
                                    .document(user.getUid())
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        db.collection("nguoi_dung").document(user.getUid())
                                                .collection("nguoi_theo_doi")
                                                .document(myUid)
                                                .delete();
                                        db.collection("nguoi_dung").document(myUid)
                                                .update("so_nguoi_dang_theo_doi", FieldValue.increment(-1));
                                        db.collection("nguoi_dung").document(user.getUid())
                                                .update("so_nguoi_theo_doi", FieldValue.increment(-1));
                                        user.setFollowed(false);
                                        notifyItemChanged(holder.getAdapterPosition());
                                        Toast.makeText(ctx, "Đã hủy theo dõi", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(ctx, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
                return true;
            });

        } else {
            // Chưa follow → hiện "Theo dõi"
            holder.btnFollow.setVisibility(View.VISIBLE);
            holder.btnFollow.setText("Theo dõi");
            holder.btnFollow.setTextColor(
                    ContextCompat.getColor(ctx, R.color.button_primary_text));
            holder.btnFollow.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(ctx, R.color.button_primary_bg)));
            holder.btnFollow.setOnLongClickListener(null);
            holder.btnFollow.setOnClickListener(v -> {
                Map<String, Object> followingData = new HashMap<>();
                followingData.put("nguoi_dung_id", user.getUid());
                followingData.put("ngay_theo_doi", Timestamp.now());

                db.collection("nguoi_dung").document(myUid)
                        .collection("nguoi_dang_theo_doi")
                        .document(user.getUid())
                        .set(followingData)
                        .addOnSuccessListener(unused -> {
                            Map<String, Object> followerData = new HashMap<>();
                            followerData.put("nguoi_dung_id", myUid);
                            followerData.put("ngay_theo_doi", new Date());
                            db.collection("nguoi_dung").document(user.getUid())
                                    .collection("nguoi_theo_doi")
                                    .document(myUid)
                                    .set(followerData);
                            db.collection("nguoi_dung").document(myUid)
                                    .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));
                            db.collection("nguoi_dung").document(user.getUid())
                                    .update("so_nguoi_theo_doi", FieldValue.increment(1));
                            NotificationsFragment.sendFollowNotification(user.getUid(), myUid);
                            user.setFollowed(true);
                            notifyItemChanged(holder.getAdapterPosition());
                            Toast.makeText(ctx, "Đã theo dõi", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(ctx, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        }

        // Click vào item → mở UserProfileActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, UserProfileActivity.class);
            intent.putExtra("user_uid", user.getUid());
            ctx.startActivity(intent);
        });
    }
    // Thêm vào UserAdapter
    public void setListWithoutCheck(ArrayList<User> newList) {
        this.users= newList;
        notifyDataSetChanged(); // không gọi checkFollowStatuses()
    }
    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }
}