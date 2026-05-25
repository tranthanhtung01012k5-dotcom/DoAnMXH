// FollowAdapter.java
package com.example.doanmxh.ProfilePage;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
public class FollowAdapter
        extends RecyclerView.Adapter<FollowAdapter.ViewHolder> {

    private final List<FollowModel> list;
    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private final String currentUid =
            FirebaseAuth.getInstance().getCurrentUser().getUid();
    public FollowAdapter(List<FollowModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        PostModel post = new PostModel();
        FollowModel model = list.get(position);

        holder.txtUsername.setText(model.getUsername());
        holder.txtName.setText(model.getName());

        Glide.with(holder.itemView.getContext())
                .load(
                        model.getAvatar() != null
                                && !model.getAvatar().isEmpty()
                                ? model.getAvatar()
                                : R.drawable.ic_placeholder_avatar
                )
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar)
                .circleCrop()
                .into(holder.imgAvatar);
        // Mở trang cá nhân
        View.OnClickListener openProfileListener = v -> {

            Intent intent = new Intent(
                    holder.itemView.getContext(),
                    UserProfileActivity.class
            );

            intent.putExtra(
                    "user_uid",
                    model.getUid()
            );

            holder.itemView.getContext()
                    .startActivity(intent);
        };

// Click avatar
        holder.imgAvatar.setOnClickListener(
                openProfileListener
        );

// Click username
        holder.txtUsername.setOnClickListener(
                openProfileListener
        );

// Click tên
        holder.txtName.setOnClickListener(
                openProfileListener
        );

        // Không hiện nút follow với chính mình
        if (model.getUid().equals(currentUid)) {

            holder.btnFollow.setVisibility(View.GONE);

            return;
        }

        holder.btnFollow.setVisibility(View.VISIBLE);

        // Check follow
        db.collection("nguoi_dung")
                .document(currentUid)
                .collection("nguoi_dang_theo_doi")
                .document(model.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    boolean isFollowing =
                            documentSnapshot.exists();

                    if (isFollowing) {

                        holder.btnFollow.setText("Đang theo dõi");

                        holder.btnFollow.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        Color.parseColor("#1E1E1E")
                                )
                        );

                        holder.btnFollow.setTextColor(Color.WHITE);

                    } else {

                        holder.btnFollow.setText("Theo dõi");

                        holder.btnFollow.setBackgroundTintList(
                                ColorStateList.valueOf(Color.WHITE)
                        );

                        holder.btnFollow.setTextColor(Color.BLACK);
                    }

                    // CLICK FOLLOW
                    holder.btnFollow.setOnClickListener(v -> {

                        if (!isFollowing) {

                            followUser(model, holder);
                        }
                    });

                    // LONG CLICK UNFOLLOW
                    holder.btnFollow.setOnLongClickListener(v -> {

                        if (isFollowing) {

                            new android.app.AlertDialog.Builder(
                                    holder.itemView.getContext()
                            )
                                    .setTitle("Hủy theo dõi")
                                    .setMessage(
                                            "Bạn có muốn hủy theo dõi @"
                                                    + model.getUsername()
                                                    + " ?"
                                    )
                                    .setPositiveButton(
                                            "Hủy theo dõi",
                                            (dialog, which) -> {

                                                unfollowUser(model, holder);
                                            }
                                    )
                                    .setNegativeButton(
                                            "Đóng",
                                            null
                                    )
                                    .show();
                        }

                        return true;
                    });
                });
    }
    private void unfollowUser(
            FollowModel model,
            ViewHolder holder
    ) {

        db.collection("nguoi_dung")
                .document(currentUid)
                .collection("nguoi_dang_theo_doi")
                .document(model.getUid())
                .delete()

                .addOnSuccessListener(unused -> {

                    // xóa khỏi follower của người kia
                    db.collection("nguoi_dung")
                            .document(model.getUid())
                            .collection("nguoi_theo_doi")
                            .document(currentUid)
                            .delete();
//                    db.collection("nguoi_dung")
//                            .document(currentUid)
//                            .collection("nguoi_theo_doi")
//                            .document(model.getUid())
//                            .delete();

                    // cập nhật trạng thái local
                    if (model != null) {
                        model.setFollowing(false);
                    }

                    // current user giảm số đang theo dõi
                    db.collection("nguoi_dung")
                            .document(currentUid)
                            .update(
                                    "so_nguoi_dang_theo_doi",
                                    FieldValue.increment(-1)
                            );

                    // người bị unfollow giảm follower
                    db.collection("nguoi_dung")
                            .document(model.getUid())
                            .update(
                                    "so_nguoi_theo_doi",
                                    FieldValue.increment(-1)
                            );

                    // cập nhật UI
                    holder.btnFollow.setText("Theo dõi");

                    holder.btnFollow.setBackgroundTintList(
                            ColorStateList.valueOf(Color.WHITE)
                    );

                    holder.btnFollow.setTextColor(Color.BLACK);
                });
    }
    private void followUser(
            FollowModel model,
            ViewHolder holder
    ) {

        // dữ liệu follow
        Map<String, Object> followData = new HashMap<>();
        followData.put("nguoi_dung_id", currentUid);
        followData.put("ngay_theo_doi", new Date());

        // currentUid follow model.getUid()
        db.collection("nguoi_dung")
                .document(currentUid)
                .collection("nguoi_dang_theo_doi")
                .document(model.getUid())
                .set(followData)

                .addOnSuccessListener(unused -> {

                    // thêm vào danh sách follower của người kia
                    db.collection("nguoi_dung")
                            .document(model.getUid())
                            .collection("nguoi_theo_doi")
                            .document(currentUid)
                            .set(followData);

                    // cập nhật PostModel
                    if (model != null) {
                        model.setFollowing(true);
                    }

                    // current user tăng số đang theo dõi
                    db.collection("nguoi_dung")
                            .document(currentUid)
                            .update(
                                    "so_nguoi_dang_theo_doi",
                                    FieldValue.increment(1)
                            );

                    // người được follow tăng follower
                    db.collection("nguoi_dung")
                            .document(model.getUid())
                            .update(
                                    "so_nguoi_theo_doi",
                                    FieldValue.increment(1)
                            );

                    // cập nhật UI
                    holder.btnFollow.setText("Đang theo dõi");

                    holder.btnFollow.setBackgroundTintList(
                            ColorStateList.valueOf(
                                    Color.parseColor("#1E1E1E")
                            )
                    );

                    holder.btnFollow.setTextColor(Color.WHITE);
                });
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView txtUsername, txtName;
        Button btnFollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtName = itemView.findViewById(R.id.txtName);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
    }
}