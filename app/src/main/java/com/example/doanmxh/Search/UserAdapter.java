package com.example.doanmxh.Search;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Notifications.NotificationsFragment;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    public UserAdapter() {
    }

    // 🔥 quan trọng: dùng khi search Firebase xong
    public void setList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow, parent, false);

        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String myUid = auth.getCurrentUser().getUid();
        User user = userList.get(position);

        holder.txtUsername.setText(user.getUsername());
        holder.txtName.setText(user.getFullname());

        if (user.isFollowed() || user.getUid().equals(myUid)) {
            holder.btnFollow.setVisibility(View.GONE);
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            holder.btnFollow.setText("Follow");
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
        Glide.with(holder.imgAvatar.getContext())
                .load(user.getAvatar())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.imgAvatar);
        holder.btnFollow.setOnClickListener( v -> {
            Map<String,Object> u1 = new HashMap<>();
//            u1.put("anh_dai_dien",user.getAvatar());
//            u1.put("ho_va_ten",user.getFullname());
//            u1.put("ten_dang_nhap",user.getUsername());
            u1.put("nguoi_dung_id",user.getUid());
            u1.put("ngay_theo_doi", Timestamp.now());
            db.collection("nguoi_dung").document(myUid)
                    .collection("nguoi_dang_theo_doi")
                    .document(user.getUid())
                    .set(u1)
                    .addOnSuccessListener( abc-> {
                            user.setFollowed(true);
                        notifyItemChanged(holder.getAdapterPosition());
                        NotificationsFragment.sendFollowNotification(user.getUid(),myUid);
                        Toast.makeText(holder.itemView.getContext(), "Follow thành công",Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView txtUsername;
        TextView txtName;
        Button btnFollow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtName = itemView.findViewById(R.id.txtName);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
    }
}