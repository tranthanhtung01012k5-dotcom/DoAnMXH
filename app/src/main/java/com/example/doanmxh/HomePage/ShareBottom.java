package com.example.doanmxh.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareBottom extends BottomSheetDialogFragment {

    private String shareText = "";
    private String postId = "";   // thêm dòng này
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String myUid;

    public static ShareBottom newInstance(String text, String postId) {
        ShareBottom fragment = new ShareBottom();
        Bundle args = new Bundle();
        args.putString("share_text", text);
        args.putString("post_id", postId);   // thêm dòng này
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shareText = getArguments().getString("share_text");
            postId = getArguments().getString("post_id", "");  // thêm dòng này
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_share, container, false);

        GridView gridShare = view.findViewById(R.id.gridShare);

        List<ShareItem> list = new ArrayList<>();
        list.add(new ShareItem("Facebook", R.drawable.ic_fb));
        list.add(new ShareItem("Messenger", R.drawable.ic_messenger));
        list.add(new ShareItem("Instagram", R.drawable.ic_instagram));
        list.add(new ShareItem("Copy", R.drawable.ic_copy_link));
        list.add(new ShareItem("More", R.drawable.ic_more_horiz_24));

        ShareAdapter adapter = new ShareAdapter(requireContext(), list);
        gridShare.setAdapter(adapter);

        // 👉 thêm dòng này
        loadRecentUsers(view);

        gridShare.setOnItemClickListener((parent, view1, position, id) -> {
            ShareItem item = list.get(position);

            switch (item.title) {

                case "Copy":
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) requireContext()
                                    .getSystemService(requireContext().CLIPBOARD_SERVICE);

                    android.content.ClipData clip =
                            android.content.ClipData.newPlainText("share", shareText);

                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(getContext(), "Đã sao chép", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT,
                            shareText != null ? shareText : "");
                    startActivity(Intent.createChooser(intent, "Chia sẻ qua"));
                    dismiss();
                    break;
            }
        });

        return view;
    }
    public interface OnShareDoneListener {
        void onShareDone();
    }

    private OnShareDoneListener shareListener;

    public void setOnShareDoneListener(OnShareDoneListener listener) {
        this.shareListener = listener;
    }
    private void loadRecentUsers(View view) {
        LinearLayout layout = view.findViewById(R.id.layoutRecentUsers);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (myUid == null) return;

        // Lấy các cuộc trò chuyện có myUid trong thanh_vien
        db.collection("cuoc_tro_chuyen")
                .whereArrayContains("thanh_vien", myUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (var doc : snapshot.getDocuments()) {

                        // Lấy uid người kia từ danh sách thanh_vien
                        List<String> thanhVien = (List<String>) doc.get("thanh_vien");
                        if (thanhVien == null) continue;

                        String otherUid = null;
                        for (String uid : thanhVien) {
                            if (!uid.equals(myUid)) { otherUid = uid; break; }
                        }
                        if (otherUid == null) continue;

                        String finalOtherUid = otherUid;

                        // Load thông tin user từ nguoi_dung
                        db.collection("nguoi_dung").document(finalOtherUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (!userDoc.exists()) return;

                                    String targetUid =userDoc.getString("uid");
                                    String ten = userDoc.getString("ten_dang_nhap");
                                    String anh = userDoc.getString("anh_dai_dien");

                                    View item = inflater.inflate(R.layout.item_share, layout, false);
                                    ImageView avatar = item.findViewById(R.id.imgIcon);
                                    TextView name = item.findViewById(R.id.txtName);

                                    name.setText(ten);

                                    if (anh != null && !anh.isEmpty()) {
                                        Glide.with(requireContext()).load(anh).placeholder(R.drawable.ic_person_outline_24).error(R.drawable.ic_person_outline_24).circleCrop().into(avatar);
                                    }

                                    item.setOnClickListener(click -> {
                                        db.collection("cuoc_tro_chuyen")
                                                .whereArrayContains("thanh_vien", myUid)
                                                .get()
                                                .addOnSuccessListener(convSnapshot -> {  // đổi tên ở đây
                                                    String conversationId = null;

                                                    for (var convDoc : convSnapshot.getDocuments()) {  // đổi tên ở đây
                                                        List<String> tv = (List<String>) convDoc.get("thanh_vien");
                                                        if (tv != null && tv.contains(finalOtherUid)) {
                                                            conversationId = convDoc.getId();
                                                            break;
                                                        }
                                                    }

                                                    String finalConvId = conversationId;
                                                    if (finalConvId != null) {
                                                        sendShareMessage(finalConvId,finalOtherUid);
                                                    } else {
                                                        Map<String, Object> newConv = new HashMap<>();
                                                        newConv.put("thanh_vien", Arrays.asList(myUid, finalOtherUid));
                                                        newConv.put("ngay_tao", new Date());
                                                        sendShareMessage(finalConvId, finalOtherUid);
// và
                                                        db.collection("cuoc_tro_chuyen").add(newConv)
                                                                .addOnSuccessListener(ref -> sendShareMessage(ref.getId(), finalOtherUid));
                                                    }
                                                });
//                                        dismiss();
                                    });

                                    layout.addView(item);
                                });
                    }
                });
    }
    private void sendShareMessage(String conversationId,String finalOtherUid) {
        Map<String, Object> message = new HashMap<>();
        message.put("nguoi_gui_id", myUid);
        message.put("noi_dung", shareText != null ? shareText : "");
        message.put("loai", "share_bai_viet");
        message.put("da_doc", false);
        message.put("post_id", postId);
        message.put("thoi_gian", com.google.firebase.Timestamp.now()); // đổi sang Timestamp.now()
        db.collection("bai_viet").document(postId).update("so_share", FieldValue.increment(1));
        db.collection("cuoc_tro_chuyen").document(conversationId)
                .collection("tin_nhan").add(message)
                .addOnSuccessListener(ref -> {
                    // Cập nhật tin nhắn cuối
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("tin_nhan_cuoi", "📎 Đã chia sẻ một bài viết");
                    conv.put("thoi_gian_cuoi", com.google.firebase.Timestamp.now());
                    conv.put("nguoi_gui_cuoi_id", myUid);
                    conv.put("loai_tin_nhan_cuoi", "share_bai_viet");
                    conv.put("id_tin_nhan_cuoi", ref.getId());
                    conv.put("thanh_vien", java.util.Arrays.asList(myUid, finalOtherUid));

                    db.collection("cuoc_tro_chuyen").document(conversationId)
                            .set(conv, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (getContext() != null)
                                    Toast.makeText(getContext(), "Đã chia sẻ!", Toast.LENGTH_SHORT).show();
                                dismiss(); // ✅ đóng bottom sheet
                                if (shareListener != null) shareListener.onShareDone(); // ✅ reload fragment
                            });
                });

    }
}